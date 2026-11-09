package com.jay.cardboardvr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Listens on 127.0.0.1:port for "fx,fy,fz[,ux,uy,uz]" from the gyro app (and "VP1..." viewer packets):
 *   f = look direction, u = the phone's x axis (head up/down axis), both in East/North/Up.
 * The UDP thread only computes the target yaw/pitch. The target is applied once per frame on the render
 * thread (MinecraftFrameMixin) so both eyes of a stereo frame see exactly the same head pose.
 * If that hook is missing, it falls back to writing the rotation straight from the UDP thread.
 */
public class GyroReceiver implements TrackingProvider {
    public static final GyroReceiver I = new GyroReceiver();

    public volatile boolean bound = false;
    public volatile long lastPacketMs = 0;
    public volatile int pps = 0;
    public volatile boolean recenter = true;
    public volatile long frameHookNs = 0;
    private volatile float outYaw, outPitch;
    private volatile boolean haveTarget = false;

    private DatagramSocket socket;
    private int count;
    private long ppsT;
    private float lastH, baseYaw, yawAccum, sYaw, sPitch, upSign = 0f;
    private boolean haveLast;

    public synchronized void start(int port) {
        stop();
        try {
            socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
            socket.setSoTimeout(500);
            bound = true;
        } catch (Exception e) {
            CardboardVrClient.LOG.warn("Could not open UDP port {}", port, e);
            socket = null;
            bound = false;
            return;
        }
        final DatagramSocket s = socket;
        Thread t = new Thread(() -> loop(s), "cardboardvr-udp");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stop() {
        if (socket != null) socket.close();
        socket = null;
        bound = false;
    }

    private void loop(DatagramSocket s) {
        byte[] buf = new byte[1024];
        while (!s.isClosed()) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                s.receive(p);
                String msg = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                if (msg.startsWith("VP") || msg.startsWith("SC")) ViewerParams.onPacket(msg);   // viewer (lens) settings from the QR scan
                else handle(msg);
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (s.isClosed()) break;
            }
        }
    }

    private void handle(String msg) {
        long now = System.currentTimeMillis();
        lastPacketMs = now;
        count++;
        if (now - ppsT >= 1000) { pps = count; count = 0; ppsT = now; }

        VrConfig c = VrConfig.I;
        if (!c.enabled() || !c.tracking) { haveLast = false; haveTarget = false; return; }

        String[] p = msg.trim().split(",");
        if (p.length < 3) return;
        float fx, fy, fz, ux = 0, uy = 0, uz = 0;
        boolean hasUp = p.length >= 6;
        try {
            fx = Float.parseFloat(p[0]);
            fy = Float.parseFloat(p[1]);
            fz = Float.parseFloat(p[2]);
            if (hasUp) {
                ux = Float.parseFloat(p[3]);
                uy = Float.parseFloat(p[4]);
                uz = Float.parseFloat(p[5]);
            }
        } catch (NumberFormatException e) {
            return;
        }

        float pitch = (float) Math.toDegrees(Math.asin(Math.max(-1f, Math.min(1f, fz))));

        // Heading. Plain atan2(f) flips violently when looking straight up or down, so with the head-up
        // vector u we use h = f_horizontal - f_z * u_horizontal, which stays stable all the way to the poles.
        float h;
        if (hasUp) {
            if (Math.abs(fz) < 0.5f) upSign = uz >= 0 ? 1f : -1f;      // looking level: head-up must point to the sky
            float s = upSign != 0f ? upSign : (uz >= 0 ? 1f : -1f);
            float hx = fx - fz * s * ux, hy = fy - fz * s * uy;
            h = (hx * hx + hy * hy > 1e-6f) ? (float) Math.toDegrees(Math.atan2(hx, hy)) : lastH;
        } else {
            h = (float) Math.toDegrees(Math.atan2(fx, fy));
        }

        if (!haveLast) { lastH = h; haveLast = true; }
        float d = h - lastH;
        while (d > 180) d -= 360;
        while (d < -180) d += 360;
        lastH = h;
        yawAccum += d;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (pl == null) return;

        if (recenter) {
            recenter = false;
            baseYaw = pl.getYRot();
            yawAccum = 0;
            sYaw = 0;
            sPitch = pitch;
        }

        float a = 1f - c.smoothing;
        sYaw += a * (yawAccum - sYaw);
        sPitch += a * (pitch - sPitch);

        outYaw = baseYaw + sYaw * c.lookSens;
        outPitch = Math.max(-90f, Math.min(90f, -sPitch * c.lookSens));
        haveTarget = true;

        // Fallback only: if the per-frame hook is not running, write from this thread.
        if (System.nanoTime() - frameHookNs > 300_000_000L) applyToPlayer(mc);
    }

    /** Called on the render thread at the start of every frame. */
    public void frameStart(Minecraft mc) {
        frameHookNs = System.nanoTime();
        applyToPlayer(mc);
    }

    private void applyToPlayer(Minecraft mc) {
        LocalPlayer pl = mc.player;
        VrConfig c = VrConfig.I;
        if (pl == null || !haveTarget || !c.enabled() || !c.tracking) return;
        if (ScreenCompat.current(mc) != null) return;
        float y = outYaw, x = outPitch;
        pl.setYRot(y);
        pl.setXRot(x);
        setOld(pl, y, x);
    }

    private static Field fYO, fXO;
    private static boolean reflTried;

    /** Keeps the previous-tick rotation equal to the current one so Minecraft does not blend between them. */
    static void setOld(Entity e, float y, float x) {
        if (!reflTried) {
            reflTried = true;
            try {
                fYO = Entity.class.getField("yRotO");
                fXO = Entity.class.getField("xRotO");
            } catch (Throwable t) {
                try {
                    fYO = Entity.class.getDeclaredField("yRotO");
                    fXO = Entity.class.getDeclaredField("xRotO");
                    fYO.setAccessible(true);
                    fXO.setAccessible(true);
                } catch (Throwable t2) {
                    fYO = null;
                    fXO = null;
                    CardboardVrClient.LOG.warn("Could not find old-rotation fields; camera may stutter");
                }
            }
        }
        if (fYO == null) return;
        try {
            fYO.setFloat(e, y);
            fXO.setFloat(e, x);
        } catch (Throwable ignored) {
        }
    }

    @Override public boolean isActive() { return System.currentTimeMillis() - lastPacketMs < 500; }
    @Override public float yawDeg() { return outYaw; }
    @Override public float pitchDeg() { return outPitch; }
}
