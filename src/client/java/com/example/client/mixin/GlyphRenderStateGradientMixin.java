package com.example.client.mixin;

import com.example.client.utils.render.GradientTextRenderer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 将文字提取时的渐变参数保存到字形，并在生成顶点时写入上下颜色。 */
@Mixin(GlyphRenderState.class)
public class GlyphRenderStateGradientMixin {
    @Unique
    private GradientTextRenderer.Context zombiesmod$gradient;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void zombiesmod$captureGradient(Matrix3x2fc pose, TextRenderable renderable,
                                             ScreenRectangle scissorArea, CallbackInfo ci) {
        zombiesmod$gradient = GradientTextRenderer.currentContext();
    }

    @Redirect(
            method = "buildVertices",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/TextRenderable;render(Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IZ)V"
            )
    )
    private void zombiesmod$renderGradient(TextRenderable renderable, Matrix4fc pose,
                                           VertexConsumer consumer, int light, boolean inverseDepth) {
        VertexConsumer output = zombiesmod$gradient == null
                ? consumer
                : GradientTextRenderer.wrap(consumer, zombiesmod$gradient);
        renderable.render(pose, output, light, inverseDepth);
    }
}
