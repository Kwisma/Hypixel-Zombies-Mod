package com.example.client.mixin;

import com.example.client.utils.render.GradientTextRenderer;
import com.example.client.utils.render.GradientTextStateAccess;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在 graphics.text() 提交时，把当前渐变保存到延迟文字状态。 */
@Mixin(GuiTextRenderState.class)
public class GuiTextRenderStateGradientMixin implements GradientTextStateAccess {
    @Unique
    private GradientTextRenderer.Context zombiesmod$gradientContext;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void zombiesmod$captureGradient(Font font, FormattedCharSequence text, Matrix3x2fc pose,
                                             int x, int y, int color, int backgroundColor,
                                             boolean dropShadow, boolean includeEmpty,
                                             ScreenRectangle scissor, CallbackInfo ci) {
        zombiesmod$gradientContext = GradientTextRenderer.currentContext();
    }

    @Override
    public GradientTextRenderer.Context zombiesmod$getGradientContext() {
        return zombiesmod$gradientContext;
    }
}
