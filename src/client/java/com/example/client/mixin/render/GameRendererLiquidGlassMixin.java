package com.example.client.mixin.render;

import com.example.client.module.modules.LiquidGlassTest;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 GameRenderer.render(...) 的 TAIL 调用液态玻璃绘制：
 * 此处世界 + GUI 都已渲染进主目标、尚未呈现到屏幕，立即模式画的覆盖层能正常显示在最上层。
 */
@Mixin(GameRenderer.class)
public class GameRendererLiquidGlassMixin {

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("TAIL"))
    private void zombiesmod$liquidGlass(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        LiquidGlassTest.renderIfEnabled();
    }
}
