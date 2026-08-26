package com.example.client.skia.image;

import com.example.client.skia.Skia;

import com.example.client.utils.IMinecraft;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.SurfaceOrigin;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.lwjgl.opengl.GL11;

public final class ImageInGame implements IMinecraft {
    private static final Long2ObjectOpenHashMap<Image> textureCache = new Long2ObjectOpenHashMap<>();

    public static Image getCachedImage(int textureId,  int width, int height) {
        return getCachedImage(Skia.getContext(), textureId, width, height, SurfaceOrigin.BOTTOM_LEFT, ColorType.RGBA_8888);
    }
    public static Image getCachedImage(DirectContext context, int textureId, int width, int height, SurfaceOrigin origin, ColorType colorType) {
        long key = ((long) textureId << 32) | (long) (width & 0xFFFF) << 16 | (height & 0xFFFF);

        Image image = textureCache.get(key);

        if (image == null || image.isClosed() || image.getWidth() != width || image.getHeight() != height) {
            if (image != null && !image.isClosed()) {
                image.close();
            }

            try {
                if (context == null) return null;

                image = Image.adoptGLTextureFrom(context, textureId, GL11.GL_TEXTURE_2D, width, height, GL11.GL_RGBA8, origin, colorType);
                textureCache.put(key, image);
            } catch (Exception e) {
                return null;
            }
        }

        return image;
    }


    public static void cleanup() {
        textureCache.values().forEach(img -> {
            if (img != null && !img.isClosed()) {
                img.close();
            }
        });
        textureCache.clear();
    }
}
