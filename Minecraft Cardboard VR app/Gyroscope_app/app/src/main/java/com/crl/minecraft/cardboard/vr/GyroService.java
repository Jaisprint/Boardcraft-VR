package com.crl.minecraft.cardboard.vr;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Locale;

/** Sends "fx,fy,fz,ux,uy,uz" (look direction + phone x axis, East/North/Up) to 127.0.0.1:port ~100x per second.
 *  Also re-sends the saved viewer parameters (QR scan) about every 2 seconds. */
public class GyroService extends Service implements SensorEventListener {
    public static volatile boolean running = false;

    private SensorManager sm;
    private DatagramSocket sock;
    private InetAddress addr;
    private int port = 9876;
    private HandlerThread ht;
    private final float[] R = new float[9];
    private int cnt;
    private String screenWire = null;
    private android.content.SharedPreferences sp;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent in, int flags, int startId) {
        if (in != null) port = in.getIntExtra("port", 9876);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("gyro", "Gyroscope sender", NotificationManager.IMPORTANCE_LOW));
            nb = new Notification.Builder(this, "gyro");
        } else {
            nb = new Notification.Builder(this);
        }
        Notification n = nb.setContentTitle("Gyroscope Sender")
                .setContentText("Sending head tracking to port " + port)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);

        sp = getSharedPreferences("gyro", MODE_PRIVATE);
        try {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            ((android.view.WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
            if (dm.xdpi > 50 && dm.ydpi > 50) {
                float xm = dm.widthPixels / dm.xdpi * 25.4f, ym = dm.heightPixels / dm.ydpi * 25.4f;
                screenWire = String.format(Locale.US, "SC1\nl=%.2f\ns=%.2f\n", Math.max(xm, ym), Math.min(xm, ym));
            }
        } catch (Throwable ignored) {
        }
        stopSensors();
        try {
            addr = InetAddress.getByName("127.0.0.1");
            sock = new DatagramSocket();
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor s = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (s == null) s = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (s == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        ht = new HandlerThread("gyro");
        ht.start();
        sm.registerListener(this, s, 10000, new Handler(ht.getLooper()));
        running = true;
        return START_NOT_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        try {
            SensorManager.getRotationMatrixFromVector(R, e.values);
            // back of the phone = -Z of the device, expressed in world (East, North, Up)
            String s = String.format(Locale.US, "%.5f,%.5f,%.5f,%.5f,%.5f,%.5f", -R[2], -R[5], -R[8], R[0], R[3], R[6]);
            byte[] d = s.getBytes();
            sock.send(new DatagramPacket(d, d.length, addr, port));
            if (cnt++ % 200 == 0) {
                if (screenWire != null) {
                    byte[] sd = screenWire.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    sock.send(new DatagramPacket(sd, sd.length, addr, port));
                }
                String vp = sp.getString("vp", null);
                if (vp != null) {
                    byte[] vd = vp.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    sock.send(new DatagramPacket(vd, vd.length, addr, port));
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void stopSensors() {
        if (sm != null) sm.unregisterListener(this);
        if (ht != null) ht.quitSafely();
        if (sock != null) sock.close();
        sm = null; ht = null; sock = null;
    }

    @Override
    public void onDestroy() {
        stopSensors();
        running = false;
        super.onDestroy();
    }
}
