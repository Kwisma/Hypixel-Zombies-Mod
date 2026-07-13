package com.example.client.mixin;

import com.example.client.utils.HideEntityState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zombiesmod$hideHeadWhenEntityFaded(
            PoseStack poseStack,
            SubmitNodeCollector nodeCollector,
            int packedLight,
            LivingEntityRenderState renderState,
            float yRot,
            float xRot,
            CallbackInfo ci
    ) {
        if (renderState instanceof HideEntityState hideState && hideState.zombiesmod$isFaded()) {
            ci.cancel();
        }
    }
}
