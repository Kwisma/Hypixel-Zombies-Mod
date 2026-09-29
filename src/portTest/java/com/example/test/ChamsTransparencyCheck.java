package com.example.test;

import com.example.client.chams.ChamsRenderType;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL14;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLVideo.*;

/** Optional GPU check of the production depth-seed state; uses an offscreen framebuffer. */
final class ChamsTransparencyCheck {
    private static final float[] WALL_COLOR = {0.15F, 0.35F, 0.65F, 1.0F};
    private static final float[] ARMOR_COLOR = {0.9F, 0.1F, 0.2F};

    private ChamsTransparencyCheck() {
    }

    static void run() throws ReflectiveOperationException {
        Field field = ChamsRenderType.class.getDeclaredField("DEPTH_SEED_PIPELINE");
        field.setAccessible(true);
        RenderPipeline seed = (RenderPipeline) field.get(null);
        require(SDL_Init(SDL_INIT_VIDEO), "SDL video initialization");
        long window = 0;
        long context = 0;
        int framebuffer = 0;
        int colorBuffer = 0;
        int depthBuffer = 0;
        try {
            SDL_GL_ResetAttributes();
            require(SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3), "OpenGL major version");
            require(SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 3), "OpenGL minor version");
            require(SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_COMPATIBILITY),
                    "OpenGL compatibility profile");
            window = SDL_CreateWindow("Chams transparency check", 16, 16, SDL_WINDOW_OPENGL | SDL_WINDOW_HIDDEN);
            require(window != 0, "Hidden window creation");
            context = SDL_GL_CreateContext(window);
            require(context != 0, "OpenGL context creation");
            require(SDL_GL_MakeCurrent(window, context), "Making OpenGL current");
            GL.createCapabilities();

            framebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            colorBuffer = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, colorBuffer);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA8, 16, 16);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorBuffer);
            depthBuffer = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, 16, 16);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new AssertionError("Incomplete offscreen framebuffer");
            }
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            glReadBuffer(GL_COLOR_ATTACHMENT0);
            glViewport(0, 0, 16, 16);
            glDisable(GL_DITHER);
            glDisable(GL_CULL_FACE);
            glEnable(GL_DEPTH_TEST);

            int cases = 0;
            // Reversed Z: a rear face crosses the wall at depth 0.5, while the front stays visible.
            for (float[] depths : new float[][]{{0.6F, 0.7F}, {0.49F, 0.51F}, {0.3F, 0.4F}}) {
                for (float alpha : new float[]{0.0F, 32.0F / 255.0F, 0.5F, 1.0F}) {
                    checkPixel(seed, depths[0], depths[1], alpha);
                    cases++;
                }
            }
            if (glGetError() != GL_NO_ERROR) throw new AssertionError("OpenGL error during pixel checks");
            System.out.println("CHAMS PIXEL CHECK PASSED: " + cases + " wall/alpha cases on " + glGetString(GL_RENDERER));
        } finally {
            if (framebuffer != 0) glDeleteFramebuffers(framebuffer);
            if (colorBuffer != 0) glDeleteRenderbuffers(colorBuffer);
            if (depthBuffer != 0) glDeleteRenderbuffers(depthBuffer);
            if (context != 0) {
                GL.setCapabilities(null);
                SDL_GL_DestroyContext(context);
            }
            if (window != 0) SDL_DestroyWindow(window);
            SDL_QuitSubSystem(SDL_INIT_VIDEO);
        }
    }

    private static void checkPixel(RenderPipeline seed, float rearDepth, float frontDepth, float alpha) {
        glColorMask(true, true, true, true);
        glDepthMask(true);
        glClearColor(WALL_COLOR[0], WALL_COLOR[1], WALL_COLOR[2], WALL_COLOR[3]);
        glClearDepth(0.5);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Apply the actual production seed's color/depth state, then draw the occluded armor face.
        var target = seed.getColorTargetStates().getFirst();
        if (target.blendFunction().isPresent()) throw new AssertionError("Unexpected seed blend function");
        glDisable(GL_BLEND);
        glColorMask(target.writeRed(), target.writeGreen(), target.writeBlue(), target.writeAlpha());
        var depth = seed.getDepthStencilState();
        glDepthMask(depth.writeDepth());
        glDepthFunc(switch (depth.depthTest()) {
            case ALWAYS_PASS -> GL_ALWAYS;
            case NEVER_PASS -> GL_NEVER;
            case LESS_THAN -> GL_LESS;
            case LESS_THAN_OR_EQUAL -> GL_LEQUAL;
            case GREATER_THAN -> GL_GREATER;
            case GREATER_THAN_OR_EQUAL -> GL_GEQUAL;
            case EQUAL -> GL_EQUAL;
            case NOT_EQUAL -> GL_NOTEQUAL;
        });
        drawArmor(rearDepth, alpha);
        float[] seededColor = readColor();
        for (int channel = 0; channel < 4; channel++) {
            near(seededColor[channel], WALL_COLOR[channel], "Depth seed painted over wall channel " + channel);
        }
        FloatBuffer seededDepth = BufferUtils.createFloatBuffer(1);
        glReadPixels(8, 8, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, seededDepth);
        near(seededDepth.get(0), Math.min(0.5F, rearDepth), "Depth seed must still expose occluded geometry");

        // The subsequent visible face must blend over the wall once, even when the back intersects it.
        glColorMask(true, true, true, true);
        glDepthMask(false);
        glDepthFunc(GL_GEQUAL);
        glEnable(GL_BLEND);
        GL14.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        drawArmor(frontDepth, alpha);
        float[] color = readColor();
        for (int channel = 0; channel < 3; channel++) {
            near(color[channel], ARMOR_COLOR[channel] * alpha + WALL_COLOR[channel] * (1.0F - alpha),
                    "Incorrect armor blend at alpha " + alpha + ", rear depth " + rearDepth);
        }
    }

    private static void drawArmor(float depth, float alpha) {
        glColor4f(ARMOR_COLOR[0], ARMOR_COLOR[1], ARMOR_COLOR[2], alpha);
        float z = depth * 2.0F - 1.0F;
        glBegin(GL_QUADS);
        glVertex3f(-1, -1, z);
        glVertex3f(1, -1, z);
        glVertex3f(1, 1, z);
        glVertex3f(-1, 1, z);
        glEnd();
    }

    private static float[] readColor() {
        FloatBuffer color = BufferUtils.createFloatBuffer(4);
        glReadPixels(8, 8, 1, 1, GL_RGBA, GL_FLOAT, color);
        return new float[]{color.get(0), color.get(1), color.get(2), color.get(3)};
    }

    private static void near(float actual, float expected, String message) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > 2.0F / 255.0F) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void require(boolean success, String operation) {
        if (!success) throw new AssertionError(operation + ": " + SDL_GetError());
    }
}
