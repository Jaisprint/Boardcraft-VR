package com.jay.cardboardvr.mixin;

import com.jay.cardboardvr.Icons;
import com.jay.cardboardvr.ScreenCompat;
import com.jay.cardboardvr.VrSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pause menu icon: cardboard (grey/green) with a small gear. It goes at the end of the row of small
 * 20x20 icon buttons (bug, chat, friends, ...). If that row isn't found it goes right of the main buttons.
 */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void cardboardvr$addVrButton(CallbackInfo ci) {
        int maxRight = this.width / 2 + 102;
        int y = this.height / 4 + 72;
        int iconRight = -1, iconY = 0;
        String options = Component.translatable("menu.options").getString();
        for (var ch : this.children()) {
            if (ch instanceof AbstractWidget w) {
                maxRight = Math.max(maxRight, w.getX() + w.getWidth());
                String m = w.getMessage().getString();
                if (m.contains("Mod") || m.equals(options)) y = w.getY();
                if (w.getWidth() == 20 && w.getHeight() == 20 && w.getX() + w.getWidth() > iconRight) {
                    iconRight = w.getX() + w.getWidth();
                    iconY = w.getY();
                }
            }
        }
        int x, yy;
        if (iconRight >= 0) { x = iconRight + 4; yy = iconY; }
        else { x = maxRight + 4; yy = y; }
        if (x + 20 > this.width) x = this.width - 22;
        this.addRenderableWidget(Icons.button(Icons.sprite(true), Component.literal("VR Settings"),
                b -> ScreenCompat.open(Minecraft.getInstance(), new VrSettingsScreen(this)), x, yy));
    }
}
