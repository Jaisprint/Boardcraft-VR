package com.jay.cardboardvr;

/** Converts GLFW key codes (and our 1000+ mouse codes) to Minecraft key names and readable labels. */
public class KeyNames {
    /** Minecraft's own key name for a code, e.g. 69 -> "key.keyboard.e". Null if unknown. */
    public static String mc(int code) {
        if (code >= 1000) {
            int b = code - 1000;
            return b == 0 ? "key.mouse.left" : b == 1 ? "key.mouse.right" : b == 2 ? "key.mouse.middle" : null;
        }
        if (code >= 65 && code <= 90) return "key.keyboard." + (char) (code + 32);
        if (code >= 48 && code <= 57) return "key.keyboard." + (char) code;
        if (code >= 290 && code <= 301) return "key.keyboard.f" + (code - 289);
        switch (code) {
            case 32: return "key.keyboard.space";
            case 257: return "key.keyboard.enter";
            case 256: return "key.keyboard.escape";
            case 258: return "key.keyboard.tab";
            case 259: return "key.keyboard.backspace";
            case 262: return "key.keyboard.right";
            case 263: return "key.keyboard.left";
            case 264: return "key.keyboard.down";
            case 265: return "key.keyboard.up";
            case 340: return "key.keyboard.left.shift";
            case 344: return "key.keyboard.right.shift";
            case 341: return "key.keyboard.left.control";
            case 345: return "key.keyboard.right.control";
            case 342: return "key.keyboard.left.alt";
            case 346: return "key.keyboard.right.alt";
            default: return null;
        }
    }

    public static String label(int code) {
        if (code == 0) return "Not set";
        String n = mc(code);
        if (n == null) return "Key " + code;
        String s = n.startsWith("key.mouse.") ? "Mouse " + n.substring(10) : n.substring(13);
        s = s.replace('.', ' ');
        if (s.length() == 1) return s.toUpperCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
