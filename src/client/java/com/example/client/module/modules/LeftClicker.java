package com.example.client.module.modules;

import com.darkmagician6.eventapi.EventTarget;
import com.example.client.events.RenderEvent;
import com.example.client.mixin.MinecraftAccessor;
import com.example.client.mixin.MouseHandlerInvoker;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import com.example.client.setting.annotation.SettingInfo;
import com.example.client.setting.settings.ModeSetting;
import com.example.client.setting.settings.NumberSetting;
import com.example.client.utils.TimeUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.tags.ItemTags;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

@ModuleInfo(name = "module.left_clicker", enable = true)
public class LeftClicker extends AbstractModule {
    private static final long HOLD_DELAY_NANOS = 200_000_000L;

    @SettingInfo(name = "setting.click_mode")
    private final ModeSetting mode = new ModeSetting("Simulate", Arrays.asList("Simulate", "Key"));
    @SettingInfo(name = "setting.max_cps")
    private final NumberSetting maxCPS = new NumberSetting(12, 1.0, 20.0, "#");
    @SettingInfo(name = "setting.min_cps")
    private final NumberSetting minCPS = new NumberSetting(11, 1.0, 20.0, "#");
    @SettingInfo(name = "setting.only_guns")
    private final ModeSetting clickerType = new ModeSetting("Any", Arrays.asList("Any", "Sword"));

    private long holdStartNanos;
    private long nextClickNanos;

    public LeftClicker() {
        registerSetting(mode, maxCPS, minCPS, clickerType);
    }

    @EventTarget
    public void onClick(RenderEvent event) {
        if (!mc.options.keyAttack.isDown() || mc.player == null || mc.level == null || mc.gui.screen() != null) {
            resetClickState();
            return;
        }

        long now = System.nanoTime();
        if (holdStartNanos == 0) {
            holdStartNanos = now;
            nextClickNanos = now + HOLD_DELAY_NANOS;
            return;
        }
        if (now - holdStartNanos < HOLD_DELAY_NANOS) return;

        if (clickerType.is("Sword") && !mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
            nextClickNanos = 0;
            return;
        }

        if (nextClickNanos == 0) nextClickNanos = now;
        if (now >= nextClickNanos) {
            ((MinecraftAccessor) mc).zombiesmod$setMissTime(0);
            if (mode.is("Simulate")) {
                MouseHandlerInvoker mouseHandler = (MouseHandlerInvoker) (Object) mc.mouseHandler;
                MouseButtonInfo button = new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0);
                mouseHandler.zombiesmod$onButton(
                        mc.getWindow().handle(),
                        button,
                        GLFW.GLFW_RELEASE);
                mouseHandler.zombiesmod$onButton(
                        mc.getWindow().handle(),
                        button,
                        GLFW.GLFW_PRESS);
            } else {
                var attackKey = mc.options.keyAttack.getDefaultKey();
                KeyMapping.set(attackKey, false);
                KeyMapping.set(attackKey, true);
                KeyMapping.click(attackKey);
            }
            long interval = TimeUtils.randomClickDelayNanos(minCPS.getValue().intValue(), maxCPS.getValue().intValue());
            nextClickNanos += interval;
            if (nextClickNanos <= now) nextClickNanos = now + interval;
        }
    }

    @Override
    protected void onDisable() {
        resetClickState();
    }

    private void resetClickState() {
        holdStartNanos = 0;
        nextClickNanos = 0;
    }
}
