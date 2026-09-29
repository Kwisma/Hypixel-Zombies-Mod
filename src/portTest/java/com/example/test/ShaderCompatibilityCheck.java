package com.example.test;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.DeviceType;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.device.HintsAndWorkarounds;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates render-pass bindings and compiles production shaders without a GPU context. */
final class ShaderCompatibilityCheck {
    static int run() throws Exception {
        List<RenderPipeline> pipelines = new ArrayList<>();
        pipelines.add(pipelineField("com.example.client.chams.ChamsRenderType", "DEPTH_SEED_PIPELINE"));
        pipelines.add(pipelineField("com.example.client.newrender.LiquidGlass", "PIPELINE"));
        Method blur = Class.forName("com.example.client.utils.render.BlurRenderer")
                .getDeclaredMethod("pipelineFor", int.class);
        blur.setAccessible(true);
        for (int radius = 1; radius <= 10; radius++) {
            pipelines.add((RenderPipeline) blur.invoke(null, radius));
        }
        int compiled = 0;
        // The compiler only queries driver workarounds. Reject any actual GPU operation.
        DeviceInfo info = new DeviceInfo("Shader test", "Test", "", true, "Test", 1F,
                null, null, Set.of(), new HintsAndWorkarounds(false, false, false, false), DeviceType.CPU);
        RenderPassCompatibilityCheck.run(pipelines, info);
        GpuDevice device = (GpuDevice) Proxy.newProxyInstance(GpuDevice.class.getClassLoader(),
                new Class<?>[]{GpuDevice.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getDeviceInfo")) return info;
                    throw new AssertionError("Unexpected GPU access: " + method.getName());
                });
        Field deviceField = RenderSystem.class.getDeclaredField("DEVICE");
        deviceField.setAccessible(true);
        Object previousDevice = deviceField.get(null);
        deviceField.set(null, device);
        try (ClasspathShaderSource source = new ClasspathShaderSource()) {
            for (boolean zeroToOne : new boolean[]{false, true}) {
                try (GlslCompiler compiler = new GlslCompiler(zeroToOne, true)) {
                    for (RenderPipeline pipeline : pipelines) {
                        Set<String> uniforms = new HashSet<>();
                        for (var uniform : BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts())) {
                            uniforms.add(uniform.name());
                        }
                        for (var shader : pipeline.getShaders().entrySet()) {
                            try (var module = compiler.compileToSpv(shader.getValue().toString(),
                                    source.getShader(shader.getValue(), shader.getKey()), shader.getKey(),
                                    pipeline.getShaderDefines(), source)) {
                                for (var descriptor : module.reflect().descriptors()) {
                                    if (!uniforms.contains(descriptor.name())) {
                                        throw new AssertionError(pipeline.getLocation()
                                                + " has no binding for " + descriptor.name());
                                    }
                                }
                                compiled++;
                            }
                        }
                    }
                }
            }
        } finally {
            deviceField.set(null, previousDevice);
        }
        return compiled;
    }

    private static RenderPipeline pipelineField(String owner, String name) throws Exception {
        Field field = Class.forName(owner).getDeclaredField(name);
        field.setAccessible(true);
        return (RenderPipeline) field.get(null);
    }

    private static final class ClasspathShaderSource implements ShaderSource {
        private final Map<Identifier, CachedIncludeSource> includes = new HashMap<>();

        @Override
        public String getShader(Identifier id, ShaderType type) {
            return read("assets/" + id.getNamespace() + "/shaders/" + id.getPath()
                    + (type == ShaderType.VERTEX ? ".vsh" : ".fsh"));
        }

        @Override
        public CachedIncludeSource getInclude(Identifier id) {
            return includes.computeIfAbsent(id, key -> CachedIncludeSource.create(key,
                    read("assets/" + key.getNamespace() + "/shaders/include/" + key.getPath())));
        }

        private String read(String path) {
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
                if (stream == null) throw new AssertionError("Missing shader resource: " + path);
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void close() {
            includes.values().forEach(CachedIncludeSource::close);
        }
    }
}
