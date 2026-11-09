package com.jay.cardboardvr;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

/** Mouse-walk, rebindable buttons, Vol Up + Vol Down pause combo, F9 hotkey, camera-look lock. */
public class InputHandler {
    static final int VOL_UP = 2, VOL_DOWN = 3;

    private static Minecraft MC;
    private static boolean f9Prev, prevOn, prevScreenNull = true, comboLatched, holdSet;
    private static double lastX, lastY;
    private static float vx, vy, holdYaw, holdPitch;
    private static final boolean[] walk = new boolean[4];
    private static final boolean[] down = new boolean[4];
    private static final boolean[] prevDown = new boolean[4];
    private static final boolean[] active = new boolean[4];
    private static final KeyMapping[] heldMap = new KeyMapping[4];

    /** Uses only Minecraft's own input calls (raw GLFW polling crashes on Android launchers). */
    static boolean poll(int code) {
        if (MC == null) return false;
        if (code >= 1000) {
            int b = code - 1000;
            if (b == 0) return MC.mouseHandler.isLeftPressed();
            if (b == 1) return MC.mouseHandler.isRightPressed();
            if (b == 2) return MC.mouseHandler.isMiddlePressed();
            return false;
        }
        return InputConstants.isKeyDown(code);
    }

    /** While VR is active, force Minecraft's mouse sensitivity to 0 (restored when VR goes off). */
    static void manageSensitivity(Minecraft mc, VrConfig c) {
        var opt = mc.options.sensitivity();
        boolean force = c.vrMode == 1 || (c.vrMode == 2 && mc.player != null);
        if (force) {
            if (c.savedSens < 0) { c.savedSens = opt.get(); VrConfig.save(); }
            if (opt.get() > 0.0001) opt.set(0.0);
        } else if (c.savedSens >= 0) {
            opt.set(c.savedSens);
            c.savedSens = -1;
            VrConfig.save();
        }
    }

    static boolean isVrScreen(Screen s) {
        return s instanceof VrSettingsScreen || s instanceof VrModeScreen || s instanceof VrActionPickerScreen;
    }

    static boolean on(boolean cur, float v, float dead) {
        return cur ? v > dead * 0.4f : v > dead;
    }

    public static void tick(Minecraft mc) {
        MC = mc;
        VrConfig c = VrConfig.I;
        Screen cur = ScreenCompat.current(mc);

        manageSensitivity(mc, c);
        boolean wantStereo = c.stereo && (c.vrMode == 1 || (c.vrMode == 2 && mc.player != null));
        StereoBridge.apply(wantStereo, c.effectiveIpd(), c.conv);

        boolean f9 = InputConstants.isKeyDown(GLFW.GLFW_KEY_F9);
        if (f9 && !f9Prev && !isVrScreen(cur)) {
            ScreenCompat.open(mc, new VrSettingsScreen(cur));
        }
        f9Prev = f9;

        boolean active = c.enabled() && mc.player != null;
        if (!active) {
            if (prevOn) releaseAll();
            prevOn = false;
            return;
        }
        if (!prevOn) {
            GyroReceiver.I.recenter = true;
            vx = 0; vy = 0; holdSet = false;
        }
        prevOn = true;

        boolean screenNull = cur == null;

        // Hold the camera if there is no fresh gyro data, so the mouse can never turn it.
        if (screenNull) {
            boolean fresh = c.tracking && GyroReceiver.I.isActive();
            if (fresh) {
                holdSet = false;
            } else if (!holdSet) {
                holdYaw = mc.player.getYRot();
                holdPitch = mc.player.getXRot();
                holdSet = true;
            } else {
                mc.player.setYRot(holdYaw);
                mc.player.setXRot(holdPitch);
                GyroReceiver.setOld(mc.player, holdYaw, holdPitch);
            }
        }

        // Mouse -> walking (mouse mode only; in gamepad mode the launcher's stick->WASD does the walking)
        double x = mc.mouseHandler.xpos(), y = mc.mouseHandler.ypos();
        double dx = x - lastX, dy = y - lastY;
        lastX = x; lastY = y;
        if (screenNull != prevScreenNull || Math.abs(dx) > 300 || Math.abs(dy) > 300) { dx = 0; dy = 0; }
        prevScreenNull = screenNull;
        Diag.dx = dx;

        if (screenNull && c.moveMode == 0) {
            float sx = c.invertX ? -1f : 1f, sy = c.invertY ? -1f : 1f;
            vx = vx * 0.6f + (float) dx * c.mouseSens * sx;
            vy = vy * 0.6f + (float) dy * c.mouseSens * sy;
            float mag = (float) Math.hypot(vx, vy);
            boolean moving = (walk[0] || walk[1] || walk[2] || walk[3]) ? mag > c.deadZone * 0.4f : mag > c.deadZone;
            boolean fw = false, bk = false, rt = false, lf = false;
            if (moving) {
                // 8-way sectors: a slightly off-axis stick still walks straight, only a clear diagonal strafes
                double ang = Math.toDegrees(Math.atan2(vx, -vy));   // 0 = forward, 90 = right
                double aa = Math.abs(ang);
                fw = aa < 67.5;
                bk = aa > 112.5;
                rt = ang > 22.5 && ang < 157.5;
                lf = ang < -22.5 && ang > -157.5;
            }
            setWalk(0, fw);
            setWalk(1, bk);
            setWalk(2, rt);
            setWalk(3, lf);
        } else {
            vx = 0; vy = 0;
            for (int i = 0; i < 4; i++) setWalk(i, false);
        }

        // Buttons
        for (int i = 0; i < 4; i++) down[i] = c.codes[i] != 0 && poll(c.codes[i]);

        boolean both = c.codes[VOL_UP] != 0 && c.codes[VOL_DOWN] != 0 && down[VOL_UP] && down[VOL_DOWN];
        if (both && !comboLatched) {
            comboLatched = true;
            cancel(VOL_UP);
            cancel(VOL_DOWN);
            pause(mc);
        }
        if (!down[VOL_UP] && !down[VOL_DOWN]) comboLatched = false;

        for (int i = 0; i < 4; i++) {
            if (comboLatched && (i == VOL_UP || i == VOL_DOWN)) { prevDown[i] = down[i]; continue; }
            if (down[i] && !prevDown[i]) {
                InputHandler.active[i] = true;
                begin(mc, i);
            }
            if (!down[i] && prevDown[i] && InputHandler.active[i]) {
                InputHandler.active[i] = false;
                end(mc, i);
            }
            prevDown[i] = down[i];
        }
    }

