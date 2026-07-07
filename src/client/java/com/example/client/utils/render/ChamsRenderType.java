package com.example.client.utils.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

/**
 * 僵尸穿墙 chams 的 RenderType 工厂。
 *
 * <p>把 1.20.1 的 {@code glPolygonOffset(1, -1000000)} 移植到 26.1 的渲染管线：
 * 用一个和 ENTITY_CUTOUT 完全相同、但带巨大负向深度偏移的 pipeline，把模型深度拉到墙前面。
 * 深度测试仍是 LESS_THAN_OR_EQUAL，所以模型自身遮挡正常、且没有描边——就是纯填充穿墙。</p>
 */
public final class ChamsRenderType {

    /**
     * chams 是否处于激活状态。由 LivingEntityRendererChamsMixin 在目标实体 submit() 期间置 true，
     * RenderTypesChamsMixin 据此把各层 RenderType 换成无深度版。渲染单线程，普通静态即可。
     */
    public static boolean active = false;

    // = ENTITY_CUTOUT 的 pipeline，外加深度偏移（CompareOp.LESS_THAN_OR_EQUAL, 写深度, slope, 常量）
    private static final RenderPipeline CHAMS_PIPELINE =
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation("pipeline/zombiesmod_chams")
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withSampler("Sampler1")
                    .withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -1.0F, -1000000.0F))
                    .build();

    // 按贴图缓存，避免每帧 new
    private static final Function<Identifier, RenderType> CHAMS = Util.memoize(texture ->
            RenderType.create("zombiesmod_chams",
                    RenderSetup.builder(CHAMS_PIPELINE)
                            .withTexture("Sampler0", texture)
                            .useLightmap()
                            .useOverlay()
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup()));

    private ChamsRenderType() {}

    public static RenderType noDepth(Identifier texture) {
        return CHAMS.apply(texture);
    }
}
