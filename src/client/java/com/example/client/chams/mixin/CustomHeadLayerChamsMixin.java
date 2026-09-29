package com.example.client.chams.mixin;

import com.example.client.chams.ChamsRenderType;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Includes cached player-head skin RenderTypes in the Chams depth-seed pass. */
@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerChamsMixin {

    @Shadow
    @Final
    private PlayerSkinRenderCache playerSkinRenderCache;

    @Inject(method = "resolveSkullRenderType", at = @At("HEAD"), cancellable = true)
    private void zombiesmod$chamsPlayerHead(LivingEntityRenderState state, SkullBlock.Type skullType,
                                             CallbackInfoReturnable<RenderType> cir) {
        if (!ChamsRenderType.active
                || skullType != SkullBlock.Types.PLAYER
                || state.wornHeadProfile == null) {
            return;
        }

        Identifier skinTexture = playerSkinRenderCache.getOrDefault(state.wornHeadProfile)
                .playerSkin()
                .body()
                .texturePath();
        cir.setReturnValue(ChamsRenderType.noDepth(skinTexture));
    }
}