    static KeyMapping walkMap(int i) {
        var o = MC.options;
        return i == 0 ? o.keyUp : i == 1 ? o.keyDown : i == 2 ? o.keyRight : o.keyLeft;
    }

    static void setWalk(int i, boolean want) {
        if (MC == null) return;
        if (want != walk[i]) {
            walk[i] = want;
            walkMap(i).setDown(want);
        }
    }

    static InputConstants.Key keyOf(KeyMapping m) {
        try {
            return InputConstants.getKey(m.saveString());
        } catch (Exception e) {
            return m.getDefaultKey();
        }
    }

    static void cancel(int i) {
        if (heldMap[i] != null) { heldMap[i].setDown(false); heldMap[i] = null; }
        active[i] = false;
    }

    static void begin(Minecraft mc, int i) {
        VrConfig c = VrConfig.I;
        String a = c.acts[i];
        if (a == null || a.isEmpty()) return;
        if (VrActions.PAUSE.equals(a)) { pause(mc); return; }
        if (VrActions.RECENTER.equals(a)) { GyroReceiver.I.recenter = true; return; }
        KeyMapping m = VrActions.find(mc, a);
        if (m == null || VrActions.passthrough(c.codes[i], m)) return;
        if ("key.inventory".equals(a)) return;               // handled on release
        if (ScreenCompat.current(mc) != null) return;
        m.setDown(true);
        KeyMapping.click(keyOf(m));
        heldMap[i] = m;
    }

    static void end(Minecraft mc, int i) {
        VrConfig c = VrConfig.I;
        if (heldMap[i] != null) { heldMap[i].setDown(false); heldMap[i] = null; }
        String a = c.acts[i];
        if ("key.inventory".equals(a)) {
            KeyMapping m = VrActions.find(mc, a);
            if (m != null && !VrActions.passthrough(c.codes[i], m)) inventory(mc, m);
        }
    }

    static void inventory(Minecraft mc, KeyMapping m) {
        Screen cur = ScreenCompat.current(mc);
        if (cur == null) {
            KeyMapping.click(keyOf(m));
        } else if (cur instanceof AbstractContainerScreen<?> && mc.player != null) {
            mc.player.closeContainer();
        }
    }

    static void pause(Minecraft mc) {
        Screen cur = ScreenCompat.current(mc);
        if (cur == null) ScreenCompat.open(mc, new PauseScreen(true));
        else if (cur instanceof PauseScreen) ScreenCompat.open(mc, null);
    }

    static void releaseAll() {
        for (int i = 0; i < 4; i++) {
            setWalk(i, false);
            cancel(i);
            prevDown[i] = false;
            down[i] = false;
        }
        comboLatched = false;
        vx = 0; vy = 0;
    }
}
