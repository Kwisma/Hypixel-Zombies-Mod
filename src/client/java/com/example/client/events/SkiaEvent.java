package com.example.client.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import com.example.client.skia.CanvasStack;
import com.example.client.skia.fbo.GameFramebuffer;

public class SkiaEvent extends EventCancellable {
    private final CanvasStack canvasStack;
    private final GameFramebuffer customLayer;

    public SkiaEvent(CanvasStack canvasStack, GameFramebuffer customLayer) {
        this.canvasStack = canvasStack;
        this.customLayer = customLayer;
    }

    public CanvasStack getCanvasStack() {
        return canvasStack;
    }

    public GameFramebuffer getCustomLayer() {
        return customLayer;
    }
}
