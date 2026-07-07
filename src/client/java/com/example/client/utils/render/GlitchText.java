package com.example.client.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Random;

/**
 * 故障(glitch)文字：白色基底 + RGB 错位 + 随机横切片位移，按状态机周期性抖动。
 * 移植自 ImGui 版本。状态是 static(全局一份动画)，单个 HUD 标题够用。
 *
 * 改大小：用 scale 参数(基于 MC 默认字体放大)。文字走 pose 缩放，切片裁剪按缩放后屏幕像素算，
 * 所以不能在外面整体缩放——要缩放就传 scale 进来。
 *
 * 用法：GlitchText.draw(g, mc.font, x, y, "ZOMBIES", 3f);  // 3 倍大
 * 居中：int x = centerX - (int)(mc.font.width(text) * scale) / 2;
 */
public final class GlitchText {
    private GlitchText() {}

    private enum Phase { IDLE, SMALL, PAUSE, BIG }

    private static Phase phase = Phase.IDLE;
    private static double phaseEnd = 0, nextStart = 0, nextUpdate = 0;

    private static final float[] sliceY = new float[8];
    private static final float[] sliceH = new float[8];
    private static final float[] sliceShift = new float[8];
    private static int sliceCount = 0;
    private static float rgbOffset = 0;

    private static final Random RAND = new Random();

    /** scale=1 的便捷重载。 */
    public static void draw(GuiGraphicsExtractor g, Font font, int x, int y, String text) {
        draw(g, font, x, y, text, 1f);
    }

    public static void draw(GuiGraphicsExtractor g, Font font, int x, int y, String text, float scale) {
        double t = System.currentTimeMillis() / 1000.0;
        int h = Math.max(1, (int) (font.lineHeight * scale)); // 缩放后文字像素高，用来定切片范围

        // ---- 状态机：Idle → Small(0.07) → Pause(0.07) → Big(0.18) → Idle(2.5~4.5s) ----
        if (phase == Phase.IDLE && t > nextStart) { phase = Phase.SMALL; phaseEnd = t + 0.07; }
        if (phase == Phase.SMALL && t > phaseEnd) { phase = Phase.PAUSE; phaseEnd = t + 0.07; }
        if (phase == Phase.PAUSE && t > phaseEnd) { phase = Phase.BIG;   phaseEnd = t + 0.18; }
        if (phase == Phase.BIG && t > phaseEnd)   { phase = Phase.IDLE;  nextStart = t + 2.5 + RAND.nextInt(200) / 100.0; }

        // 基底白字
        drawScaled(g, font, text, x, y, 0xFFFFFFFF, scale);

        if (phase == Phase.IDLE) return;

        // ---- glitch 参数每 0.06s 刷新（位移量乘 scale，随字号成比例）----
        if (t > nextUpdate) {
            nextUpdate = t + 0.06;
            if (phase == Phase.SMALL) { sliceCount = 2; rgbOffset = 1.5f * scale; }
            else                      { sliceCount = 5; rgbOffset = 3.0f * scale; }

            for (int i = 0; i < sliceCount; i++) {
                sliceY[i] = y + RAND.nextInt(h);                       // 切片在文字高度内（屏幕px）
                sliceH[i] = 1 + RAND.nextInt(Math.max(1, h / 3));      // 切片厚度
                sliceShift[i] = (phase == Phase.SMALL)
                        ? (RAND.nextInt(8) - 4) * scale
                        : (RAND.nextInt(24) - 12) * scale;
            }
        }

        // ---- RGB 错位（红 / 青，半透明，叠在白字上）----
        drawScaled(g, font, text, Math.round(x - rgbOffset), y, 0xB4FF0000, scale); // 红 alpha180
        drawScaled(g, font, text, Math.round(x + rgbOffset), y, 0xB400FFFF, scale); // 青

        // ---- 横切片：裁剪到一条横带（屏幕px），里面画位移后的白字 ----
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        for (int i = 0; i < sliceCount; i++) {
            int y1 = (int) sliceY[i];
            int y2 = (int) (sliceY[i] + sliceH[i]);
            g.enableScissor(0, y1, screenW, y2);
            drawScaled(g, font, text, Math.round(x + sliceShift[i]), y, 0xFFFFFFFF, scale);
            g.disableScissor();
        }
    }

    /** 用 pose 缩放画一段文字（scale=1 时直接画，省一次矩阵）。 */
    private static void drawScaled(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color, float scale) {
        if (scale == 1f) {
            g.text(font, text, x, y, color, true);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font, text, 0, 0, color, true);
        g.pose().popMatrix();
    }
}
