package com.example.client.utils.render;

import com.example.client.chams.ChamsRenderType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.BiFunction;

/** Translucent armor with the 26.3 OIT and combined glint paths. */
public final class ArmorRenderTypes {
    private static final BiFunction<Identifier, Boolean, RenderType> TRANSLUCENT =
            Util.memoize((texture, glint) -> {
                var setup = RenderSetup.builder(glint
                                ? RenderPipelines.ITEM_TRANSLUCENT_GLINT : RenderPipelines.ENTITY_TRANSLUCENT)
                        .setOitPipelines(glint ? RenderPipelines.OIT_ITEM_GLINT : RenderPipelines.OIT_ENTITY)
                        .withTexture("Sampler0", texture)
                        .useLightmap()
                        .useOverlay()
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .affectsCrumbling()
                        .sortOnUpload()
                        .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
                if (glint) {
                    setup.withTexture("GlintSampler", ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR)
                            .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING);
                }
                return RenderType.create("zombiesmod_armor_translucent" + (glint ? "_glint" : ""),
                        setup.createRenderSetup());
            });

    private ArmorRenderTypes() { }

    public static RenderType translucent(Identifier texture, boolean glint) {
        return ChamsRenderType.active ? ChamsRenderType.noDepth(texture) : TRANSLUCENT.apply(texture, glint);
    }
}
