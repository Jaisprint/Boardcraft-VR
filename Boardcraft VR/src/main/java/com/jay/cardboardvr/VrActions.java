package com.jay.cardboardvr;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Actions a physical button can trigger: any real Minecraft key binding, plus two VR actions. */
public class VrActions {
    public static final String PAUSE = "cardboardvr.pause";
    public static final String RECENTER = "cardboardvr.recenter";

    public static KeyMapping find(Minecraft mc, String name) {
        if (name == null || name.isEmpty()) return null;
        for (KeyMapping m : mc.options.keyMappings) {
            if (m.getName().equals(name)) return m;
        }
        return null;
    }

    public static String label(String name) {
        if (name == null || name.isEmpty()) return "None";
        if (PAUSE.equals(name)) return "VR: Pause menu";
        if (RECENTER.equals(name)) return "VR: Recenter view";
        return Component.translatable(name).getString();
    }

    /** True if Minecraft already handles this physical key for that action (so we must not double-trigger). */
    public static boolean passthrough(int code, KeyMapping m) {
        String n = KeyNames.mc(code);
        return n != null && n.equals(m.saveString());
    }
}
