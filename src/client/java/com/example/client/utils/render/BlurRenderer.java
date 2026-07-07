package com.example.client.utils.render;

import com.example.ZombiesMod;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.resources.Identifier;

import java.util.OptionalDouble;

/**
 * GUI/HUD 矩形背景模糊。
 *
 * 提取阶段把矩形作为普通 GUI 元素提交；真正渲染 GUI 前只复制一次主颜色目标，
 * 所有矩形都从这张未包含 GUI 的快照采样。因此矩形外不受影响，后续 GUI 内容保持清晰。
 */
public final class BlurRenderer {
    public static final int MAX_RADIUS = 10;

    private static final RenderPipeline[] PIPELINES = new RenderPipeline[MAX_RADIUS + 1];

    private static GpuTexture snapshotTexture;
    private static GpuTextureView snapshotView;
    private static GpuSampler sampler;
    private static TextureSetup textureSetup;
    private static int textureWidth = -1;
    private static int textureHeight = -1;

    private static boolean requestedThisFrame;
    private static boolean failed;

    private BlurRenderer() {
    }

    public static void draw(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float radius
    ) {
        if (failed || graphics == null || width <= 0 || height <= 0 || !Float.isFinite(radius)) {
            return;
        }

        int roundedRadius = Math.clamp(Math.round(radius), 0, MAX_RADIUS);
        if (roundedRadius == 0) {
            return;
        }

        try {
            RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
            if (main == null || main.width <= 0 || main.height <= 0) {
                return;
            }

            ensureResources(RenderSystem.getDevice(), main.width, main.height);
            requestedThisFrame = true;

            graphics.fill(
                    pipelineFor(roundedRadius),
                    textureSetup,
                    x,
                    y,
                    x + width,
                    y + height
            );
        } catch (Throwable throwable) {
            fail("Failed to submit GUI blur", throwable);
        }
    }

    /** 在 GuiRenderer 开始画 GUI 前调用；没有 drawBlur 请求时不会复制帧缓冲。 */
    public static void captureIfRequested() {
        if (failed || !requestedThisFrame) {
            return;
        }
        requestedThisFrame = false;

        try {
            RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
            if (main == null || main.width <= 0 || main.height <= 0) {
                return;
            }

            GpuDevice device = RenderSystem.getDevice();
            ensureResources(device, main.width, main.height);
            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyTextureToTexture(
                    main.getColorTexture(),
                    snapshotTexture,
                    0,
                    0,
                    0,
                    0,
                    0,
                    main.width,
                    main.height
            );
        } catch (Throwable throwable) {
            fail("Failed to capture the GUI blur background", throwable);
        }
    }

    public static void close() {
        requestedThisFrame = false;
        textureSetup = null;

        if (snapshotView != null) {
            snapshotView.close();
            snapshotView = null;
        }
        if (snapshotTexture != null) {
            snapshotTexture.close();
            snapshotTexture = null;
        }
        if (sampler != null) {
            sampler.close();
            sampler = null;
        }

        textureWidth = -1;
        textureHeight = -1;
    }

    private static RenderPipeline pipelineFor(int radius) {
        RenderPipeline pipeline = PIPELINES[radius];
        if (pipeline != null) {
            return pipeline;
        }

        pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("zombies-mod", "pipeline/gui_blur_" + radius))
                .withVertexShader(Identifier.fromNamespaceAndPath("minecraft", "core/position_color"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("zombies-mod", "core/gui_blur"))
                .withShaderDefine("BLUR_RADIUS", radius)
                .withSampler("Sampler0")
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                .withCull(false)
                .build();
        PIPELINES[radius] = pipeline;
        return pipeline;
    }

    private static void ensureResources(GpuDevice device, int width, int height) {
        if (snapshotTexture != null && width == textureWidth && height == textureHeight) {
            return;
        }

        textureSetup = null;
        if (snapshotView != null) {
            snapshotView.close();
        }
        if (snapshotTexture != null) {
            snapshotTexture.close();
        }

        textureWidth = width;
        textureHeight = height;
        snapshotTexture = device.createTexture(
                "zombiesmod-gui-blur-snapshot",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width,
                height,
                1,
                1
        );
        snapshotView = device.createTextureView(snapshotTexture);

        if (sampler == null) {
            sampler = device.createSampler(
                    AddressMode.CLAMP_TO_EDGE,
                    AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR,
                    FilterMode.LINEAR,
                    1,
                    OptionalDouble.empty()
            );
        }
        textureSetup = TextureSetup.singleTexture(snapshotView, sampler);
    }

    private static void fail(String message, Throwable throwable) {
        if (failed) {
            return;
        }
        failed = true;
        requestedThisFrame = false;
        ZombiesMod.LOGGER.error("[BlurRenderer] {}. Blur has been disabled for this session.", message, throwable);
    }
}
