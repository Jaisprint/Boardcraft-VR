package com.jay.cardboardvr.mixin;

import com.jay.cardboardvr.Diag;
import com.jay.cardboardvr.VrConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks Minecraft's own mouse/controller camera turning for the local player while VR is active. */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void cardboardvr$noMouseLook(double yRot, double xRot, CallbackInfo ci) {
        if (VrConfig.I.enabled() && (Object) this instanceof LocalPlayer) {
            Diag.lookBlocked.incrementAndGet();
            ci.cancel();
        }
    }
}
