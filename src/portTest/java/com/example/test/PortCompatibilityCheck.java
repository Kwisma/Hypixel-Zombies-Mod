package com.example.test;

import com.example.client.config.KeyCodeMigration;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Runs inside Fabric before Minecraft starts, so no account, assets or display are needed. */
public final class PortCompatibilityCheck implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        try {
            if (!FabricLoader.getInstance().isModLoaded("zombies-mod")) {
                throw new AssertionError("The production mod must be present in the test runtime");
            }
            ClassLoader loader = getClass().getClassLoader();
            Set<String> targets = new LinkedHashSet<>();
            int mixins = 0;
            for (String config : List.of("zombies-mod.mixins.json", "zombies-mod.client.mixins.json",
                    "zombies-mod.chams.mixins.json")) {
                JsonObject json;
                try (InputStream stream = requireResource(loader, config);
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
                String prefix = json.get("package").getAsString();
                for (String section : List.of("mixins", "client")) {
                    if (!json.has(section)) continue;
                    for (JsonElement element : json.getAsJsonArray(section)) {
                        String resource = (prefix + "." + element.getAsString()).replace('.', '/') + ".class";
                        ClassNode node = new ClassNode();
                        try (InputStream stream = requireResource(loader, resource)) {
                            new ClassReader(stream).accept(node,
                                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        }
                        collectTargets(node.invisibleAnnotations, targets);
                        collectTargets(node.visibleAnnotations, targets);
                        mixins++;
                    }
                }
            }
            for (String target : targets) {
                // Class loading applies and validates the actual Fabric/Mixin transformations.
                Class.forName(target, false, loader);
            }
            checkKeyBindings();
            ReflectionCompatibilityCheck.run();
            int shaders = ShaderCompatibilityCheck.run();
            if (Boolean.getBoolean("zombiesmod.test.chamsPixels")) {
                ChamsTransparencyCheck.run();
            }
            System.out.println("PORT CHECK PASSED: " + mixins + " mixins, " + targets.size()
                    + " target classes, " + shaders
                    + " shader stages, legacy and current key bindings, reflective event/setting access");
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static InputStream requireResource(ClassLoader loader, String resource) {
        InputStream stream = loader.getResourceAsStream(resource);
        if (stream == null) throw new AssertionError("Missing resource: " + resource);
        return stream;
    }

    private static void collectTargets(List<AnnotationNode> annotations, Set<String> targets) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) continue;
            for (int i = 0; i < annotation.values.size(); i += 2) {
                String key = (String) annotation.values.get(i);
                if (!key.equals("value") && !key.equals("targets")) continue;
                for (Object value : (List<?>) annotation.values.get(i + 1)) {
                    targets.add(value instanceof Type type ? type.getClassName() : (String) value);
                }
            }
        }
    }

    private static void checkKeyBindings() {
        // Known physical keys, including non-contiguous number/keypad groups and modifier keys.
        int[][] cases = {{0, 0}, {65, 4}, {90, 29}, {48, 39}, {49, 30}, {256, 41},
                {290, 58}, {301, 69}, {302, 104}, {313, 115}, {320, 98}, {321, 89},
                {335, 88}, {340, 225}, {343, 227}, {344, 229}, {347, 231}, {-1, 0}, {314, 0}};
        JsonObject legacy = new JsonObject();
        JsonObject current = new JsonObject();
        current.addProperty("keyCodeFormat", KeyCodeMigration.FORMAT);
        for (int[] pair : cases) {
            check(KeyCodeMigration.readKey(legacy, pair[0]) == pair[1], "Legacy key " + pair[0]);
            check(KeyCodeMigration.readKey(current, pair[1]) == pair[1], "Current key " + pair[1]);
        }
        // A current SDL code must not be reinterpreted as an old GLFW code on subsequent loads.
        check(KeyCodeMigration.readKey(current, 65) == 65, "Current key format must be idempotent");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
