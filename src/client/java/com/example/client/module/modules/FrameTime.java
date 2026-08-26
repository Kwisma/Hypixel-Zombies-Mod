package com.example.client.module.modules;

import com.darkmagician6.eventapi.EventTarget;
import com.example.client.events.SkiaEvent;
import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import com.example.client.setting.annotation.SettingInfo;
import com.example.client.setting.settings.NumberSetting;
import com.example.client.skia.CanvasStack;
import com.example.client.skia.font.FilterCache;
import com.example.client.skia.font.SkiaFont;
import com.example.client.skia.font.SkiaFonts;
import com.example.client.skia.render.RenderUtils;
import io.github.humbleui.skija.*;

import java.util.Locale;

@ModuleInfo(name = {
        @Text(label = "FrameTime", language = Language.English),
        @Text(label = "帧生成时间", language = Language.Chinese)
}, enable = true)
public class FrameTime extends AbstractModule {
    private static final int SAMPLE_COUNT = 120;
    private static final long AVERAGE_WINDOW_NANOS = 200_000_000L;
    private static final float GRAPH_MAX_MS = 50F;
    private static final float SIXTY_FPS_MS = 1000F / 60F;
    private static final float THIRTY_FPS_MS = 1000F / 30F;

    private static final float PANEL_WIDTH = 190F;
    private static final float PANEL_HEIGHT = 76F;
    private static final float PANEL_RADIUS = 7F;
    private static final float GRAPH_X_OFFSET = 7F;
    private static final float GRAPH_Y_OFFSET = 25F;
    private static final float GRAPH_WIDTH = PANEL_WIDTH - 14F;
    private static final float GRAPH_HEIGHT = 43F;

    private static final int PANEL_COLOR = 0xB81A1E24;
    private static final int PANEL_HIGHLIGHT = 0x70343C46;
    private static final int GRAPH_BACKGROUND = 0xA012171C;
    private static final int GRID_COLOR = 0x304F5B66;
    private static final int TEXT_COLOR = 0xFFF2F5F7;
    private static final int MUTED_TEXT_COLOR = 0xFF98A5B1;
    private static final int SMOOTH_COLOR = 0xFF64FF91;
    private static final int WARNING_COLOR = 0xFFFFC857;
    private static final int SPIKE_COLOR = 0xFFFF5A67;

    @SettingInfo(name = {
            @Text(label = "X", language = Language.English),
            @Text(label = "X", language = Language.Chinese)
    })
    public static final NumberSetting posX = new NumberSetting(0.02, 0, 1, "#.00");

    @SettingInfo(name = {
            @Text(label = "Y", language = Language.English),
            @Text(label = "Y", language = Language.Chinese)
    })
    public static final NumberSetting posY = new NumberSetting(0.18, 0, 1, "#.00");

    private final float[] samples = new float[SAMPLE_COUNT];
    private final Paint graphPaint = new Paint();
    private int writeIndex;
    private int sampleSize;
    private long lastFrameNanos;
    private long averageWindowStartNanos;
    private double averageWindowTotalMs;
    private int averageWindowFrameCount;
    private float currentMs;
    private float displayMs;

    public FrameTime() {
        registerSetting(posX, posY);
    }

    @EventTarget
    public void onRenderSkia(SkiaEvent event) {
        sampleFrameTime();
        if (mc.player == null || mc.level == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float x = Math.clamp(
                screenWidth * posX.getValue().floatValue(),
                2F,
                Math.max(2F, screenWidth - PANEL_WIDTH - 2F)
        );
        float y = Math.clamp(
                screenHeight * posY.getValue().floatValue(),
                2F,
                Math.max(2F, screenHeight - PANEL_HEIGHT - 2F)
        );

        drawPanel(event.getCanvasStack(), x, y);
    }

    private void sampleFrameTime() {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            averageWindowStartNanos = now;
            return;
        }

        float elapsedMs = (now - lastFrameNanos) / 1_000_000F;
        lastFrameNanos = now;
        if (elapsedMs <= 0F) return;

        averageWindowTotalMs += Math.min(elapsedMs, 250F);
        averageWindowFrameCount++;

        if (now - averageWindowStartNanos >= AVERAGE_WINDOW_NANOS) {
            currentMs = (float) (averageWindowTotalMs / averageWindowFrameCount);
            displayMs = currentMs;

            samples[writeIndex] = currentMs;
            writeIndex = (writeIndex + 1) % SAMPLE_COUNT;
            sampleSize = Math.min(sampleSize + 1, SAMPLE_COUNT);

            averageWindowStartNanos = now;
            averageWindowTotalMs = 0D;
            averageWindowFrameCount = 0;
        }
    }

