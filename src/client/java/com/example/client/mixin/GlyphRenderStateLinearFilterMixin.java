package com.example.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 在 GUI 最终绑定纹理时，根据当前 GUI Scale 为 HUD 字体选择采样方式。 */
@Mixin(GlyphRenderState.class)
public class GlyphRenderStateLinearFilterMixin {
    @Shadow
    @Final
    private TextRenderable renderable;

    @Inject(method = "textureSetup", at = @At("HEAD"), cancellable = true)
    private void zombiesmod$useLinearFontFiltering(CallbackInfoReturnable<TextureSetup> cir) {
        GpuTextureView textureView = renderable.textureView();
        String label = textureView.texture().getLabel();
        if (label != null && label.contains("zombies-mod") && label.contains("hud")) {
            int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            // 1x 下 LINEAR 会把高超采样字形过度平滑；放大后才需要 LINEAR 消除像素块。
            FilterMode filter = guiScale <= 1 ? FilterMode.NEAREST : FilterMode.LINEAR;
            cir.setReturnValue(TextureSetup.singleTextureWithLightmap(
                    textureView,
                    RenderSystem.getSamplerCache().getClampToEdge(filter)
            ));
        }
    }
}
