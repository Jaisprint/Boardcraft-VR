package com.jay.cardboardvr;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;

/** Drives the StereoCraft mod's options (mode / IPD / convergence) from Boardcraft's own settings. */
public class StereoBridge {
    private static boolean tried, ok;
    private static Object inst, modeOff, modeFull, lastMode;
    private static Field fMode, fIpd, fConv;
    private static float lastIpd = -1f, lastConv = -1f;

    public static boolean available() {
        if (!tried) {
            tried = true;
            try {
                if (FabricLoader.getInstance().isModLoaded("stereoscopic")) {
                    Class<?> so = Class.forName("com.mitchellmarx.stereoscopic.core.StereoOptions");
                    Class<?> sm = Class.forName("com.mitchellmarx.stereoscopic.core.StereoMode");
                    inst = so.getField("INSTANCE").get(null);
                    fMode = so.getField("mode");
                    fIpd = so.getField("ipd");
                    fConv = so.getField("convergence");
                    modeOff = sm.getField("OFF").get(null);
                    modeFull = sm.getField("SBS_FULL").get(null);
                    ok = true;
                }
            } catch (Throwable t) {
                CardboardVrClient.LOG.warn("StereoCraft found but its options could not be read", t);
                ok = false;
            }
        }
        return ok;
    }

    /** True while StereoCraft has been switched to SBS_FULL by Boardcraft. */
    public static boolean stereoOn() { return ok && lastMode != null && lastMode == modeFull; }

    public static void apply(boolean on, float ipdMm, float conv) {
        if (!available()) return;
        try {
            Object want = on ? modeFull : modeOff;
            if (lastMode != want) { fMode.set(inst, want); lastMode = want; }
            if (on) {
                float ipd = Math.max(0.055f, Math.min(0.100f, ipdMm / 1000f));
                float cv = Math.max(0f, Math.min(16f, conv));
                if (ipd != lastIpd) { fIpd.setFloat(inst, ipd); lastIpd = ipd; }
                if (cv != lastConv) { fConv.setFloat(inst, cv); lastConv = cv; }
            }
        } catch (Throwable t) {
            CardboardVrClient.LOG.warn("Could not update StereoCraft options", t);
            ok = false;
        }
    }
}
