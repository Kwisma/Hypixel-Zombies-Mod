package com.example.client.skia.fbo;

import com.example.client.mixin.skia.GameRendererAccessor;
import com.example.client.skia.CanvasStack;
import com.example.client.skia.Skia;
import com.example.client.skia.image.ImageInGame;
import com.example.client.skia.render.RenderUtils;
import com.example.client.utils.IMinecraft;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.humbleui.skija.Image;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class GameFramebuffer implements AutoCloseable, IMinecraft {
    private static final Map<String, GameFramebuffer> FRAMEBUFFERS = new LinkedHashMap<>();
    private static RenderTarget activeGuiTarget;

    @FunctionalInterface
    public interface GuiLayerRenderer {
        void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
    }

    public static GameFramebuffer registerFramebuffer(String id) {
        GameFramebuffer existing = FRAMEBUFFERS.get(id);
        if (existing != null) return existing;

        GameFramebuffer framebuffer = new GameFramebuffer(id);
        FRAMEBUFFERS.put(id, framebuffer);
        return framebuffer;
    }

    public static GameFramebuffer getFramebuffer(String id) {
        return FRAMEBUFFERS.get(id);
    }

    public static void removeFramebuffer(String id) {
        GameFramebuffer framebuffer = FRAMEBUFFERS.remove(id);
        if (framebuffer != null) framebuffer.close();
    }

    public static void renderAll(DeltaTracker deltaTracker) {
        RenderSystem.assertOnRenderThread();

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        for (GameFramebuffer framebuffer : List.copyOf(FRAMEBUFFERS.values())) {
            framebuffer.renderFrame(width, height, deltaTracker);
        }
    }

    public static void cleanupAll() {
        RenderSystem.assertOnRenderThread();
        List.copyOf(FRAMEBUFFERS.values()).forEach(GameFramebuffer::close);
        FRAMEBUFFERS.clear();
    }

    public static RenderTarget getActiveGuiTarget() {
        return activeGuiTarget;
    }

    public static boolean isRenderingCustomGui() {
        return activeGuiTarget != null;
    }

    private final String id;
    private final GuiRenderState guiRenderState = new GuiRenderState();
    private TextureTarget target;
    private GuiRenderer guiRenderer;
    private GuiLayerRenderer renderer;

    private GameFramebuffer(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Framebuffer id cannot be blank");
        }
        this.id = id;
    }

    public GameFramebuffer setRenderer(GuiLayerRenderer renderer) {
        this.renderer = renderer;
        return this;
    }
    public void render(CanvasStack canvasStack) {
        Image image = ImageInGame.getCachedImage(Skia.getColorTextureId(target()), target().width, target().height);

        RenderUtils.drawImage(canvasStack,image, 0,0,target().width, target().height,0);
    }
    public RenderTarget target() {
        if (target == null) {
            throw new IllegalStateException("Framebuffer has not rendered yet: " + id);
        }
        return target;
    }

    private void renderFrame(int width, int height, DeltaTracker deltaTracker) {
        ensureResources(width, height);
        clear();

        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(
                mc,
                guiRenderState,
                0,
                0
        );
        if (renderer != null) {
            renderer.render(graphics, deltaTracker);
        }

        GameRendererAccessor accessor = (GameRendererAccessor) mc.gameRenderer;
        GpuBufferSlice fogBuffer = accessor.modid$getFogRenderer()
                .getBuffer(FogRenderer.FogMode.NONE);

        if (activeGuiTarget != null) {
            throw new IllegalStateException("Nested custom GUI framebuffer rendering is not supported");
        }

        activeGuiTarget = target;
        try {
            guiRenderer.render(fogBuffer);
            guiRenderer.endFrame();
        } finally {
            activeGuiTarget = null;
            guiRenderState.reset();
        }
    }

    private void ensureResources(int width, int height) {
        if (target == null) {
            target = new TextureTarget("modid/" + id, width, height, false);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height);
        }

        if (guiRenderer == null) {
            GameRendererAccessor accessor = (GameRendererAccessor) mc.gameRenderer;
            guiRenderer = new GuiRenderer(
                    guiRenderState,
                    accessor.modid$getRenderBuffers().bufferSource(),
                    accessor.modid$getSubmitNodeStorage(),
                    accessor.modid$getFeatureRenderDispatcher(),
                    List.of()
            );
        }
    }

    private void clear() {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.clearColorTexture(target.getColorTexture(), 0x00000000);
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (guiRenderer != null) {
            guiRenderer.close();
            guiRenderer = null;
        }
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
