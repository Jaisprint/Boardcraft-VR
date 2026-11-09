package com.jay.cardboardvr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class VrConfig {
    // input slots: 0 Tap, 1 Back, 2 Vol Up, 3 Vol Down
    public int ver = 0;
    public int vrMode = 0;            // 0 = OFF, 1 = ON (menus too), 2 = World only
    public boolean tracking = true;
    public int port = 9876;
    public boolean stereo = true;     // drive StereoCraft (SBS_FULL) while VR is active
    public float ipdMm = 63.9f;       // eye spacing in millimetres (Google Cardboard default)
    public float conv = 0f;           // depth plane in blocks, 0 = parallel (no convergence shear)
    public float lookSens = 1.0f;
    public float smoothing = 0.3f;
    public int moveMode = 0;          // 0 = mouse deltas -> walking, 1 = gamepad (launcher maps stick to WASD)
    public float mouseSens = 1.5f;
    public float deadZone = 1.0f;
    public boolean invertX = false;
    public boolean invertY = false;
    public int[] codes = {0, 0, 0, 0};
    public String[] acts = defaultActs();
    // viewer (lens) parameters from the gyro app's QR scan; lengths in mm, angles in degrees
    public boolean hasViewer = false;
    public boolean useViewerIpd = true;   // use the viewer's lens gap as IPD
    public String viewerName = "";
    public float lensMm = 0f;             // inter-lens distance
    public float screenToLensMm = 0f;
    public float trayToLensMm = 0f;
    public int vAlign = 0;                // 0 bottom, 1 center, 2 top
    public float[] fov = new float[0];    // left eye: left, right, bottom, top (used in 2c)
    public float[] dist = new float[0];   // K1, K2, ... (used in 2c)
    public String viewerSig = "";
    // lens distortion pass (2c)
    public boolean distortion = true;
    public boolean screenAuto = true;     // use the phone size reported by the gyro app (falls back to screenIn)
    public float devLongMm = 0f, devShortMm = 0f;   // phone screen size reported by the gyro app
    public float screenIn = 6.4f;         // phone screen diagonal in inches (used to turn pixels into mm)
    public float distStrength = 1.0f;     // scales k1/k2: 1.0 = exactly what the viewer says
    public boolean vFlip = false;         // flip the lens-centre height (phone held rotated 180 degrees)
    public double savedSens = -1;     // the player's real mouse sensitivity while VR forces it to 0

    public static VrConfig I = new VrConfig();

    public boolean enabled() { return vrMode != 0; }

    /** IPD actually sent to StereoCraft: the viewer's lens gap if one is set and enabled, else the manual value. */
    public float effectiveIpd() {
        return (hasViewer && useViewerIpd && lensMm > 0f) ? lensMm : ipdMm;
    }

    public static String[] defaultActs() {
        return new String[]{"key.attack", "key.use", "key.inventory", "key.jump"};
    }

    static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("cardboardvr.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                VrConfig c = new Gson().fromJson(Files.readString(p), VrConfig.class);
                if (c != null) I = c;
            }
        } catch (Exception e) {
            CardboardVrClient.LOG.warn("Could not read config, using defaults", e);
        }
        I.fix();
    }

    public static void save() {
        try {
            Files.writeString(path(), new GsonBuilder().setPrettyPrinting().create().toJson(I));
        } catch (Exception e) {
            CardboardVrClient.LOG.warn("Could not save config", e);
        }
    }

    public void resetBindings() {
        codes = new int[]{0, 0, 0, 0};
        acts = defaultActs();
    }

    void fix() {
        if (ver < 2) { mouseSens = 1.5f; deadZone = 1.0f; ver = 2; }
        if (ver < 3) { ver = 3; }
        if (viewerName == null) viewerName = "";
        if (viewerSig == null) viewerSig = "";
        if (screenIn < 4f || screenIn > 10f) screenIn = 6.4f;
        if (distStrength < 0f || distStrength > 2f) distStrength = 1.0f;
        if (fov == null) fov = new float[0];
        if (dist == null) dist = new float[0];
        if (codes == null || codes.length != 4) codes = new int[]{0, 0, 0, 0};
        if (acts == null || acts.length != 4) acts = defaultActs();
        for (int i = 0; i < 4; i++) if (acts[i] == null) acts[i] = "";
        if (vrMode < 0 || vrMode > 2) vrMode = 0;
        if (port < 1024 || port > 65535) port = 9876;
        if (lookSens <= 0) lookSens = 1f;
        if (smoothing < 0 || smoothing > 0.9f) smoothing = 0.3f;
        if (mouseSens <= 0) mouseSens = 1.5f;
        if (deadZone < 0) deadZone = 1f;
        if (moveMode < 0 || moveMode > 1) moveMode = 0;
        if (ipdMm < 55f || ipdMm > 100f) ipdMm = 63.9f;
        if (conv < 0f || conv > 16f) conv = 0f;
    }
}
