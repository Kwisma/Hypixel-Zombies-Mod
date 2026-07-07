package com.example.client.utils.render;

import com.example.client.events.RenderEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Zombies Mod 自定义 TTF 文字渲染器。
 *
 * 使用多档原生字号的 TTF 图集。传入字号会吸附到最近的预设档位，绘制时不使用
 * pose 缩放，并通过线性过滤采样自定义字体图集，避免模糊与最近邻锯齿。
 */
public final class CustomFontRenderer {
    public static final float MIN_SIZE = 12.0F;
    public static final float MAX_SIZE = 128.0F;
    private record FontFace(float size, FontDescription font) {
    }

    private static final FontFace[] FACES = {
            face(12.0F, "hud_12"),
            face(16.0F, "hud_16"),
            face(18.0F, "hud"),
            face(24.0F, "hud_24"),
            face(32.0F, "hud_32"),
            face(48.0F, "hud_48"),
            face(64.0F, "hud_64"),
            face(96.0F, "hud_96"),
            face(128.0F, "hud_128")
    };

    private CustomFontRenderer() {
    }

    public static void draw(
            RenderEvent event,
            String text,
            float x,
            float y,
            float size,
            int color
    ) {
        draw(event, text, x, y, size, color, false);
    }

    public static void draw(
            RenderEvent event,
            String text,
            float x,
            float y,
            float size,
            int color,
            boolean shadow
    ) {
        if (event == null || text == null || text.isEmpty() || !validNumber(x)
                || !validNumber(y) || !validNumber(size) || size <= 0.0F) {
            return;
        }

        FontFace face = nearestFace(Math.clamp(size, MIN_SIZE, MAX_SIZE));
        Component component = component(text, face.font());

        if (shadow) {
            drawPass(event.getGuiGraphicsExtractor(), component, x + 1.0F, y + 1.0F, shadowColor(color));
        }
        drawPass(event.getGuiGraphicsExtractor(), component, x, y, color);
    }

    public static void drawCentered(
            RenderEvent event,
            String text,
            float centerX,
            float y,
            float size,
            int color,
            boolean shadow
    ) {
        draw(event, text, centerX - width(text, size) / 2.0F, y, size, color, shadow);
    }

    /** 返回目标字号下的实际 GUI 宽度。 */
    public static float width(String text, float size) {
        if (text == null || text.isEmpty() || !validNumber(size) || size <= 0.0F) {
            return 0.0F;
        }
        FontFace face = nearestFace(Math.clamp(size, MIN_SIZE, MAX_SIZE));
        return Minecraft.getInstance().font.width(component(text, face.font()));
    }

    /** 返回用于 HUD 排版的目标行高。 */
    public static float height(float size) {
        if (!validNumber(size) || size <= 0.0F) {
            return 0.0F;
        }
        return nearestFace(Math.clamp(size, MIN_SIZE, MAX_SIZE)).size();
    }

    private static Component component(String text, FontDescription font) {
        return Component.literal(text).withStyle(style -> style.withFont(font));
    }

    private static FontFace face(float size, String resource) {
        return new FontFace(size, new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("zombies-mod", resource)));
    }

    private static FontFace nearestFace(float targetSize) {
        FontFace nearest = FACES[0];
        float nearestDistance = Math.abs(targetSize - nearest.size());
        for (int i = 1; i < FACES.length; i++) {
            FontFace candidate = FACES[i];
            float distance = Math.abs(targetSize - candidate.size());
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static void drawPass(
            GuiGraphicsExtractor graphics,
            Component text,
            float x,
            float y,
            int color
    ) {
        if (graphics == null) {
            return;
        }

        graphics.text(Minecraft.getInstance().font, text, Math.round(x), Math.round(y), color, false);
    }

    private static int shadowColor(int color) {
        int sourceAlpha = (color >>> 24) & 0xFF;
        int shadowAlpha = Math.round(sourceAlpha * 0.45F);
        return shadowAlpha << 24;
    }

    private static boolean validNumber(float value) {
        return Float.isFinite(value);
    }
}