    private void drawPanel(CanvasStack canvasStack, float x, float y) {
        SkiaFont titleFont = SkiaFonts.getBoldFont(7);
        SkiaFont valueFont = SkiaFonts.getDefaultFont(8);
        SkiaFont smallFont = SkiaFonts.getDefaultFont(5);
        RenderUtils.drawShadow(
                canvasStack, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS,
                java.awt.Color.BLACK.getRGB()
        );
        RenderUtils.drawRect(canvasStack, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS, PANEL_COLOR);
//        RenderUtils.drawRect(canvasStack, x + 1F, y + 1F, PANEL_WIDTH - 2F, 1F, 1F, PANEL_HIGHLIGHT);

        int currentColor = colorForFrameTime(displayMs);
        RenderUtils.drawRect(canvasStack, x + 7F, y + 6F, 2F, 8F, 1F, currentColor);
        titleFont.drawShadowString(canvasStack, "FRAME TIME", x + 12F, y + 4F, TEXT_COLOR, true);

        String frameText = String.format(Locale.ROOT, "%.1f ms", displayMs);
        valueFont.drawShadowString(
                canvasStack,
                frameText,
                x + PANEL_WIDTH - 7F - valueFont.getWidth(frameText),
                y + 3F,
                currentColor,
                true
        );

        int fps = displayMs > 0F ? Math.round(1000F / displayMs) : 0;
        String statistics = fps + " FPS   200ms AVG   PEAK " + String.format(Locale.ROOT, "%.1f ms", getPeakMs());
        smallFont.drawShadowString(canvasStack, statistics, x + 12F, y + 14F, MUTED_TEXT_COLOR, true);

        float graphX = x + GRAPH_X_OFFSET;
        float graphY = y + GRAPH_Y_OFFSET;
        RenderUtils.drawRect(
                canvasStack, graphX, graphY, GRAPH_WIDTH, GRAPH_HEIGHT, 3F, GRAPH_BACKGROUND
        );
        drawGrid(canvasStack, smallFont, graphX, graphY);
        drawGraph(canvasStack, graphX, graphY);
    }

    private void drawGrid(CanvasStack canvasStack, SkiaFont smallFont, float x, float y) {
        float sixtyFpsY = frameTimeToY(SIXTY_FPS_MS, y);
        float thirtyFpsY = frameTimeToY(THIRTY_FPS_MS, y);

        for (int column = 1; column < 4; column++) {
            float gridX = x + GRAPH_WIDTH * column / 4F;
            RenderUtils.drawRect(canvasStack, gridX, y + 2F, 0.5F, GRAPH_HEIGHT - 4F, 0F, GRID_COLOR);
        }
        RenderUtils.drawRect(canvasStack, x + 2F, sixtyFpsY, GRAPH_WIDTH - 4F, 0.5F, 0F, 0x405DCB80);
        RenderUtils.drawRect(canvasStack, x + 2F, thirtyFpsY, GRAPH_WIDTH - 4F, 0.5F, 0F, 0x50E6A94A);

        smallFont.drawShadowString(canvasStack, "16.7", x + 3F, sixtyFpsY - 5F, 0xA064FF91, false);
        smallFont.drawShadowString(canvasStack, "33.3", x + 3F, thirtyFpsY - 5F, 0xA0FFC857, false);
    }

