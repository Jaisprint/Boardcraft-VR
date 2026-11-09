package com.jay.cardboardvr;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Scrolling list of every Minecraft key binding (plus two VR actions). Use the ^ and v buttons to scroll. */
public class VrActionPickerScreen extends Screen {
    private final Screen parent;
    private final int slot;
    private int scroll = 0;
    private int maxScrollRows = 0;
    private final List<String> names = new ArrayList<>();

    public VrActionPickerScreen(Screen parent, int slot) {
        super(Component.literal("Choose action"));
        this.parent = parent;
        this.slot = slot;
    }

    @Override
    protected void init() {
        names.clear();
        names.add("");
        names.add(VrActions.PAUSE);
        names.add(VrActions.RECENTER);
        for (KeyMapping m : minecraft.options.keyMappings) names.add(m.getName());

        int left = width / 2 - 155, top = 32, bottom = height - 54, row = 24;
        int visible = Math.max(1, (bottom - top + 4) / row);
        int maxScroll = Math.max(0, names.size() - visible);
        maxScrollRows = maxScroll;
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        Button title = addRenderableWidget(Button.builder(
                Component.literal("Action for " + VrSettingsScreen.NAMES[slot]), b -> {})
                .bounds(left, 6, 310, 20).build());
        title.active = false;

        for (int k = 0; k < visible; k++) {
            int idx = scroll + k;
            if (idx >= names.size()) break;
            final String n = names.get(idx);
            addRenderableWidget(Button.builder(Component.literal(VrActions.label(n)), b -> {
                VrConfig.I.acts[slot] = n;
                VrConfig.save();
                minecraft.setScreenAndShow(parent);
            }).bounds(left, top + row * k, 280, 20).build());
        }

        final int ms = maxScroll;
        Button up = addRenderableWidget(Button.builder(Component.literal("^"), b -> {
            scroll = Math.max(0, scroll - 3);
            rebuildWidgets();
        }).bounds(left + 286, top, 24, 20).build());
        up.active = scroll > 0;
        Button down = addRenderableWidget(Button.builder(Component.literal("v"), b -> {
            scroll = Math.min(ms, scroll + 3);
            rebuildWidgets();
        }).bounds(left + 286, top + 24, 24, 20).build());
        down.active = scroll < ms;
        Button pos = addRenderableWidget(Button.builder(
                Component.literal((scroll + 1) + "-" + Math.min(names.size(), scroll + visible) + "/" + names.size()),
                b -> {}).bounds(left + 286, top + 48, 24, 20).build());
        pos.active = false;

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20).build());
    }

    /** Mouse wheel: one notch = one row. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int old = scroll;
        scroll = (int) Math.max(0, Math.min(maxScrollRows, scroll - Math.signum(scrollY)));
        if (scroll != old) rebuildWidgets();
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }
}
