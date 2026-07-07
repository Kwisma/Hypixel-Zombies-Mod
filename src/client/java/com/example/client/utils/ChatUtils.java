package com.example.client.utils;

import com.example.client.module.modules.DPSCounter;
import net.minecraft.network.chat.Component;

public class ChatUtils implements IMinecraft {
    public static void print(String text) {
        if (mc.player == null) {
            return;
        }
        mc.gui.chatListener().handleSystemMessage(Component.literal(text), false);
    }
    public static void print(Component text) {
        if (mc.player == null) {
            return;
        }
        mc.gui.chatListener().handleSystemMessage(text, false);
    }
}
