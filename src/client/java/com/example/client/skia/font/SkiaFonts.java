package com.example.client.skia.font;

import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkiaFonts {
    private static final Identifier MISANS_REGULAR = Identifier.fromNamespaceAndPath(
            "zombies-mod",
            "font/misans-regular.ttf"
    );
    private static final Identifier MISANS_BOLD = Identifier.fromNamespaceAndPath(
            "zombies-mod",
            "font/misans-bold.ttf"
    );
    private static final Map<String, SkiaFont> cache = new ConcurrentHashMap<>();

    public static SkiaFont getDefaultFont(int size) {
        return get(MISANS_REGULAR, size);
    }

    public static SkiaFont getBoldFont(int size) {
        return get(MISANS_BOLD, size);
    }

    public static SkiaFont getResourceFont(Identifier identifier, int size) {
        return get(identifier, size);
    }

    private static SkiaFont get(int size) {
        return cache.computeIfAbsent("DEF", k -> SkiaFont.creatDefault(size));
    }

    private static SkiaFont get(Identifier identifier, int size) {
        String key = identifier + ":" + size;
        return cache.computeIfAbsent(key, k -> SkiaFont.create(identifier, size));
    }
}
