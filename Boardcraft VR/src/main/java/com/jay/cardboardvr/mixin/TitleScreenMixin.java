package com.jay.cardboardvr.mixin;

import com.jay.cardboardvr.Icons;
import com.jay.cardboardvr.ScreenCompat;
import com.jay.cardboardvr.VrSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One icon button on the title screen: grey cardboard = VR off, green = VR on. Opens the VR settings. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void cardboardvr$addButtons(CallbackInfo ci) {
        int x = this.width / 2 + 104, y = this.height / 4 + 48;
        this.addRenderableWidget(Icons.button(Icons.sprite(false), Component.literal("VR Settings"),
                b -> ScreenCompat.open(Minecraft.getInstance(), new VrSettingsScreen(this)), x, y));
    }
}
