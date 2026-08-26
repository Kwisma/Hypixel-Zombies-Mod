package com.example.client.utils;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/** Encodes keyboard keys and mouse buttons in the existing integer bind field. */
public final class InputBindingUtils {

    public static final int NONE = 0;
    private static final int MOUSE_CODE_BASE = -1000;

    private InputBindingUtils() {
    }

    /** Mouse 4 and above are treated as bindable side/extra buttons. */
    public static boolean isBindableMouseButton(int button) {
        return button >= GLFW.GLFW_MOUSE_BUTTON_4 && button <= GLFW.GLFW_MOUSE_BUTTON_LAST;
    }

    public static int encodeMouseButton(int button) {
        if (!isBindableMouseButton(button)) {
            throw new IllegalArgumentException("Not a bindable mouse button: " + button);
        }
        return MOUSE_CODE_BASE - button;
    }

    public static boolean isMouseBinding(int binding) {
        return binding <= MOUSE_CODE_BASE - GLFW.GLFW_MOUSE_BUTTON_4
                && binding >= MOUSE_CODE_BASE - GLFW.GLFW_MOUSE_BUTTON_LAST;
    }

    public static int decodeMouseButton(int binding) {
        if (!isMouseBinding(binding)) {
            throw new IllegalArgumentException("Not a mouse binding: " + binding);
        }
        return MOUSE_CODE_BASE - binding;
    }

    public static boolean matchesKeyboard(int binding, int key) {
        return binding != NONE && !isMouseBinding(binding) && binding == key;
    }

    public static boolean matchesMouse(int binding, int button) {
        return isMouseBinding(binding) && decodeMouseButton(binding) == button;
    }

    public static String displayName(int binding) {
        if (binding == NONE) {
            return "None";
        }
        if (isMouseBinding(binding)) {
            // GLFW numbers buttons from zero, while users conventionally call
            // the first two side buttons Mouse 4 and Mouse 5.
            return "Mouse " + (decodeMouseButton(binding) + 1);
        }
        return InputConstants.Type.KEYSYM.getOrCreate(binding).getDisplayName().getString();
    }
}
