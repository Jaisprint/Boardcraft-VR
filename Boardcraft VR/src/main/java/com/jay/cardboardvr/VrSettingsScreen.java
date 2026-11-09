package com.jay.cardboardvr;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** One settings screen, split into titled sections (Head Tracking / Movement / Controls). Scroll with ^ and v. */
public class VrSettingsScreen extends Screen {
    private interface RowBuilder { void build(int y); }

    static final String[] NAMES = {"Tap", "Back", "Vol Up", "Vol Down"};
    static final float[] LOOK = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
    static final float[] SMOOTH = {0f, 0.2f, 0.4f, 0.6f, 0.8f};
    static final float[] MSENS = {0.5f, 1f, 1.5f, 2f, 3f, 4f};
    static final float[] DEAD = {0.5f, 1f, 2f, 3f, 5f};
    static final float[] SCREEN_IN = {5.5f, 6.0f, 6.1f, 6.2f, 6.4f, 6.5f, 6.6f, 6.7f, 6.8f, 7.0f};
    static final float[] STRENGTH = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f, 1.25f};
    static final float[] IPD = {58f, 60f, 62f, 63.9f, 65f, 67f, 70f};
    static final float[] CONV = {0f, 4f, 8f, 16f};
    static final int ROW = 24;

    private final Screen parent;
    private int scroll = 0;
    private String portText = null;
    private int learning = -1;
    private boolean waitRelease = false;
    private String note = "Vol Up + Vol Down together = Pause menu";
    private Button status;
    private EditBox portBox;
    private int maxScrollPx = 0;

    public VrSettingsScreen(Screen parent) {
        super(Component.literal("Boardcraft VR Settings"));
        this.parent = parent;
    }

    static float next(float[] a, float v) {
        int best = 0;
        float bd = 1e9f;
        for (int i = 0; i < a.length; i++) {
            float d = Math.abs(a[i] - v);
            if (d < bd) { bd = d; best = i; }
        }
        return a[(best + 1) % a.length];
    }

    static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    private Button btn(int x, int y, int w, Supplier<String> label, Runnable action) {
        Button b = Button.builder(Component.literal(label.get()), bb -> {
            action.run();
            VrConfig.save();
            bb.setMessage(Component.literal(label.get()));
        }).bounds(x, y, w, 20).build();
        return addRenderableWidget(b);
    }

    private Button label(int x, int y, int w, String text) {
        Button b = Button.builder(Component.literal(text), bb -> {}).bounds(x, y, w, 20).build();
        b.active = false;
        return addRenderableWidget(b);
    }

    private String keyLabel(int i) {
        return learning == i ? NAMES[i] + ": press a key..." : NAMES[i] + ": " + KeyNames.label(VrConfig.I.codes[i]);
    }

    static boolean validKey(int k) {
        return k == 32 || k == 39 || (k >= 44 && k <= 57) || k == 59 || k == 61 || (k >= 65 && k <= 93)
                || k == 96 || k == 161 || k == 162 || (k >= 256 && k <= 269) || (k >= 280 && k <= 284)
                || (k >= 290 && k <= 314) || (k >= 320 && k <= 348);
    }

    private String statusText() {
        GyroReceiver g = GyroReceiver.I;
        String t;
        if (!g.bound) t = "Port " + VrConfig.I.port + " in use";
        else if (g.isActive()) t = String.format("Gyro OK %d/s", g.pps);
        else t = "No gyro (port " + VrConfig.I.port + ")";
        return t + String.format(" | blk %d | dx %.0f", Diag.lookBlocked.get(), Diag.dx);
    }

    @Override
    protected void init() {
        VrConfig c = VrConfig.I;
        final int left = width / 2 - 155;
        final int lx = left, rx = left + 148, cw = 144, fw = 292;
        final int top = 32, bottom = height - 54;

        addRenderableWidget(Button.builder(
                Component.literal("VR: " + VrModeScreen.modeName(c.vrMode) + "  (tap to change)"),
                b -> minecraft.setScreenAndShow(new VrModeScreen(this))).bounds(left, 6, 310, 20).build());

        List<RowBuilder> rows = new ArrayList<>();

        // ---- Head Tracking ----
        rows.add(y -> label(lx, y, fw, "-- Head Tracking --"));
        rows.add(y -> {
            btn(lx, y, cw, () -> "Head tracking: " + onOff(c.tracking), () -> c.tracking = !c.tracking);
            btn(rx, y, cw, () -> "Recenter view", () -> GyroReceiver.I.recenter = true);
        });
        rows.add(y -> {
            btn(lx, y, cw, () -> String.format("Look sens: x%.2f", c.lookSens), () -> c.lookSens = next(LOOK, c.lookSens));
            btn(rx, y, cw, () -> String.format("Smoothing: %.1f", c.smoothing), () -> c.smoothing = next(SMOOTH, c.smoothing));
        });
        rows.add(y -> {
            portBox = new EditBox(font, lx, y, 96, 20, Component.literal("Gyro port"));
            portBox.setValue(portText != null ? portText : String.valueOf(c.port));
            portBox.setHint(Component.literal("Gyro port"));
            portBox.setResponder(s -> portText = s);
            addRenderableWidget(portBox);
            addRenderableWidget(Button.builder(Component.literal("Apply"), b -> applyPort())
                    .bounds(lx + 100, y, 44, 20).build());
            label(rx, y, cw, "Must match the app");
        });

        // ---- Stereo View ----
        rows.add(y -> label(lx, y, fw, "-- Stereo View --"));
        rows.add(y -> {
            btn(lx, y, cw, () -> "Stereo: " + onOff(c.stereo), () -> c.stereo = !c.stereo);
            btn(rx, y, cw, () -> String.format("IPD: %.1f mm%s", c.effectiveIpd(), (c.hasViewer && c.useViewerIpd) ? " (viewer)" : ""), () -> {
                if (c.hasViewer && c.useViewerIpd) c.useViewerIpd = false;   // switch to manual, keep the shown value
                else c.ipdMm = next(IPD, c.ipdMm);
            });
        });
        rows.add(y -> {
            btn(lx, y, cw, () -> c.conv <= 0f ? "Depth plane: far" : String.format("Depth plane: %.0f", c.conv), () -> c.conv = next(CONV, c.conv));
            label(rx, y, cw, StereoBridge.available() ? "StereoCraft: found" : "StereoCraft: MISSING");
        });

        // ---- Viewer ----
        rows.add(y -> label(lx, y, fw, "-- Viewer (scan QR in gyro app) --"));
        rows.add(y -> label(lx, y, fw, c.hasViewer ? c.viewerName : "Not set: double vision until you scan"));
        if (c.hasViewer) {
            rows.add(y -> {
                btn(lx, y, cw, () -> "Use viewer IPD: " + onOff(c.useViewerIpd), () -> c.useViewerIpd = !c.useViewerIpd);
                label(rx, y, cw, String.format("lens gap %.1f mm", c.lensMm));
            });
            rows.add(y -> {
                btn(lx, y, cw, () -> "Forget viewer", () -> {
                    c.hasViewer = false;
                    c.viewerName = "";
                    c.viewerSig = "";
                    rebuildWidgets();
                });
                label(rx, y, cw, c.dist.length >= 2
                        ? String.format("k1 %.2f  k2 %.2f", c.dist[0], c.dist[1])
                        : c.dist.length == 1 ? String.format("k1 %.2f", c.dist[0]) : "no lens coeffs");
            });
            rows.add(y -> {
                btn(lx, y, cw, () -> "Lens distortion: " + onOff(c.distortion), () -> c.distortion = !c.distortion);
                btn(rx, y, cw, () -> c.screenAuto
                        ? (c.devLongMm > 0f ? String.format("Screen: Auto %.0fx%.0f", c.devLongMm, c.devShortMm) : "Screen: Auto (no data)")
                        : String.format("Screen: %.1f in", c.screenIn), () -> {
                    if (c.screenAuto) { c.screenAuto = false; c.screenIn = SCREEN_IN[0]; }
                    else if (c.screenIn >= SCREEN_IN[SCREEN_IN.length - 1]) c.screenAuto = true;
                    else c.screenIn = next(SCREEN_IN, c.screenIn);
                });
            });
            rows.add(y -> {
                btn(lx, y, cw, () -> "Lens height: " + (c.vFlip ? "flipped" : "normal"), () -> c.vFlip = !c.vFlip);
                btn(rx, y, cw, () -> String.format("Strength: %d%%", Math.round(c.distStrength * 100)), () -> c.distStrength = next(STRENGTH, c.distStrength));
            });
        }

        // ---- Movement ----
        rows.add(y -> label(lx, y, fw, "-- Movement --"));
        rows.add(y -> {
            btn(lx, y, cw, () -> "Move with: " + (c.moveMode == 0 ? "Mouse" : "Gamepad"), () -> {
                c.moveMode = 1 - c.moveMode;
                rebuildWidgets();
            });
            if (c.moveMode == 0) {
                btn(rx, y, cw, () -> String.format("Walk sens: x%.1f", c.mouseSens), () -> c.mouseSens = next(MSENS, c.mouseSens));
            } else {
                label(rx, y, cw, "Stick -> WASD (launcher)");
            }
        });
        if (c.moveMode == 0) {
            rows.add(y -> {
                btn(lx, y, cw, () -> String.format("Dead zone: %.1f", c.deadZone), () -> c.deadZone = next(DEAD, c.deadZone));
                btn(rx, y, cw, () -> "Invert left/right: " + onOff(c.invertX), () -> c.invertX = !c.invertX);
            });
            rows.add(y -> {
                btn(lx, y, cw, () -> "Invert fwd/back: " + onOff(c.invertY), () -> c.invertY = !c.invertY);
                label(rx, y, cw, "Camera never moves");
            });
        }

        // ---- Controls ----
        rows.add(y -> label(lx, y, fw, "-- Controls --"));
        for (int i = 0; i < 4; i++) {
            final int k = i;
            rows.add(y -> {
                addRenderableWidget(Button.builder(Component.literal(keyLabel(k)), b -> {
                    learning = k;
                    waitRelease = true;
                    rebuildWidgets();
                }).bounds(lx, y, cw, 20).build());
                addRenderableWidget(Button.builder(Component.literal(VrActions.label(c.acts[k])),
                        b -> minecraft.setScreenAndShow(new VrActionPickerScreen(this, k)))
                        .bounds(rx, y, cw, 20).build());
            });
        }
        rows.add(y -> label(lx, y, fw, note));
        rows.add(y -> btn(lx, y, cw, () -> "Reset bindings", () -> {
            c.resetBindings();
            learning = -1;
            note = "Bindings reset";
            rebuildWidgets();
        }));

        // ---- scrolling ----
        int viewport = bottom - top;
        int total = rows.size() * ROW;
        int maxScroll = Math.max(0, ((total - viewport + ROW - 1) / ROW) * ROW);
        maxScrollPx = maxScroll;
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
        for (int idx = 0; idx < rows.size(); idx++) {
            int y = top - scroll + idx * ROW;
            if (y >= top && y + 20 <= bottom) rows.get(idx).build(y);
        }
        final int ms = maxScroll;
        Button up = addRenderableWidget(Button.builder(Component.literal("^"), b -> {
            scroll = Math.max(0, scroll - ROW * 3);
            rebuildWidgets();
        }).bounds(left + 296, top, 14, 20).build());
        up.active = scroll > 0;
        Button down = addRenderableWidget(Button.builder(Component.literal("v"), b -> {
            scroll = Math.min(ms, scroll + ROW * 3);
            rebuildWidgets();
        }).bounds(left + 296, bottom - 20, 14, 20).build());
        down.active = scroll < ms;

        status = label(left, height - 50, 310, statusText());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20).build());
    }

    /** Mouse wheel: one notch = one row. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int old = scroll;
        scroll = (int) Math.max(0, Math.min(maxScrollPx, scroll - Math.signum(scrollY) * ROW));
        if (scroll != old) rebuildWidgets();
        return true;
    }

    private void applyPort() {
        try {
            String txt = portBox != null ? portBox.getValue() : String.valueOf(VrConfig.I.port);
            int p = Integer.parseInt(txt.trim());
            if (p >= 1024 && p <= 65535) {
                VrConfig.I.port = p;
                VrConfig.save();
                GyroReceiver.I.start(p);
                portText = String.valueOf(p);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static boolean anyPressed() {
        for (int k = 32; k <= 348; k++) if (validKey(k) && InputHandler.poll(k)) return true;
        for (int m = 0; m <= 2; m++) if (InputHandler.poll(1000 + m)) return true;
        return false;
    }

    private static int firstPressed() {
        for (int k = 32; k <= 348; k++) if (validKey(k) && InputHandler.poll(k)) return k;
        for (int m = 0; m <= 2; m++) if (InputHandler.poll(1000 + m)) return 1000 + m;
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (status != null) status.setMessage(Component.literal(statusText()));
        if (learning < 0) return;
        // Click the key button once to select it, let go, then press any key or mouse button (left click too).
        if (waitRelease) {
            if (!anyPressed()) waitRelease = false;
            return;
        }
        int found = firstPressed();
        if (found != 0) bind(found);
    }

    private void bind(int found) {
        VrConfig c = VrConfig.I;
        note = "Vol Up + Vol Down together = Pause menu";
        for (int j = 0; j < 4; j++) {
            if (j != learning && c.codes[j] == found) {
                c.codes[j] = 0;
                note = "Removed that key from " + NAMES[j];
            }
        }
        c.codes[learning] = found;
        String mcName = KeyNames.mc(found);
        if (mcName != null) {
            for (KeyMapping m : minecraft.options.keyMappings) {
                if (mcName.equals(m.saveString())) {
                    c.acts[learning] = m.getName();
                    note = NAMES[learning] + " = " + VrActions.label(m.getName());
                    break;
                }
            }
        }
        learning = -1;
        waitRelease = false;
        VrConfig.save();
        rebuildWidgets();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return learning < 0;
    }

    @Override
    public void onClose() {
        VrConfig.save();
        minecraft.setScreenAndShow(parent);
    }
}
