package com.example.client.module.modules;

import com.darkmagician6.eventapi.EventTarget;
import com.example.client.events.EntityLoadEvent;
import com.example.client.events.PacketEvent;
import com.example.client.events.SkiaEvent;
import com.example.client.events.TickEvent;
import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import com.example.client.setting.annotation.SettingInfo;
import com.example.client.setting.settings.NumberSetting;
import com.example.client.skia.CanvasStack;
import com.example.client.skia.font.SkiaFont;
import com.example.client.skia.font.SkiaFonts;
import com.example.client.skia.render.RenderUtils;
import com.example.client.utils.PlayerUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.world.entity.EntityType;

import java.awt.*;
import java.util.Arrays;

@ModuleInfo(name = {
        @Text(label = "Lightning Rod Queue", language = Language.English),
        @Text(label = "LRod 队列", language = Language.Chinese)
}, enable = true)
public class LightningRodQueue extends AbstractModule {
    private static final int SLOT_COUNT = 4;
    private static final long COOLDOWN_MS = 20_000L;
    private static final long OUTSIDE_RESET_GRACE_MS = 3_000L;

    private static final float PANEL_WIDTH = 154F;
    private static final float PANEL_HEIGHT = 45F;
    private static final float PANEL_RADIUS = 7F;
    private static final float PANEL_PADDING = 7F;
    private static final float SLOT_WIDTH = 32F;
    private static final float SLOT_HEIGHT = 23F;
    private static final float SLOT_GAP = 4F;

    private static final int PANEL_COLOR = 0xB81A1E24;
    private static final int PANEL_HIGHLIGHT = 0x70343C46;
    private static final int SLOT_COLOR = 0xA8232931;
    private static final int SLOT_INNER_COLOR = 0xE0181D23;
    private static final int READY_COLOR = 0xFF64FF91;
    private static final int COOLDOWN_COLOR = 0xFF41A5FF;
    private static final int PRIMARY_TEXT_COLOR = 0xFFF3F7FA;
    private static final int MUTED_TEXT_COLOR = 0xFF9CA9B5;

    @SettingInfo(name = {
            @Text(label = "X", language = Language.English),
            @Text(label = "X", language = Language.Chinese)
    })
    public static final NumberSetting posX = new NumberSetting(0.50, 0, 1, "#.00");

    @SettingInfo(name = {
            @Text(label = "Y", language = Language.English),
            @Text(label = "Y", language = Language.Chinese)
    })
    public static final NumberSetting posY = new NumberSetting(0.10, 0, 1, "#.00");

    private final long[] cooldownEndMs = new long[SLOT_COUNT];
    private long lastZombiesSeenMs;

