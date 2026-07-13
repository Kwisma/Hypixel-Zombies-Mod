package com.example.client.newrender;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
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
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class LiquidGlass {
    private LiquidGlass() {}

    // ---- 自定义渲染管线（首次使用时由系统惰性编译） ----
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("zombies-mod", "pipeline/liquid_glass"))
            .withVertexShader(Identifier.fromNamespaceAndPath("zombies-mod", "core/liquid_glass"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("zombies-mod", "core/liquid_glass"))
            .withSampler("Sampler0")
            .withUniform("LiquidGlass", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.TRIANGLES)
            .withCull(false)                                                   // 不剔除（quad 绕序无所谓，否则可能整块被剔掉看不见）
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)) // 半透明混合，alpha 生效
            .withDepthStencilState(Optional.empty())                           // 渲染通道无深度附件 → 不做深度测试
            .build();

    // UBO 字节数（std140，对应 fsh 的 layout）：vec2+pad + vec4 + vec4 + 8*float = 80
    private static final int UBO_BYTES = 80;
    private static final int FLOATS_PER_VERTEX = 3;  // POSITION = vec3
    private static final int VERTS = 6;              // 两个三角形

    // ---- 按屏幕尺寸缓存的资源 ----
    private static GpuTexture snapshotTex;
    private static GpuTextureView snapshotView;
    private static GpuSampler sampler;
    private static int texW = -1, texH = -1;

    private static boolean failed = false; // 出错后停手，避免每帧刷屏

    /**
     * 画一块液态玻璃面板（坐标为 GUI 像素，左上原点）。在 GUI 渲染阶段调用。
     */
    public static void draw(
            float x, float y, float width, float height,
            float radius, float blur, float refraction, float dispersion,
            float tintR, float tintG, float tintB, float tintStrength,
            float edge, float alpha
    ) {
        if (failed) return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (main == null) return;

        try {
            GpuDevice device = RenderSystem.getDevice();
            int sw = main.width;   // VERIFY: 主目标像素宽高字段名（width/height 或 getWidth()）
            int sh = main.height;

            ensureResources(device, sw, sh);

            // 1) 快照：主颜色 → snapshotTex（先拷，再画，避免读写同一张图）
            CommandEncoder encoder = device.createCommandEncoder();
            encoder.copyTextureToTexture(main.getColorTexture(), snapshotTex, 0, 0, 0, 0, 0, sw, sh);

            // 2) UBO（std140 顺序必须和 fsh 一致）
            ByteBuffer ubo = ByteBuffer.allocateDirect(UBO_BYTES).order(ByteOrder.nativeOrder());
            ubo.putFloat(sw).putFloat(sh).putFloat(0).putFloat(0);          // uScreenSize + pad
            ubo.putFloat(x).putFloat(y).putFloat(width).putFloat(height);    // uRect
            ubo.putFloat(tintR).putFloat(tintG).putFloat(tintB).putFloat(tintStrength); // uTint
            ubo.putFloat(radius).putFloat(blur).putFloat(refraction).putFloat(dispersion);
            ubo.putFloat(edge).putFloat(alpha).putFloat(0).putFloat(0);
            ubo.flip();
            GpuBuffer uboBuf = device.createBuffer(() -> "liquidglass-ubo",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, ubo); // VERIFY: usage 常量名

            // 3) 顶点：面板矩形 4 角 → NDC（左上原点 px → NDC）
            ByteBuffer vb = ByteBuffer.allocateDirect(VERTS * FLOATS_PER_VERTEX * Float.BYTES).order(ByteOrder.nativeOrder());
            putQuad(vb, x, y, width, height, sw, sh);
            vb.flip();
            GpuBuffer vbo = device.createBuffer(() -> "liquidglass-vbo",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, vb);   // VERIFY: usage 常量名

            // 4) 画到主颜色目标
            try (RenderPass pass = encoder.createRenderPass(
                    () -> "liquid-glass",
                    main.getColorTextureView(),
                    OptionalInt.empty())) {       // 不清屏
                pass.setPipeline(PIPELINE);
                pass.bindTexture("Sampler0", snapshotView, sampler);
                pass.setUniform("LiquidGlass", uboBuf);
                pass.setVertexBuffer(0, vbo);
                pass.draw(0, VERTS);
            }

            uboBuf.close();
            vbo.close();
            // 直接缓冲交给 GC，不需要手动释放
        } catch (Throwable t) {
            failed = true;
            System.out.println("[LiquidGlass] 渲染失败，已停用。核对 // VERIFY 处。 " + t);
            t.printStackTrace();
        }
    }

    private static void ensureResources(GpuDevice device, int sw, int sh) {
        if (snapshotTex != null && sw == texW && sh == texH) return;
        // 释放旧的
        if (snapshotView != null) snapshotView.close();
        if (snapshotTex != null) snapshotTex.close();
        texW = sw; texH = sh;
        // VERIFY: createTexture(label, usage, format, w, h, depth/layers, mipLevels)
        snapshotTex = device.createTexture("liquidglass-snapshot",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, sw, sh, 1, 1);
        snapshotView = device.createTextureView(snapshotTex);
        if (sampler == null) {
            // VERIFY: createSampler(addrU, addrV, min, mag, maxLod, aniso)
            sampler = device.createSampler(
                    AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                    FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty()); // maxAnisotropy 必须 1~16
        }
    }

    /** 把面板矩形写成两个三角形的 NDC 顶点（z=0）。 */
    private static void putQuad(ByteBuffer b, float x, float y, float w, float h, int sw, int sh) {
        float x0 = x / sw * 2f - 1f;
        float x1 = (x + w) / sw * 2f - 1f;
        float y0 = 1f - y / sh * 2f;          // 左上原点 → NDC y 翻转
        float y1 = 1f - (y + h) / sh * 2f;
        // 三角形 1: 左上, 右上, 右下
        v(b, x0, y0); v(b, x1, y0); v(b, x1, y1);
        // 三角形 2: 左上, 右下, 左下
        v(b, x0, y0); v(b, x1, y1); v(b, x0, y1);
    }

    private static void v(ByteBuffer b, float ndcX, float ndcY) {
        b.putFloat(ndcX).putFloat(ndcY).putFloat(0f);
    }
}
