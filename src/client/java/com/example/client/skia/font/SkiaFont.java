package com.example.client.skia.font;

import com.example.client.skia.CanvasStack;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.shaper.Shaper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkiaFont {
    private final Paint textPaint = new Paint().setAntiAlias(true);
    private final Typeface fontTypeface;
    private final Font skiaFont;
    private final float fontHeight;
    private final float fontSize;
    private final Shaper textShaper = Shaper.make();
    private SkiaFont(Typeface typeface, float size) {
        this.fontTypeface = typeface;
        this.fontSize = size;
        this.skiaFont = new Font(typeface, size).setSubpixel(true).setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);

        FontMetrics metrics = skiaFont.getMetrics();
        this.fontHeight = metrics.getDescent() - metrics.getAscent();
    }
    private final Map<String, TextBlob> blobCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, TextBlob> eldest) {
            if (size() > 2000) {
                eldest.getValue().close();
                return true;
            }
            return false;
        }
    };
    public static SkiaFont creatDefault(int size) {
        return new SkiaFont(Typeface.makeDefault(), (float) size);
    }
    public static SkiaFont create(Identifier resourceId, int size) {
        try (InputStream stream = Minecraft.getInstance()
                .getResourceManager()
                .getResourceOrThrow(resourceId)
                .open();
             Data fontData = Data.makeFromBytes(stream.readAllBytes())) {
            Typeface typeface = Typeface.makeFromData(fontData);
            return new SkiaFont(typeface, (float) size);
        } catch (Exception e) {
            System.err.println("Failed to load Skia font resource: " + resourceId);
            e.printStackTrace();
            return new SkiaFont(Typeface.makeDefault(), (float) size);
        }
    }


    public float getWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        return skiaFont.measureTextWidth(text);
    }

    public float getHeight() {
        return fontHeight;
    }
    public void drawString(CanvasStack canvasStack, String text, float x, float y, int color) {
        render(canvasStack, text, x, y, color, false, 0f, false);
    }

    public void drawShadowString(CanvasStack canvasStack, String text, float x, float y, int color, boolean shadow) {
        render(canvasStack, text, x, y, color, shadow, 0f, false);
    }

    public void drawDynamicString(CanvasStack canvasStack, String text, float x, float y, boolean shadow, float glow) {
        render(canvasStack, text, x, y, -1, shadow, glow, true);
    }

    public void drawGlowString(CanvasStack canvasStack, String text, float x, float y, int color, boolean shadow, float glow) {
        render(canvasStack, text, x, y, color, shadow, glow, false);
    }

    private void render(CanvasStack canvasStack, String text, float x, float y, int color, boolean shadow, float glowRadius, boolean dynamic) {
        if (text == null || text.isEmpty()) return;

        List<ColorProcessor.TextPart> textParts = ColorProcessor.parse(text, color, skiaFont);
        int alpha = (color >> 24 & 0xFF);
        if (alpha == 0 && (color & 0xFFFFFF) != 0) alpha = 255;
        if (alpha <= 1) return;

        canvasStack.push();

        if (glowRadius > 0) {
            textPaint.setMaskFilter(FilterCache.getMaskBlur(glowRadius));
            drawInternal(canvasStack.canvas(), textParts, x, y, alpha, dynamic, true);
            textPaint.setMaskFilter(null);
        }

        if (shadow) {
            drawShadowInternal(canvasStack.canvas(), textParts, x + 0.5f, y + 0.5f, applyAlpha(0, (int) (alpha * 0.6f)));
        }

        drawInternal(canvasStack.canvas(), textParts, x, y, alpha, dynamic, false);
        canvasStack.pop();
    }
    public static int applyAlpha(int color, int alpha) {
        alpha = Math.clamp(alpha, 0, 255);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void drawInternal(Canvas canvas, List<ColorProcessor.TextPart> textParts, float x, float y, int alpha, boolean isDynamic, boolean isGlow) {
        float currentX = x;
        int charOffset = 0;

        for (ColorProcessor.TextPart part : textParts) {
            String content = part.content();
            TextBlob blob = blobCache.computeIfAbsent(content, k -> textShaper.shape(k, skiaFont));
            float width = part.width();

            if (isDynamic && !isGlow && (part.color() & 0xFFFFFF) == 0xFFFFFF) {
                int c1 = java.awt.Color.WHITE.getRGB();
                int c2 = java.awt.Color.BLACK.getRGB();
                try (Shader shader = Shader.makeLinearGradient(currentX, y, currentX + width, y, new int[]{c1, c2})) {
                    textPaint.setShader(shader);
                    canvas.drawTextBlob(blob, currentX, y, textPaint);
                }
                textPaint.setShader(null);
            } else {
                int c = isGlow ? (part.color() == -1 ? java.awt.Color.WHITE.getRGB() : part.color()) : part.color();
                textPaint.setColor((alpha << 24) | (c & 0xFFFFFF));
                canvas.drawTextBlob(blob, currentX, y, textPaint);
            }

            currentX += width;
            charOffset += content.length();
        }
    }

    private void drawShadowInternal(Canvas canvas, List<ColorProcessor.TextPart> textParts, float x, float y, int color) {
        float currentX = x;
        textPaint.setColor(color);
        for (ColorProcessor.TextPart part : textParts) {
            TextBlob blob = blobCache.get(part.content());
            if (blob != null) canvas.drawTextBlob(blob, currentX, y, textPaint);
            currentX += part.width();
        }
    }
}
