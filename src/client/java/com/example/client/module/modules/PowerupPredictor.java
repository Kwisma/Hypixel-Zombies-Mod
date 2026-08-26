package com.example.client.module.modules;

import com.darkmagician6.eventapi.EventTarget;
import com.example.client.data.PowerupPredictor.Type;          // 直接导入嵌套枚举，避开与本模块同名的冲突
import com.example.client.events.SkiaEvent;
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
import com.example.client.tracker.ServerTracker;
import com.example.client.utils.PlayerUtils;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = {
        @Text(label = "AA Powerup Predictor", language = Language.English),
        @Text(label = "AA 道具预测", language = Language.Chinese)
}, enable = true)
public class PowerupPredictor extends AbstractModule {
    private static final float PANEL_WIDTH = 176F;
    private static final float PANEL_HEIGHT = 53F;
    private static final float PANEL_RADIUS = 6F;
    private static final float PADDING = 7F;
    private static final float ROW_HEIGHT = 11F;
    private static final float ROW_GAP = 1.5F;

    private static final int PANEL_COLOR = 0xA8181D23;
    private static final int PANEL_HIGHLIGHT = 0x60343C46;
    private static final int ROW_COLOR = 0x7A20262D;
    private static final int TEXT_COLOR = 0xFFF1F5F8;
    private static final int MUTED_TEXT_COLOR = 0xFF909DA9;


    @SettingInfo(name = {@Text(label = "X", language = Language.English)})
    public static final NumberSetting posX = new NumberSetting(0.01, 0, 1, "#.00");

    @SettingInfo(name = {@Text(label = "Y", language = Language.English)})
    public static final NumberSetting posY = new NumberSetting(0.45, 0, 1, "#.00");

    /** 每个道具往后列几个掉落回合 */
    @SettingInfo(name = {@Text(label = "Count", language = Language.English)})
    public static final NumberSetting count = new NumberSetting(3, 1, 8, "#");

    public PowerupPredictor() {
        registerSetting(posX, posY, count);
    }

    @EventTarget
    public void onRenderSkia(SkiaEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!PlayerUtils.isInHypZombies()) return;

        var pred = ServerTracker.powerup.getPredictor();
        int cur = ServerTracker.currentRound;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float x = Math.clamp(
                screenWidth * posX.getValue().floatValue(),
                2F,
                Math.max(2F, screenWidth - PANEL_WIDTH - 2F)
        );
        float y = Math.clamp(
                screenHeight * posY.getValue().floatValue(),
                2F,
                Math.max(2F, screenHeight - PANEL_HEIGHT - 2F)
        );
        int show = count.getValue().intValue();

