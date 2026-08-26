package com.example.client.mixin.skia;

import com.example.client.skia.fbo.GameFramebuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiRenderer.class)
public class GuiRendererTargetMixin {
    @Redirect(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
            )
    )
    private RenderTarget modid$selectGuiRenderTarget(Minecraft minecraft) {
        RenderTarget customTarget = GameFramebuffer.getActiveGuiTarget();
        return customTarget != null ? customTarget : minecraft.getMainRenderTarget();
    }
}
