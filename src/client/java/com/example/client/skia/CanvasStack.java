package com.example.client.skia;

import io.github.humbleui.skija.Canvas;

public record CanvasStack(Canvas canvas) {

    public void push() {
        canvas.save();
    }

    public void pop() {
        canvas.restore();
    }

    public void translate(float x, float y) {
        canvas.translate(x, y);
    }

    public void rotate(float radians) {
        canvas.rotate((float) Math.toDegrees(radians));
    }

    public void scale(float s) {
        canvas.scale(s, s);
    }

    public void scale(float sx, float sy) {
        canvas.scale(sx, sy);
    }
}
