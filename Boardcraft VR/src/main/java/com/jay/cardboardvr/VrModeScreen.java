package com.jay.cardboardvr;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Popup: ON / OFF on top, World only below. */
public class VrModeScreen extends Screen {
    private final Screen parent;

    public VrModeScreen(Screen parent) {
        super(Component.literal("VR Mode"));
        this.parent = parent;
    }

    public static String modeName(int m) {
        return m == 1 ? "ON" : m == 2 ? "World only" : "OFF";
    }

    private void pick(int mode) {
        VrConfig.I.vrMode = mode;
        VrConfig.save();
        minecraft.setScreenAndShow(parent);
    }

    private Button info(int x, int y, int w, String text) {
        Button b = Button.builder(Component.literal(text), bb -> {}).bounds(x, y, w, 20).build();
        b.active = false;
        return addRenderableWidget(b);
    }

    @Override
    protected void init() {
        int left = width / 2 - 155;
        int y = Math.max(8, height / 2 - 70);
        info(left, y, 310, "VR Mode (now: " + modeName(VrConfig.I.vrMode) + ")");
        addRenderableWidget(Button.builder(Component.literal("ON"), b -> pick(1)).bounds(left, y + 28, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("OFF"), b -> pick(0)).bounds(left + 160, y + 28, 150, 20).build());
        addRenderableWidget(Button.builder(Component.literal("World only"), b -> pick(2)).bounds(left, y + 52, 310, 20).build());
        info(left, y + 80, 310, "ON = also in menus and pause screen");
        info(left, y + 104, 310, "World only = only while inside a world");
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(width / 2 - 50, y + 132, 100, 20).build());
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }
}
