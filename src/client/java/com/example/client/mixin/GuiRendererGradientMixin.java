package com.example.client.mixin;

import com.example.client.utils.render.GradientTextRenderer;
import com.example.client.utils.render.GradientTextStateAccess;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 在延迟字形创建期间恢复对应文字提交时保存的渐变参数。 */
@Mixin(GuiRenderer.class)
public class GuiRendererGradientMixin {
    @Redirect(
            method = "lambda$prepareText$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font$PreparedText;visit(Lnet/minecraft/client/gui/Font$GlyphVisitor;)V"
            )
    )
    private void zombiesmod$visitWithGradient(Font.PreparedText preparedText, Font.GlyphVisitor visitor,
                                              GuiTextRenderState textState) {
        GradientTextRenderer.Context context =
                ((GradientTextStateAccess) (Object) textState).zombiesmod$getGradientContext();
        if (context == null) {
            preparedText.visit(visitor);
            return;
        }

        try (GradientTextRenderer.Scope ignored = GradientTextRenderer.push(context)) {
            preparedText.visit(visitor);
        }
    }
}