        drawPanel(event.getCanvasStack(), x, y, pred, cur, show);
    }

    private void drawPanel(
            CanvasStack canvasStack,
            float x,
            float y,
            com.example.client.data.PowerupPredictor predictor,
            int currentRound,
            int showCount
    ) {
        SkiaFont titleFont = SkiaFonts.getBoldFont(6);
        SkiaFont rowFont = SkiaFonts.getDefaultFont(5);
        int lockedCount = 0;
        for (Type type : Type.values()) {
            if (predictor.isLocked(type)) lockedCount++;
        }

        RenderUtils.drawShadow(
                canvasStack, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS,
                0x60000000, 6F, 0F, 2F
        );
        RenderUtils.drawBlur(canvasStack, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS, 10F);
        RenderUtils.drawRect(canvasStack, x, y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS, PANEL_COLOR);
        RenderUtils.drawRect(canvasStack, x + 1F, y + 1F, PANEL_WIDTH - 2F, 1F, 1F, PANEL_HIGHLIGHT);

        RenderUtils.drawRect(canvasStack, x + PADDING, y + 5F, 2F, 7F, 1F, 0xFFB65CFF);
        titleFont.drawShadowString(canvasStack, "POWERUP FORECAST", x + 12F, y + 3.5F, TEXT_COLOR, true);

        String lockText = lockedCount + "/" + Type.values().length + " LOCKED";
        rowFont.drawShadowString(
                canvasStack,
                lockText,
                x + PANEL_WIDTH - PADDING - rowFont.getWidth(lockText),
                y + 5F,
                lockedCount == Type.values().length ? 0xFF64FF91 : MUTED_TEXT_COLOR,
                true
        );

        float rowY = y + 15F;
        for (Type type : Type.values()) {
            drawPowerupRow(canvasStack, rowFont, x + PADDING, rowY, type, predictor, currentRound, showCount);
            rowY += ROW_HEIGHT + ROW_GAP;
        }
    }

    private void drawPowerupRow(
            CanvasStack canvasStack,
            SkiaFont font,
            float x,
            float y,
            Type type,
            com.example.client.data.PowerupPredictor predictor,
            int currentRound,
            int showCount
    ) {
        float rowWidth = PANEL_WIDTH - PADDING * 2F;
        int typeColor = color(type);
        boolean locked = predictor.isLocked(type);

        RenderUtils.drawRect(canvasStack, x, y, rowWidth, ROW_HEIGHT, 3F, ROW_COLOR);
        RenderUtils.drawRect(canvasStack, x, y, 2F, ROW_HEIGHT, 1F, withAlpha(typeColor, locked ? 0xFF : 0x60));
        font.drawShadowString(
                canvasStack,
                label(type),
                x + 5F,
                y + 2F,
                locked ? typeColor : MUTED_TEXT_COLOR,
                true
        );

        float timelineX = x + 39F;
        float timelineWidth = rowWidth - 43F;
        float timelineY = y + 8.5F;
        RenderUtils.drawRect(canvasStack, timelineX, timelineY, timelineWidth, 0.75F, 0F, 0x504B5661);

        if (!locked) {
            font.drawShadowString(canvasStack, "SCANNING", timelineX, y + 2F, MUTED_TEXT_COLOR, true);
            for (int i = 0; i < 4; i++) {
                float dotX = timelineX + timelineWidth - 20F + i * 6F;
                RenderUtils.drawRect(canvasStack, dotX, timelineY - 1F, 2.5F, 2.5F, 1.25F, 0x805D6974);
            }
            return;
        }

        List<String> entries = predictionEntries(type, predictor, currentRound, showCount);
        if (entries.isEmpty()) entries = List.of("-");
        float spacing = entries.size() <= 1 ? 0F : timelineWidth / (entries.size() - 1F);

        for (int i = 0; i < entries.size(); i++) {
            String entry = entries.get(i);
            float nodeX = entries.size() == 1 ? timelineX + timelineWidth * 0.5F : timelineX + spacing * i;
            boolean now = "NOW".equals(entry);
            float labelX = Math.clamp(
                    nodeX - font.getWidth(entry) * 0.5F,
                    timelineX,
                    timelineX + timelineWidth - font.getWidth(entry)
            );

            RenderUtils.drawRect(
                    canvasStack,
                    nodeX - (now ? 1.8F : 1.2F),
                    timelineY - (now ? 1.8F : 1.2F),
                    now ? 3.6F : 2.4F,
                    now ? 3.6F : 2.4F,
                    now ? 1.8F : 1.2F,
                    now ? 0xFFFFC857 : typeColor
            );
            font.drawShadowString(
                    canvasStack,
                    entry,
                    labelX,
                    y + 1F,
                    now ? 0xFFFFD56A : TEXT_COLOR,
                    true
            );
        }
    }

    private static List<String> predictionEntries(
            Type type,
            com.example.client.data.PowerupPredictor predictor,
            int currentRound,
            int showCount
    ) {
        List<String> entries = new ArrayList<>();
        if (predictor.isPowerupRound(type, currentRound)) {
            entries.add("NOW");
        }

        int round = currentRound;
        for (int i = 0; i < showCount; i++) {
            int next = predictor.nextRound(type, round);
            if (next < 0) break;
            entries.add("R" + next);
            round = next;
        }
        return entries;
    }

    private static String label(Type t) {
        return switch (t) {
            case INSTA -> "INSTA";
            case MAX -> "MAX";
            case SS -> "SPREE";
        };
    }

    private static int color(Type t) {
        return switch (t) {
            case INSTA -> 0xFFFF5A67;
            case MAX -> 0xFF59A8FF;
            case SS -> 0xFFB65CFF;
        };
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.clamp(alpha, 0, 255) << 24);
    }
}
