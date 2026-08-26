package com.example.client.mixin.skia;

import com.example.client.skia.Skia;
import com.example.client.skia.fbo.GameFramebuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Unique
    private boolean modid$renderedSkiaThisFrame;

    @Inject(
            method = "render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            at = @At("HEAD")
    )
    private void modid$beginGuiFrame(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        modid$renderedSkiaThisFrame = false;
    }

    @Inject(
            method = "executeDrawRange",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/blaze3d/systems/RenderPass;",
                    shift = At.Shift.AFTER
            )
    )
    private void modid$renderSkiaBelowGui(CallbackInfo ci) {
        if (!GameFramebuffer.isRenderingCustomGui() && !modid$renderedSkiaThisFrame) {
            modid$renderedSkiaThisFrame = true;
            Skia.tick();
        }
    }
}