    public LightningRodQueue() {
        registerSetting(posX, posY);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundLoginPacket) {
            mc.execute(this::resetQueue);
            return;
        }

    }

    @EventTarget
    public void onEntityLoad(EntityLoadEvent event) {
        if (event.getEntity().getType() != EntityType.LIGHTNING_BOLT) return;

        // Fabric 的实体加载事件在客户端线程触发，单人和多人使用同一检测路径。
        recordLightningStrike();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        long now = System.currentTimeMillis();
        if (isTrackingEnvironment()) {
            lastZombiesSeenMs = now;
            return;
        }

        if (lastZombiesSeenMs != 0L && now - lastZombiesSeenMs >= OUTSIDE_RESET_GRACE_MS) {
            resetQueue();
        }
    }

    @EventTarget
    public void onRenderSkia(SkiaEvent event) {
        if (mc.player == null || mc.level == null || !isTrackingEnvironment()) return;

        long now = System.currentTimeMillis();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float x = Math.round(screenWidth * posX.getValue().doubleValue()) - PANEL_WIDTH * 0.5F;
        float y = Math.round(screenHeight * posY.getValue().doubleValue());

        drawPanel(event.getCanvasStack(), x, y, now);
    }

    private void recordLightningStrike() {
        if (mc.player == null || mc.level == null || !isTrackingEnvironment()) return;

        long now = System.currentTimeMillis();
        lastZombiesSeenMs = now;
        for (int slot = 0; slot < cooldownEndMs.length; slot++) {
            if (cooldownEndMs[slot] <= now) {
                cooldownEndMs[slot] = now + COOLDOWN_MS;
                return;
            }
        }
    }

    private void drawPanel(CanvasStack canvasStack, float x, float y, long now) {
        SkiaFont titleFont = SkiaFonts.getDefaultFont(8);
        SkiaFont slotFont = SkiaFonts.getDefaultFont(6);
        SkiaFont indexFont = SkiaFonts.getDefaultFont(6);

        int readyCount = 0;
        for (long cooldownEnd : cooldownEndMs) {
            if (cooldownEnd <= now) readyCount++;
        }

        RenderUtils.drawShadow(
                canvasStack, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS,
                Color.BLACK.getRGB()
        );
        RenderUtils.drawBlur(canvasStack, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS, 12F);
        RenderUtils.drawRect(canvasStack, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS, PANEL_COLOR);

        // 标题前的状态色强调线。
        RenderUtils.drawRect(canvasStack, x + PANEL_PADDING, y + 5F, 2F, 7F, 1F, READY_COLOR);
        titleFont.drawShadowString(
                canvasStack, "LR QUEUE", x + PANEL_PADDING + 5F, y + 3.5F,
                PRIMARY_TEXT_COLOR, true
        );

        String readyText = readyCount + "/" + SLOT_COUNT + " READY";
        titleFont.drawShadowString(
                canvasStack,
                readyText,
                x + PANEL_WIDTH - PANEL_PADDING - titleFont.getWidth(readyText),
                y + 3.5F,
                readyCount == 0 ? COOLDOWN_COLOR : READY_COLOR,
                true
        );

        float slotY = y + 16F;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            float slotX = x + PANEL_PADDING + slot * (SLOT_WIDTH + SLOT_GAP);
            drawSlot(canvasStack, slotFont, indexFont, slotX, slotY, slot, now);
        }
    }

    private void drawSlot(
            CanvasStack canvasStack,
            SkiaFont slotFont,
            SkiaFont indexFont,
            float x,
            float y,
            int slot,
            long now
    ) {
        long remainingMs = Math.max(0L, cooldownEndMs[slot] - now);
        boolean coolingDown = remainingMs > 0L;
        int stateColor = coolingDown ? COOLDOWN_COLOR : READY_COLOR;

        String status;
        float progress;
        if (coolingDown) {
            status = (remainingMs + 999L) / 1_000L + "s";
            progress = Math.clamp(remainingMs / (float) COOLDOWN_MS, 0F, 1F);
        } else {
            status = "READY";
            progress = 1F;
        }

        // 两层圆角矩形形成细描边，不依赖额外的 stroke 工具。
        RenderUtils.drawRect(canvasStack, x, y, SLOT_WIDTH, SLOT_HEIGHT, 4F, stateColor);
        RenderUtils.drawRect(
                canvasStack, x + 0.75F, y + 0.75F,
                SLOT_WIDTH - 1.5F, SLOT_HEIGHT - 1.5F, 3.5F, SLOT_INNER_COLOR
        );

        String index = "#" + (slot + 1);
        indexFont.drawShadowString(canvasStack, index, x + 3F, y + 1.5F, MUTED_TEXT_COLOR, true);

        // 右上角的小状态灯可以在不读文字时快速判断槽位状态。
        RenderUtils.drawRect(canvasStack, x + SLOT_WIDTH - 6F, y + 3F, 3F, 3F, 1.5F, stateColor);

        slotFont.drawShadowString(
                canvasStack,
                status,
                x + (SLOT_WIDTH - slotFont.getWidth(status)) * 0.5F,
                y + 9F,
                coolingDown ? PRIMARY_TEXT_COLOR : READY_COLOR,
                true
        );

        RenderUtils.drawRect(
                canvasStack, x + 3F, y + SLOT_HEIGHT - 4F,
                SLOT_WIDTH - 6F, 2F, 1F, SLOT_COLOR
        );
        RenderUtils.drawRect(
                canvasStack, x + 3F, y + SLOT_HEIGHT - 4F,
                (SLOT_WIDTH - 6F) * progress, 2F, 1F, stateColor
        );
    }

    private void resetQueue() {
        Arrays.fill(cooldownEndMs, 0L);
        lastZombiesSeenMs = 0L;
    }

    /** 正式环境仅 Zombies；集成服务器用于单人指令测试闪电检测与 UI。 */
    private boolean isTrackingEnvironment() {
        return PlayerUtils.isInHypZombies() || mc.hasSingleplayerServer();
    }

    @Override
    protected void onDisable() {
        resetQueue();
    }
}
