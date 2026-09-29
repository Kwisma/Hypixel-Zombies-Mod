package com.example.client.chams;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.Optional;
import java.util.function.Function;

/**
 * 僵尸穿墙 chams 的 RenderType 工厂。
 *
 * <p>26.2 使用反向 Z。这个 RenderType 先用 LESS_THAN 找到被世界挡住的片元并写入实体的
 * 最远深度，随后由原版 GREATER_THAN_OR_EQUAL 通道从后向前重建实体的正确表面。</p>
 */
public final class ChamsRenderType {

    /**
     * chams 是否处于激活状态。由 LivingEntityRendererChamsMixin 在目标实体 submit() 期间置 true，
     * RenderTypesChamsMixin 据此把各层 RenderType 换成深度种子通道。渲染单线程，普通静态即可。
     */
    public static boolean active = false;

    // 第一遍只写墙后片元的深度、不写颜色；第二遍原版通道负责正确表面和透明混合。
    private static final RenderPipeline DEPTH_SEED_PIPELINE =
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation("pipeline/zombiesmod_chams_depth_seed")
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withShaderDefine("NO_OVERLAY")
                    .withCull(false)
                    // Keep the world pass's color attachment without painting an opaque seed layer.
                    .withColorTargetState(new ColorTargetState(Optional.empty(),
                            GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_NONE))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true))
                    .build();

    // 按贴图缓存，避免每帧 new
    private static final Function<Identifier, RenderType> CHAMS = Util.memoize(texture ->
            RenderType.create("zombiesmod_chams_depth_seed",
                    RenderSetup.builder(DEPTH_SEED_PIPELINE)
                            .withTexture("Sampler0", texture)
                            .useLightmap()
                            .setOutline(RenderSetup.OutlineProperty.NONE)
                            .createRenderSetup()));

    private ChamsRenderType() {}

    public static RenderType noDepth(Identifier texture) {
        return CHAMS.apply(texture);
    }
}
