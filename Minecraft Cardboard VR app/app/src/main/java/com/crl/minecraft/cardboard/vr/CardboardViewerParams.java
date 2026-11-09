package com.crl.minecraft.cardboard.vr;
import android.net.Uri;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

/** Decodes Google's Cardboard DeviceParams protobuf carried in a viewer QR URL's p= parameter. */
final class CardboardViewerParams {
    final String encodedProto;
    String vendor = "";
    String model = "";
    float screenToLensMeters;
    float interLensMeters;
    float trayToLensMeters;
    final ArrayList<Float> fovAngles = new ArrayList<>();
    final ArrayList<Float> distortion = new ArrayList<>();
    int verticalAlignment;
    int primaryButton;

    private CardboardViewerParams(String encodedProto) { this.encodedProto = encodedProto; }

    static CardboardViewerParams fromQr(String raw) {
        String encoded = extractPayload(raw);
        byte[] bytes;
        try {
            bytes = Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (IllegalArgumentException first) {
            bytes = Base64.decode(encoded, Base64.DEFAULT);
        }
        if (bytes.length == 0 || bytes.length > 65536) throw new IllegalArgumentException("Invalid Cardboard payload size");

        CardboardViewerParams result = new CardboardViewerParams(
                Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING));
        result.parse(bytes);
        if (result.screenToLensMeters <= 0f || result.interLensMeters <= 0f
                || !Float.isFinite(result.screenToLensMeters) || !Float.isFinite(result.interLensMeters)) {
            throw new IllegalArgumentException("Missing required lens distances");
        }
        if (!result.fovAngles.isEmpty() && result.fovAngles.size() != 4) throw new IllegalArgumentException("Invalid FOV tuple");
        for (float angle : result.fovAngles) {
            if (!Float.isFinite(angle) || angle <= 0f || angle >= 180f) throw new IllegalArgumentException("Invalid FOV angle");
        }
        for (float coefficient : result.distortion) {
            if (!Float.isFinite(coefficient)) throw new IllegalArgumentException("Invalid distortion coefficient");
        }
        return result;
    }

    private static String extractPayload(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Empty QR code");
        Uri uri = Uri.parse(value);
        String parameter = uri.getQueryParameter("p");
        if (parameter != null && !parameter.isEmpty()) return parameter.replace(' ', '+');
        if (value.startsWith("p=")) return value.substring(2).replace(' ', '+');
        if (value.startsWith("http://") || value.startsWith("https://")) {
            throw new IllegalArgumentException("Cardboard URL has no p parameter");
        }
        return value.replace(' ', '+');
    }

    private void parse(byte[] data) {
        ProtoReader reader = new ProtoReader(data);
        while (!reader.atEnd()) {
            long tag = reader.readVarint();
            if (tag == 0) break;
            int field = (int) (tag >>> 3);
            int wire = (int) (tag & 7);
            switch (field) {
                case 1: if (wire == 2) vendor = reader.readString(); else reader.skip(wire); break;
                case 2: if (wire == 2) model = reader.readString(); else reader.skip(wire); break;
                case 3: if (wire == 5) screenToLensMeters = reader.readFloat(); else reader.skip(wire); break;
                case 4: if (wire == 5) interLensMeters = reader.readFloat(); else reader.skip(wire); break;
                case 5: readRepeatedFloats(reader, wire, fovAngles); break;
                case 6: if (wire == 5) trayToLensMeters = reader.readFloat(); else reader.skip(wire); break;
                case 7: readRepeatedFloats(reader, wire, distortion); break;
                case 11: if (wire == 0) verticalAlignment = (int) reader.readVarint(); else reader.skip(wire); break;
                case 12: if (wire == 0) primaryButton = (int) reader.readVarint(); else reader.skip(wire); break;
                default: reader.skip(wire); break;
            }
        }
    }

    private static void readRepeatedFloats(ProtoReader reader, int wire, ArrayList<Float> out) {
        if (wire == 5) {
            out.add(reader.readFloat());
        } else if (wire == 2) {
            byte[] packed = reader.readBytes();
            if ((packed.length & 3) != 0) throw new IllegalArgumentException("Invalid packed float field");
            ByteBuffer buffer = ByteBuffer.wrap(packed).order(ByteOrder.LITTLE_ENDIAN);
            while (buffer.remaining() >= 4) out.add(buffer.getFloat());
        } else reader.skip(wire);
    }

    String fovSummary() {
        if (fovAngles.size() != 4) return "not provided";
        return String.format(Locale.US, "%.1f / %.1f / %.1f / %.1f",
                fovAngles.get(0), fovAngles.get(1), fovAngles.get(2), fovAngles.get(3));
    }

    String distortionSummary() {
        if (distortion.isEmpty()) return "not provided";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < distortion.size(); i++) {
            if (i > 0) out.append(", ");
            out.append("K").append(i + 1).append(" ")
                    .append(String.format(Locale.US, "%.5f", distortion.get(i)));
        }
        return out.toString();
    }

    private static final class ProtoReader {
        private final byte[] data;
        private int position;
        ProtoReader(byte[] data) { this.data = data; }
        boolean atEnd() { return position >= data.length; }

        long readVarint() {
            long value = 0;
            int shift = 0;
            while (shift < 64) {
                if (position >= data.length) throw new IllegalArgumentException("Truncated protobuf varint");
                int b = data[position++] & 0xff;
                value |= (long) (b & 0x7f) << shift;
                if ((b & 0x80) == 0) return value;
                shift += 7;
            }
            throw new IllegalArgumentException("Invalid protobuf varint");
        }

        float readFloat() {
            if (position + 4 > data.length) throw new IllegalArgumentException("Truncated protobuf float");
            int bits = (data[position] & 0xff) | ((data[position + 1] & 0xff) << 8)
                    | ((data[position + 2] & 0xff) << 16) | ((data[position + 3] & 0xff) << 24);
            position += 4;
            return Float.intBitsToFloat(bits);
        }

        byte[] readBytes() {
            long length = readVarint();
            if (length < 0 || length > data.length - position) throw new IllegalArgumentException("Invalid protobuf field length");
            int count = (int) length;
            byte[] bytes = new byte[count];
            System.arraycopy(data, position, bytes, 0, count);
            position += count;
            return bytes;
        }

        String readString() { return new String(readBytes(), StandardCharsets.UTF_8); }

        void skip(int wire) {
            switch (wire) {
                case 0: readVarint(); break;
                case 1: advance(8); break;
                case 2:
                    long length = readVarint();
                    if (length < 0 || length > Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid field length");
                    advance((int) length);
                    break;
                case 5: advance(4); break;
                default: throw new IllegalArgumentException("Unsupported protobuf wire type " + wire);
            }
        }

        private void advance(int count) {
            if (count < 0 || count > data.length - position) throw new IllegalArgumentException("Truncated protobuf field");
            position += count;
        }
    }
}
