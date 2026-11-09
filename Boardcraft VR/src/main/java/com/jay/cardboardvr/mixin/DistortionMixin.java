package com.jay.cardboardvr.mixin;

import com.jay.cardboardvr.DistortionPass;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the lens-distortion pass on the finished side-by-side frame, right before Minecraft blits it to the window.
 * (Same call StereoCraft hooks for its cursor thread; an inject before it does not clash with that wrap.)
 */
@Mixin(Minecraft.class)
public abstract class DistortionMixin {
    @Inject(method = "renderFrame(Z)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/device/GpuSurface;blitFromTexture(Lcom/mojang/renderpearl/api/commands/CommandEncoder;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"),
            require = 0)
    private void cardboardvr$distort(boolean tick, CallbackInfo ci) {
        DistortionPass.run();
    }
}