    private void drawGraph(CanvasStack canvasStack, float graphX, float graphY) {
        if (sampleSize < 2) return;

        try (Path smoothPath = new Path();
             Path warningPath = new Path();
             Path spikePath = new Path()) {
            float step = GRAPH_WIDTH / (SAMPLE_COUNT - 1F);
            float startX = graphX + GRAPH_WIDTH - step * (sampleSize - 1);
            float previousX = startX;
            float previousValue = getSample(0);
            float previousY = frameTimeToY(previousValue, graphY);

            for (int i = 1; i < sampleSize; i++) {
                float value = getSample(i);
                float pointX = startX + step * i;
                float pointY = frameTimeToY(value, graphY);
                Path targetPath = pathForFrameTime(Math.max(previousValue, value), smoothPath, warningPath, spikePath);
                targetPath.moveTo(previousX, previousY).lineTo(pointX, pointY);

                previousX = pointX;
                previousY = pointY;
                previousValue = value;
            }

            drawSignalPath(canvasStack, smoothPath, SMOOTH_COLOR);
            drawSignalPath(canvasStack, warningPath, WARNING_COLOR);
            drawSignalPath(canvasStack, spikePath, SPIKE_COLOR);

            graphPaint.reset();
            graphPaint.setAntiAlias(true);
            graphPaint.setColor(colorForFrameTime(currentMs));
            canvasStack.canvas().drawCircle(previousX, previousY, 1.6F, graphPaint);
        }
    }

    private void drawSignalPath(CanvasStack canvasStack, Path path, int color) {
        graphPaint.reset();
        graphPaint.setAntiAlias(true);
        graphPaint.setMode(PaintMode.STROKE);
        graphPaint.setStrokeCap(PaintStrokeCap.ROUND);
        graphPaint.setStrokeJoin(PaintStrokeJoin.ROUND);

        graphPaint.setColor((color & 0x00FFFFFF) | 0x50000000);
        graphPaint.setStrokeWidth(3F);
        graphPaint.setMaskFilter(FilterCache.getMaskBlur(1.25F));
        canvasStack.canvas().drawPath(path, graphPaint);

        graphPaint.setMaskFilter(null);
        graphPaint.setColor(color);
        graphPaint.setStrokeWidth(1.2F);
        canvasStack.canvas().drawPath(path, graphPaint);
    }

    private Path pathForFrameTime(float milliseconds, Path smooth, Path warning, Path spike) {
        if (milliseconds > THIRTY_FPS_MS) return spike;
        if (milliseconds > SIXTY_FPS_MS) return warning;
        return smooth;
    }

    private int colorForFrameTime(float milliseconds) {
        if (milliseconds > THIRTY_FPS_MS) return SPIKE_COLOR;
        if (milliseconds > SIXTY_FPS_MS) return WARNING_COLOR;
        return SMOOTH_COLOR;
    }

    private float frameTimeToY(float milliseconds, float graphY) {
        float normalized = Math.clamp(milliseconds / GRAPH_MAX_MS, 0F, 1F);
        return graphY + GRAPH_HEIGHT - 3F - normalized * (GRAPH_HEIGHT - 6F);
    }

    private float getSample(int chronologicalIndex) {
        int oldestIndex = (writeIndex - sampleSize + SAMPLE_COUNT) % SAMPLE_COUNT;
        return samples[(oldestIndex + chronologicalIndex) % SAMPLE_COUNT];
    }

    private float getPeakMs() {
        float peak = 0F;
        for (int i = 0; i < sampleSize; i++) {
            peak = Math.max(peak, getSample(i));
        }
        return peak;
    }

    @Override
    protected void onDisable() {
        lastFrameNanos = 0L;
        averageWindowStartNanos = 0L;
        averageWindowTotalMs = 0D;
        averageWindowFrameCount = 0;
        currentMs = 0F;
        displayMs = 0F;
        writeIndex = 0;
        sampleSize = 0;
    }

    @Override
    public void cleanup() {
        graphPaint.close();
    }
}
