package com.example.client.mixin.skia;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("renderBuffers")
    RenderBuffers modid$getRenderBuffers();

    @Accessor("submitNodeStorage")
    SubmitNodeStorage modid$getSubmitNodeStorage();

    @Accessor("featureRenderDispatcher")
    FeatureRenderDispatcher modid$getFeatureRenderDispatcher();

    @Accessor("fogRenderer")
    FogRenderer modid$getFogRenderer();
}
