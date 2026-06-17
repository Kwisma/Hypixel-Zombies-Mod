package com.example.client.module.modules;

import com.darkmagician6.eventapi.EventTarget;
import com.example.client.events.RenderEvent;
import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import com.example.client.setting.annotation.SettingInfo;
import com.example.client.setting.settings.BooleanSetting;
import com.example.client.setting.settings.NumberSetting;
import com.example.client.tracker.TeammateInfo;
import com.example.client.tracker.TeammateTracker;
import com.example.client.utils.PlayerUtils;
import com.example.client.utils.render.GuiGraphicsUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @see com.example.client.mixin.MinecraftMixin
 */

@ModuleInfo(name = {
        @Text(label = "Teammates Glow", language = Language.English),
        @Text(label = "队友高亮显示", language = Language.Chinese)
}, enable = true)
public class TeammatesGlow extends AbstractModule {
    @SettingInfo(name = {
            @Text(label = "Only In Zombies", language = Language.English),
            @Text(label = "仅在僵尸末日里", language = Language.Chinese)
    })
    public static final BooleanSetting onlyGame = new BooleanSetting(true);
    @SettingInfo(name = {
            @Text(label = "Info", language = Language.English),
            @Text(label = "队友信息", language = Language.Chinese)
    })
    public static final BooleanSetting info = new BooleanSetting(true);
    @SettingInfo(name = {
            @Text(label = "X", language = Language.English),
            @Text(label = "X", language = Language.Chinese)
    })
    public static final NumberSetting posX = new NumberSetting(0.1, 0, 1, "#.00");
    @SettingInfo(name = {
            @Text(label = "Y", language = Language.English),
            @Text(label = "Y", language = Language.Chinese)
    })
    public static final NumberSetting posY = new NumberSetting(0.1, 0, 1, "#.00");

    public TeammatesGlow() {
        registerSetting(onlyGame, info, posX, posY);
    }
    @EventTarget
    public void onRender(RenderEvent event) {
        if(mc.player == null || mc.level == null) return;
        if(onlyGame.getValue() && !PlayerUtils.isInHypZombies()) return;
        if(!info.getValue()) return;

        GuiGraphicsExtractor graphics = event.getGuiGraphicsExtractor();
        int maxNameWidth = 0;

        for (TeammateInfo ti : TeammateInfo.teammates) {
            String line = ti.getName() + " " + formatGold(ti.getGold()) + " (Blocking)";
            int nameWidth = mc.font.width(line);

            if (nameWidth > maxNameWidth) {
                maxNameWidth = nameWidth;
            }
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        double xPercent = posX.getValue().doubleValue();
        double yPercent = posY.getValue().doubleValue();

        int x = (int) (screenWidth * xPercent);
        int y = (int) (screenHeight * yPercent);

        int height = 28;

        for (TeammateInfo ti : TeammateInfo.teammates) {
            Player player = ti.getRenderEntity();

            boolean blocking = player != null && PlayerUtils.isPlayerBlockingHyp(player);
            boolean down = ti.isDown();

            String name = ti.getName()
                    + ChatFormatting.GOLD + " " + formatGold(ti.getGold())
                    + ChatFormatting.YELLOW + (blocking ? " (Blocking)" : "");

            int hpReserve = mc.font.width("9999/9999");
            int boxWidth = maxNameWidth + 32 + hpReserve;
            GuiGraphicsUtils.drawBackground(graphics, x, y, boxWidth, height);

            if (player != null) {
                GuiGraphicsUtils.drawPlayerHead(graphics, player, x + 4, y + 4, 20);
                if (player.hurtTime != 0) {
                    graphics.fill(x + 4, y + 4, x + 20, y + 20, new Color(255,0,0,150).getRGB());

                }
                if (!down && player.isShiftKeyDown()) {
                    int bx = x + 4 + 20 - 9;   // 头像右下角
                    int by = y + 4 + 20 - 9;
                    graphics.fill(bx - 1, by - 1, bx + 10, by + 10, 0xFF0A0A0A); // 深色描边
                    graphics.fill(bx,     by,     bx + 9,  by + 9,  0xFF1D9E75); // 青色底
                    graphics.fill(bx + 1, by + 2, bx + 8,  by + 3,  0xFFFFFFFF); // ▼ 顶
                    graphics.fill(bx + 2, by + 3, bx + 7,  by + 4,  0xFFFFFFFF);
                    graphics.fill(bx + 3, by + 4, bx + 6,  by + 5,  0xFFFFFFFF);
                    graphics.fill(bx + 4, by + 5, bx + 5,  by + 6,  0xFFFFFFFF); // 尖
                }
            }

            graphics.text(mc.font, name, x + 28, y + 4, 0xFFFFFFFF, true);

            if (player != null) {

                float health = Math.max(0.0F, player.getHealth());
                float maxHealth = Math.max(1.0F, player.getMaxHealth());
                float percent = Math.max(0.0F, Math.min(1.0F, health / maxHealth));

                // 当前/最大生命值，右对齐在名字行；按血量比例上色
                String hp = (int) Math.ceil(health) + "/" + (int) Math.ceil(maxHealth);
                int hpColor = percent > 0.5f ? 0xFF66FF66 : (percent > 0.25f ? 0xFFFFD633 : 0xFFFF5555);
                graphics.text(mc.font, hp, x + boxWidth - mc.font.width(hp) - 6, y + 4, hpColor, true);

                GuiGraphicsUtils.drawHealthBar(graphics, x + 27, y + 14, boxWidth - 30, 4, percent);

                int armor = player.getArmorValue();
                float armorPercent = Math.max(0.0F, Math.min(1.0F, armor / 20.0F));
                GuiGraphicsUtils.drawArmorBar(graphics, x + 27, y + 14 + 6, boxWidth - 30, 4, armorPercent);
            }

            //down
            if(down) {
                String str = "REVIVE";
                int strW = mc.font.width(str);
                graphics.fill(x, y, x + boxWidth, y + height, 0xAA111111);
                graphics.text(mc.font, "REVIVE", (int) (x + boxWidth / 2f - (strW / 2f)), y + 10, Color.GREEN.getRGB(), true);

            }

            y += height;
        }

    }

    private static String formatGold(long gold) {
        return String.format("%,d", gold);
    }


}
