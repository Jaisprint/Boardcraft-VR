package com.jay.cardboardvr;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** 16x16 icon buttons. Sprites live in assets/cardboardvr/textures/gui/sprites/. */
public class Icons {
    public static final String NS = "cardboardvr";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NS, path);
    }

    /** vr_off / vr_on (plain) or vr_off_gear / vr_on_gear (with the small settings gear). */
    public static String sprite(boolean gear) {
        String base = VrConfig.I.enabled() ? "vr_on" : "vr_off";
        return gear ? base + "_gear" : base;
    }

    public static Button button(String sprite, Component message, Button.OnPress press, int x, int y) {
        SpriteIconButton b = SpriteIconButton.builder(message, press, true)
                .width(20)
                .sprite(id(sprite), 16, 16)
                .build();
        b.setPosition(x, y);
        return b;
    }
}
