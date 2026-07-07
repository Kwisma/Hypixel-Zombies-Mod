package com.example.client.utils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

import java.awt.Color;

/** 为一次 GUI 文字提取附加纵向顶点渐变。 */
public final class GradientTextRenderer {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private GradientTextRenderer() { }

    public record Context(Color top, Color bottom, float topY, float bottomY) { }

    public static Scope push(Color top, Color bottom, float topY, float bottomY) {
        return push(new Context(top, bottom, topY, bottomY));
    }

    public static Scope push(Context context) {
        Context previous = CURRENT.get();
        CURRENT.set(context);
        return new Scope(previous);
    }

    public static Context currentContext() {
        return CURRENT.get();
    }

    public static VertexConsumer wrap(VertexConsumer delegate, Context context) {
        return new GradientVertexConsumer(delegate, context);
    }

    public static final class Scope implements AutoCloseable {
        private final Context previous;
        private boolean closed;

        private Scope(Context previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private static final class GradientVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final Context context;
        private float currentY;

        private GradientVertexConsumer(VertexConsumer delegate, Context context) {
            this.delegate = delegate;
            this.context = context;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            currentY = y;
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            applyGradient(alpha);
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            applyGradient(color >>> 24);
            return this;
        }

        private void applyGradient(int alpha) {
            float height = context.bottomY() - context.topY();
            float progress = height <= 0F ? 0F : (currentY - context.topY()) / height;
            progress = Math.clamp(progress, 0F, 1F);

            Color top = context.top();
            Color bottom = context.bottom();
            int red = Math.round(top.getRed() + (bottom.getRed() - top.getRed()) * progress);
            int green = Math.round(top.getGreen() + (bottom.getGreen() - top.getGreen()) * progress);
            int blue = Math.round(top.getBlue() + (bottom.getBlue() - top.getBlue()) * progress);
            delegate.setColor(red, green, blue, Math.clamp(alpha, 0, 255));
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            delegate.setLineWidth(width);
            return this;
        }
    }
}
