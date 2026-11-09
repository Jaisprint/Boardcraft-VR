package com.jay.cardboardvr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** 26.2+ moved the current screen to Minecraft.gui and renamed setScreen to setScreenAndShow. */
public class ScreenCompat {
    public static Screen current(Minecraft mc) { return mc.gui.screen(); }
    public static void open(Minecraft mc, Screen s) { mc.setScreenAndShow(s); }
}
