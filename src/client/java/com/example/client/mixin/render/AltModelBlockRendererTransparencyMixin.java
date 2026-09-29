package com.example.client.mixin.render;

import com.example.client.module.modules.BlockTransparency;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AltModelBlockRendererImpl.class)
public class AltModelBlockRendererTransparencyMixin {

    @Shadow private BlockState blockState;

    @Inject(method = "transform", at = @At("HEAD"))
    private void zombiesmod$transparency(MutableQuadView quad, CallbackInfoReturnable<Boolean> cir) {

        if (BlockTransparency.isActive() && BlockTransparency.isTarget(this.blockState)) {
            quad.multiplyColor(BlockTransparency.alphaMultiplier()); // alpha ×50%（multiplyColor 走 ARGB.multiply，会乘 alpha）
            quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT);          // 改到半透明层，否则 SOLID 层不混合、看着仍不透
        }
    }
}
