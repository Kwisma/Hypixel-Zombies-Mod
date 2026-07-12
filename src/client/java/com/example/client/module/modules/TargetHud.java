package com.example.client.module.modules;

import com.darkmagician6.eventapi.EventTarget;
import com.example.client.events.RenderEvent;
import com.example.client.events.TickEvent;
import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import com.example.client.setting.annotation.SettingInfo;
import com.example.client.setting.settings.NumberSetting;
import com.example.client.tracker.ServerTracker;
import com.example.client.utils.PlayerUtils;
import com.example.client.utils.render.BlurRenderer;
import com.example.client.utils.render.GuiGraphicsUtils;
import com.example.client.utils.render.WorldToScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

@ModuleInfo(name = {
        @Text(label = "Target Hud", language = Language.English),
        @Text(label = "目标Hud显示", language = Language.Chinese)
}, enable = true)
public class TargetHud extends AbstractModule {
    private static final Color NORMAL_BACKGROUND = new Color(17, 17, 17, 170);
    private static final Color NORMAL_BORDER = new Color(68, 68, 68, 255);
    private static final Color BAD_HEADSHOT_BACKGROUND = new Color(120, 16, 16, 190);
    private static final Color BAD_HEADSHOT_BORDER = new Color(255, 85, 85, 255);

    @SettingInfo(name = {
            @Text(label = "Distance", language = Language.English),
            @Text(label = "距离", language = Language.Chinese)
    })
    private final NumberSetting distance = new NumberSetting(35, 1, 50, "#");

    public TargetHud() {
        registerSetting(distance);
    }
    private int raycastTimer = 0;
    private LivingEntity target = null;
    @EventTarget
    public void onTick(TickEvent event) {
        if (++raycastTimer < 3) {
            return;
        }
        raycastTimer = 0;
//        System.out.println(ServerTracker.serverPlayer.xRot() + " " + ServerTracker.serverPlayer.yRot());
        target = PlayerUtils.raycastTarget(ServerTracker.serverPlayer, distance.getValue().doubleValue(), TargetHud::isValidTarget);
    }

    @EventTarget
    public void onRender(RenderEvent event) {
        if (target == null)
            return;

        float partialTicks = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        WorldToScreen.ScreenPos pos = WorldToScreen.projectEntity(target, partialTicks);
        if (pos == null)
            return;

        int width = 150;
        int height = 45;
        int x = (int) pos.x() - width / 2;
        int y = (int) pos.y() - height;


        String name = target.getName().getString();
        float health = Math.max(0.0F, target.getHealth());
        float maxHealth = Math.max(1.0F, target.getMaxHealth());
        float percent = Math.max(0.0F, Math.min(1.0F, health / maxHealth));

        int armor = target.getArmorValue();
        float armorPercent = Math.max(0.0F, Math.min(1.0F, armor / 20.0F));

        double armorToughness = 0.0D;

        try {
            armorToughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        } catch (Exception ignored) {
        }

        double distance = mc.player.distanceTo(target);
        boolean badHeadshot = BadHeadshot.isBadHeadshotEntity(target);
        BlurRenderer.draw(event.getGuiGraphicsExtractor(),  x, y, width, height,10);
        drawBackground(event.getGuiGraphicsExtractor(), x, y, width, height, badHeadshot);
        drawText(event.getGuiGraphicsExtractor(), x, y, name, health, maxHealth, armor, armorToughness, distance,
                badHeadshot);

        GuiGraphicsUtils.drawHealthBar(event.getGuiGraphicsExtractor(), x + 8, y + 32, width - 16, 8, percent);
//        GuiGraphicsUtils.drawArmorBar(event.getGuiGraphicsExtractor(), x + 8, y + 50, width - 16, 8, armorPercent);
    }

    public static void drawBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean badHeadshot) {
        Color background = badHeadshot ? BAD_HEADSHOT_BACKGROUND : NORMAL_BACKGROUND;
        Color border = badHeadshot ? BAD_HEADSHOT_BORDER : NORMAL_BORDER;

        graphics.fill(x, y, x + width, y + height, background.getRGB());


        graphics.fill(x, y, x + width, y + 1, border.getRGB());
        graphics.fill(x, y + height - 1, x + width, y + height, border.getRGB());
        graphics.fill(x, y, x + 1, y + height, border.getRGB());
        graphics.fill(x + width - 1, y, x + width, y + height, border.getRGB());
    }

    private static void drawText(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            String name,
            float health,
            float maxHealth,
            int armor,
            double armorToughness,
            double distance,
            boolean badHeadshot
    ) {
        String hpText = String.format("%.1f / %.1f HP", health, maxHealth);
        String armorText = armorToughness > 0.0D
                ? String.format("DEF: %d  T: %.1f", armor, armorToughness)
                : "DEF: " + armor;
        String distanceText = String.format("%.1f m", distance);

        int nameColor = 0xFFFFFFFF;
        graphics.text(mc.font, name, x + 8, y + 7, nameColor, true);
        graphics.text(mc.font, hpText, x + 8, y + 18, 0xFFFF5555, true);

        graphics.text(mc.font, distanceText, x + 105, y + 18, 0xFFAAAAAA, true);
//        graphics.text(mc.font, armorText, x + 8, y + 42, 0xFF55AAFF, true);
    }





    public static boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        if (!living.isAlive()) {
            return false;
        }

        if (entity == mc.player) {
            return false;
        }

        if (entity instanceof Player) {
            return false;
        }

        if (entity instanceof ArmorStand) {
            return false;
        }
        String name = entity.getName().getString();

        if (name.equalsIgnoreCase("Farmer")) {
            return false;
        }
        return true;
    }


}
