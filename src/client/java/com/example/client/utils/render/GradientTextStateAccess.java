package com.example.client.utils.render;

/** 由 GuiTextRenderState Mixin 实现，用于跨越延迟文字提取阶段保存渐变参数。 */
public interface GradientTextStateAccess {
    GradientTextRenderer.Context zombiesmod$getGradientContext();
}
