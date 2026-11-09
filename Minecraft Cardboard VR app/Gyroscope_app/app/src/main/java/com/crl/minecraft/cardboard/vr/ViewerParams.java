package com.crl.minecraft.cardboard.vr;

import android.net.Uri;
import android.util.Base64;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cardboard viewer parameters (DeviceParams protobuf from the QR link's ?p= value). Lengths are metres in the proto. */
public class ViewerParams {
    public String vendor = "", model = "";
    public float screenToLens, lensDist, tray;   // metres
    public float[] fov = new float[0];           // left eye: left, right, bottom, top (degrees)
    public float[] k = new float[0];             // distortion coefficients K1, K2, ...
    public int align, button;

    /** Returns the ?p= bytes for a scanned/pasted link, following up to 6 redirects for short links (goo.gl/...).
     *  Call off the UI thread. */
    public static byte[] fromScan(String text) throws Exception {
        String url = text.trim();
        if (!url.contains("://") && url.contains(".")) url = "https://" + url;
        for (int hop = 0; hop < 6; hop++) {
            // Android blocks plain http by default, and goo.gl / google.com both work over https
            if (url.startsWith("http://")) url = "https://" + url.substring(7);
            String p = Uri.parse(url).getQueryParameter("p");
            if (p != null && !p.isEmpty()) {
                p = p.trim().replace('+', '-').replace(' ', '-').replace('/', '_');
                return Base64.decode(p, Base64.URL_SAFE);
            }
            if (!url.startsWith("https://")) throw new Exception("Not a web link: " + url);
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) CardboardViewerSetup");
            int code = c.getResponseCode();
            String loc = c.getHeaderField("Location");
            c.disconnect();
            if (code / 100 != 3 || loc == null) {
                throw new Exception("Link gave HTTP " + code + " and no redirect (" + url + ")");
            }
            url = new URL(new URL(url), loc).toString();   // handles relative redirects too
        }
        throw new Exception("No viewer data (?p=) found. Last link: " + url);
    }

    public static ViewerParams parse(byte[] b) throws Exception {
        ViewerParams v = new ViewerParams();
        List<Float> fov = new ArrayList<>(), k = new ArrayList<>();
        int[] pos = {0};
        while (pos[0] < b.length) {
            long tag = varint(b, pos);
            int field = (int) (tag >>> 3), wt = (int) (tag & 7);
            if (wt == 0) {
                long val = varint(b, pos);
                if (field == 11) v.align = (int) val;
                else if (field == 12) v.button = (int) val;
            } else if (wt == 1) {
                pos[0] += 8;
            } else if (wt == 2) {
                int len = (int) varint(b, pos);
                int end = pos[0] + len;
                if (len < 0 || end > b.length) throw new Exception("Bad viewer data");
                if (field == 1) v.vendor = new String(b, pos[0], len, StandardCharsets.UTF_8);
                else if (field == 2) v.model = new String(b, pos[0], len, StandardCharsets.UTF_8);
                else if (field == 5) for (int i = pos[0]; i + 4 <= end; i += 4) fov.add(f32(b, i));
                else if (field == 7) for (int i = pos[0]; i + 4 <= end; i += 4) k.add(f32(b, i));
                pos[0] = end;
            } else if (wt == 5) {
                if (pos[0] + 4 > b.length) throw new Exception("Bad viewer data");
                float f = f32(b, pos[0]);
                pos[0] += 4;
                if (field == 3) v.screenToLens = f;
                else if (field == 4) v.lensDist = f;
                else if (field == 6) v.tray = f;
                else if (field == 5) fov.add(f);
                else if (field == 7) k.add(f);
            } else {
                throw new Exception("Bad viewer data");
            }
        }
        if (v.lensDist <= 0 || v.screenToLens <= 0) throw new Exception("Viewer data missing lens distances");
        v.fov = toArr(fov);
        v.k = toArr(k);
        return v;
    }

    private static long varint(byte[] b, int[] pos) throws Exception {
        long r = 0;
        int shift = 0;
        while (pos[0] < b.length) {
            int x = b[pos[0]++] & 0xff;
            r |= (long) (x & 0x7f) << shift;
            if ((x & 0x80) == 0) return r;
            shift += 7;
            if (shift > 63) break;
        }
        throw new Exception("Bad viewer data");
    }

    private static float f32(byte[] b, int i) {
        int bits = (b[i] & 0xff) | (b[i + 1] & 0xff) << 8 | (b[i + 2] & 0xff) << 16 | (b[i + 3] & 0xff) << 24;
        return Float.intBitsToFloat(bits);
    }

    private static float[] toArr(List<Float> l) {
        float[] a = new float[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }

    private static String clean(String s) { return s.replace('\n', ' ').replace('\r', ' ').trim(); }

    private static String join(float[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.US, "%.6f", a[i]));
        }
        return sb.toString();
    }

    /** Text packet for the mod. Lengths in millimetres. */
    public String toWire() {
        return "VP1\n"
                + "v=" + clean(vendor) + "\n"
                + "m=" + clean(model) + "\n"
                + String.format(Locale.US, "s2l=%.3f\nild=%.3f\ntray=%.3f\n", screenToLens * 1000f, lensDist * 1000f, tray * 1000f)
                + "fov=" + join(fov) + "\n"
                + "al=" + align + "\n"
                + "k=" + join(k) + "\n";
    }

    public String summary() {
        String name = (clean(vendor) + " " + clean(model)).trim();
        if (name.isEmpty()) name = "Unnamed viewer";
        return String.format(Locale.US, "%s\nlens gap %.1f mm, screen-lens %.1f mm, %d distortion coeffs",
                name, lensDist * 1000f, screenToLens * 1000f, k.length);
    }
}
