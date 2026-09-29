package com.example.test;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.joml.Vector4fc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

/** Uses the real render-pass validation with a stub GPU backend; no window is needed. */
final class RenderPassCompatibilityCheck {
    private RenderPassCompatibilityCheck() {
    }

    static void run(List<RenderPipeline> pipelines, DeviceInfo info) {
        GpuDeviceBackend device = stub(GpuDeviceBackend.class, (proxy, method, arguments) -> {
            if (method.getName().equals("getDeviceInfo")) return info;
            throw new AssertionError("Unexpected device call: " + method.getName());
        });
        GpuTexture texture = stub(GpuTexture.class, (proxy, method, arguments) -> {
            if (method.getName().equals("getFormat")) return GpuFormat.RGBA8_UNORM;
            throw new AssertionError("Unexpected texture call: " + method.getName());
        });
        GpuTextureView view = stub(GpuTextureView.class, (proxy, method, arguments) -> {
            if (method.getName().equals("texture")) return texture;
            throw new AssertionError("Unexpected texture-view call: " + method.getName());
        });
        BackendRenderPipeline compiledBackend = stub(BackendRenderPipeline.class, (proxy, method, arguments) -> {
            if (method.getName().equals("isClosed")) return false;
            if (method.getName().equals("close")) return null;
            throw new AssertionError("Unexpected pipeline call: " + method.getName());
        });
        // World/entity rendering and the GUI passes all use one RGBA8 color attachment.
        var attachment = new RenderPassDescriptor.Attachment<Optional<Vector4fc>>(view, Optional.empty());
        AssertionError failures = new AssertionError("Custom pipelines must bind to their render pass");
        for (RenderPipeline pipeline : pipelines) {
            boolean[] bound = {false};
            RenderPassBackend backend = stub(RenderPassBackend.class, (proxy, method, arguments) -> {
                if (method.getName().equals("setPipeline")) {
                    if (arguments[0] != compiledBackend) throw new AssertionError("Wrong backend pipeline");
                    bound[0] = true;
                    return null;
                }
                if (method.getName().equals("close")) return null;
                throw new AssertionError("Unexpected render-pass call: " + method.getName());
            });
            var uniforms = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
            var indices = new Object2IntOpenHashMap<String>();
            for (int i = 0; i < uniforms.size(); i++) indices.put(uniforms.get(i).name(), i);
            var compiled = new FrontendRenderPipeline(pipeline.getLocation().toString(), compiledBackend,
                    pipeline.getVertexFormatBindings(), indices, uniforms, pipeline.getColorTargetStates(),
                    pipeline.wantsDepthTexture(), pipeline.pushConstantSize());
            try (var pass = new FrontendRenderPass(backend, device, List.of(attachment),
                    pipeline.wantsDepthTexture(), () -> {}, new RenderPass.RenderArea(0, 0, 16, 16))) {
                pass.setPipeline(compiled);
                if (!bound[0]) throw new AssertionError("Pipeline must reach the backend after validation");
            } catch (IllegalStateException failure) {
                failures.addSuppressed(new AssertionError(pipeline.getLocation().toString(), failure));
            }
        }
        if (failures.getSuppressed().length != 0) throw failures;
        System.out.println("RENDER PASS CHECK PASSED: " + pipelines.size() + " pipeline bindings");
    }

    private static <T> T stub(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
