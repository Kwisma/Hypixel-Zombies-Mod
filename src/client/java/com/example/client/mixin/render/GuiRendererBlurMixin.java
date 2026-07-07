package com.example.client.mixin.render;

import com.example.client.utils.render.BlurRenderer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public class GuiRendererBlurMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void zombiesmod$captureBlurBackground(GpuBufferSlice globalSettings, CallbackInfo ci) {
        BlurRenderer.captureIfRequested();
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void zombiesmod$closeBlurResources(CallbackInfo ci) {
        BlurRenderer.close();
    }
}
