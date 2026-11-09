package com.jay.cardboardvr.mixin;

import com.jay.cardboardvr.GyroReceiver;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the head pose once at the start of each frame, so both stereo eyes use the same pose. */
@Mixin(Minecraft.class)
public abstract class MinecraftFrameMixin {
    @Inject(method = "renderFrame(Z)V", at = @At("HEAD"), require = 0)
    private void cardboardvr$frameStart(boolean tick, CallbackInfo ci) {
        GyroReceiver.I.frameStart((Minecraft) (Object) this);
    }
}
