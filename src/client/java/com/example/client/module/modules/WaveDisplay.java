package com.example.client.module.modules;

import com.darkmagician6.eventapi.EventTarget;
import com.example.client.data.ZombiesWaves;
import com.example.client.events.RenderEvent;
import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import com.example.client.setting.annotation.SettingInfo;
import com.example.client.setting.settings.BooleanSetting;
import com.example.client.setting.settings.NumberSetting;
import com.example.client.tracker.ServerTracker;
import com.example.client.utils.PlayerUtils;
import com.example.client.utils.ZombiesMap;
import com.example.client.utils.ZombiesUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * 波数显示：右下角列出当前回合各波次，用箭头指示当前波、变暗已过波。
 * 回合号/回合起始时间来自 {@link ServerTracker}；波次时间表见 {@link ZombiesWaves}。
 */
@ModuleInfo(name = {
        @Text(label = "Wave Display", language = Language.English),
        @Text(label = "波数显示", language = Language.Chinese)
}, enable = false)
public class WaveDisplay extends AbstractModule {

    @SettingInfo(name = {
            @Text(label = "Only In Zombies", language = Language.English),
            @Text(label = "仅在僵尸末日里", language = Language.Chinese)
    })
    public static final BooleanSetting onlyGame = new BooleanSetting(true);

    @SettingInfo(name = {
            @Text(label = "X", language = Language.English),
            @Text(label = "X", language = Language.Chinese)
    })
    public static final NumberSetting posX = new NumberSetting(0.82, 0, 1, "#.00");

    @SettingInfo(name = {
            @Text(label = "Y", language = Language.English),
            @Text(label = "Y", language = Language.Chinese)
    })
    public static final NumberSetting posY = new NumberSetting(0.60, 0, 1, "#.00");

    public WaveDisplay() {
        registerSetting(onlyGame, posX, posY);
    }

    @EventTarget
    public void onRender(RenderEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (onlyGame.getValue() && !PlayerUtils.isInHypZombies()) return;

        int round = ServerTracker.currentRound;
        if (round < 0) return;

        ZombiesMap map = ZombiesUtils.getMap();
        if (map == null || map == ZombiesMap.NULL) return;

        int[] waves = ZombiesWaves.getWaves(map, round);
        if (waves == null || waves.length == 0) return;

        boolean boss = ZombiesWaves.isBossRound(map, round);

        double elapsed = (System.currentTimeMillis() - ServerTracker.roundTime) / 1000.0;
        if (elapsed < 0) elapsed = 0;

        int current = ZombiesWaves.currentWaveIndex(waves, elapsed);

        GuiGraphicsExtractor graphics = event.getGuiGraphicsExtractor();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int x = (int) (sw * posX.getValue().doubleValue());
        int y = (int) (sh * posY.getValue().doubleValue());
        int lineHeight = 11;

        // 标题：回合 + 地图 + (boss 标记) + 距下一波倒计时
        double toNext = ZombiesWaves.secondsToNextWave(waves, elapsed);
        String header = "Round " + round + " [" + mapName(map) + "]"
                + (boss ? " BOSS" : "")
                + (toNext >= 0 ? "  (next " + String.format("%.1fs", toNext) + ")" : "");
        graphics.text(mc.font, header, x, y, boss ? 0xFFFF5555 : 0xFFFFFFFF, true);
        y += lineHeight + 2;

        for (int i = 0; i < waves.length; i++) {
            String label = "Wave " + (i + 1) + "  " + formatClock(waves[i]);

            // AA 逐波 boss：巨人/Old One/两者，分别用不同颜色（非 AA 始终 NONE）
            ZombiesWaves.WaveBoss wb = ZombiesWaves.aaWaveBoss(map, round, i + 1);

            int color = i < current
                    ? 0xFF555555                 // 已过：暗灰
                    : bossFutureColor(wb);       // 当前/未到：boss 色，否则白

            if (i == current) {
                String arrow = "→";
                graphics.text(mc.font, arrow, x - mc.font.width(arrow) - 2, y, 0xFFFFFFFF, true);
            }

            graphics.text(mc.font, label, x, y, color, true);
            y += lineHeight;
        }
    }

    /** 当前及还没到的波的颜色：巨人=紫, Old One=暗青, 两者=暗棕, 普通=白。 */
    private static int bossFutureColor(ZombiesWaves.WaveBoss wb) {
        return switch (wb) {
            case GIANT -> 0xFF663399;
            case OLD_ONE -> 0xFF006666;
            case BOTH -> 0xFF783300;
            case NONE -> 0xFFFFFFFF;
        };
    }

    private static String mapName(ZombiesMap map) {
        return switch (map) {
            case DEAD_END -> "Dead End";
            case BAD_BLOOD -> "Bad Blood";
            case ALIEN_ARCADIUM -> "Alien Arcadium";
            case THE_LAB -> "The Lab";
            case PRISON -> "Prison";
            case NULL -> "?";
        };
    }

    /** 秒 → m:ss（不足一分钟则 0:ss）。 */
    private static String formatClock(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return m + ":" + (s < 10 ? "0" + s : s);
    }
}
