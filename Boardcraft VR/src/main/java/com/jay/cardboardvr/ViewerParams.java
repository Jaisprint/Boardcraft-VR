package com.jay.cardboardvr;

/** Applies a "VP1" viewer packet from the gyro app (decoded Cardboard QR) to the config. "VP0" clears the viewer. */
public class ViewerParams {
    static void onPacket(String msg) {
        try {
            VrConfig c = VrConfig.I;
            String sig = msg.trim();
            if (sig.startsWith("SC1")) {   // physical screen size from the gyro app
                float l = 0, sh = 0;
                for (String line : sig.split("\n")) {
                    if (line.startsWith("l=")) l = Float.parseFloat(line.substring(2).trim());
                    else if (line.startsWith("s=")) sh = Float.parseFloat(line.substring(2).trim());
                }
                if (l > 60f && l < 400f && sh > 30f && sh < 300f
                        && (Math.abs(l - c.devLongMm) > 0.5f || Math.abs(sh - c.devShortMm) > 0.5f)) {
                    c.devLongMm = l;
                    c.devShortMm = sh;
                    VrConfig.save();
                    CardboardVrClient.LOG.info("Screen size from gyro app: {} x {} mm", l, sh);
                }
                return;
            }
            if (sig.startsWith("VP0")) {
                if (c.hasViewer) {
                    c.hasViewer = false;
                    c.viewerName = "";
                    c.viewerSig = "";
                    VrConfig.save();
                }
                return;
            }
            if (!sig.startsWith("VP1") || sig.equals(c.viewerSig)) return;   // unchanged: nothing to do

            String vendor = "", model = "";
            float s2l = 0, ild = 0, tray = 0;
            int al = 0;
            float[] fov = new float[0], k = new float[0];
            for (String line : sig.split("\n")) {
                int e = line.indexOf('=');
                if (e < 0) continue;
                String key = line.substring(0, e), val = line.substring(e + 1).trim();
                switch (key) {
                    case "v" -> vendor = val;
                    case "m" -> model = val;
                    case "s2l" -> s2l = Float.parseFloat(val);
                    case "ild" -> ild = Float.parseFloat(val);
                    case "tray" -> tray = Float.parseFloat(val);
                    case "al" -> al = Integer.parseInt(val);
                    case "fov" -> fov = floats(val);
                    case "k" -> k = floats(val);
                    default -> { }
                }
            }
            if (ild < 30f || ild > 120f || s2l <= 0f) {
                CardboardVrClient.LOG.warn("Ignoring viewer packet with odd values (lens gap {} mm)", ild);
                return;
            }
            c.viewerName = (vendor + " " + model).trim();
            c.lensMm = ild;
            c.screenToLensMm = s2l;
            c.trayToLensMm = tray;
            c.vAlign = al;
            c.fov = fov;
            c.dist = k;
            c.hasViewer = true;
            c.viewerSig = sig;
            VrConfig.save();
            CardboardVrClient.LOG.info("Viewer set: {} (lens gap {} mm)", c.viewerName, ild);
        } catch (Exception e) {
            CardboardVrClient.LOG.warn("Bad viewer packet", e);
        }
    }

    private static float[] floats(String s) {
        if (s.isEmpty()) return new float[0];
        String[] p = s.split(",");
        float[] a = new float[p.length];
        for (int i = 0; i < p.length; i++) a[i] = Float.parseFloat(p[i].trim());
        return a;
    }
}
