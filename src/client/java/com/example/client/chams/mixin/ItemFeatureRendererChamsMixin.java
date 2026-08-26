package com.example.client.chams.mixin;

import com.example.client.chams.ChamsRenderType;
import com.example.client.chams.DepthSeedItemQuads;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the cached RenderType of item quads only during the Chams depth-seed phase. */
@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererChamsMixin {

    @WrapOperation(
            method = "prepareMainSubmit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"
            )
    )
    private RenderType zombiesmod$chamsItemRenderType(BakedQuad.MaterialInfo material,
                                                       Operation<RenderType> original,
                                                       ItemFeatureRenderer.Submit submit) {
        if (submit.quads() instanceof DepthSeedItemQuads) {
            return ChamsRenderType.noDepth(material.sprite().atlasLocation());
        }
        return original.call(material);
    }

    @Inject(method = "prepareFoilSubmit", at = @At("HEAD"), cancellable = true)
    private void zombiesmod$skipDepthSeedFoil(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        if (submit.quads() instanceof DepthSeedItemQuads) {
            ci.cancel();
        }
    }
}
