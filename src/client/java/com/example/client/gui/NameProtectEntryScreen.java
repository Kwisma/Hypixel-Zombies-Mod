package com.example.client.gui;

import com.example.client.config.ZombiesConfig;
import com.example.client.module.modules.NameProtect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.Color;

/** 单个受保护玩家的显示设置。 */
public class NameProtectEntryScreen extends Screen {
    private final Screen parent;
    private final String realName;
    private final int index;
    private EditBox aliasBox;
    private EditBox colorBox;
    private EditBox bracketColorBox;
    private EditBox rankTextColorBox;
    private EditBox plusColorBox;
    private EditBox prefixBox;
    private Button renameButton;
    private String validation = "";

    public NameProtectEntryScreen(Screen parent, String realName, int index) {
        super(Component.literal("Name Protect Settings"));
        this.parent = parent;
        this.realName = realName;
        this.index = index;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        ZombiesConfig.NameProtectSettings settings = ZombiesConfig.getNameProtectSettings(realName);

        aliasBox = new EditBox(font, cx - 130, 70, 260, 20, Component.literal("Display name"));
        aliasBox.setMaxLength(32);
        aliasBox.setValue(NameProtect.aliasFor(realName, index));
        addRenderableWidget(aliasBox);

        renameButton = addRenderableWidget(Button.builder(renameLabel(settings.isRenameName()), b -> {
            ZombiesConfig.NameProtectSettings current = ZombiesConfig.getNameProtectSettings(realName);
            current.setRenameName(!current.isRenameName());
            b.setMessage(renameLabel(current.isRenameName()));
            ZombiesConfig.save();
        }).bounds(cx - 130, 94, 260, 20).build());

        colorBox = new EditBox(font, cx - 130, 126, 260, 20, Component.literal("R,G,B"));
        colorBox.setMaxLength(11);
        colorBox.setValue(settings.getNameColor() < 0 ? "" : colorText(settings.getNameColor()));
        colorBox.setHint(Component.literal("Original color (or R,G,B)"));
        addRenderableWidget(colorBox);

        bracketColorBox = new EditBox(font, cx - 130, 158, 260, 20, Component.literal("Bracket color"));
        bracketColorBox.setMaxLength(11);
        bracketColorBox.setValue(settings.getBracketColor() < 0 ? "" : colorText(settings.getBracketColor()));
        bracketColorBox.setHint(Component.literal("Original bracket color (or R,G,B)"));
        addRenderableWidget(bracketColorBox);

        rankTextColorBox = new EditBox(font, cx - 130, 190, 260, 20, Component.literal("Rank text color"));
        rankTextColorBox.setMaxLength(11);
        rankTextColorBox.setValue(settings.getRankTextColor() < 0 ? "" : colorText(settings.getRankTextColor()));
        rankTextColorBox.setHint(Component.literal("Original rank text color (or R,G,B)"));
        addRenderableWidget(rankTextColorBox);

        plusColorBox = new EditBox(font, cx - 130, 222, 260, 20, Component.literal("Plus color"));
        plusColorBox.setMaxLength(11);
        plusColorBox.setValue(settings.getPlusColor() < 0 ? "" : colorText(settings.getPlusColor()));
        plusColorBox.setHint(Component.literal("Original plus color (or R,G,B)"));
        addRenderableWidget(plusColorBox);

        prefixBox = new EditBox(font, cx - 130, 254, 260, 20, Component.literal("Rank prefix"));
        prefixBox.setMaxLength(32);
        prefixBox.setValue(settings.getPrefix());
        prefixBox.setHint(Component.literal("Original prefix (or MVP+++)"));
        addRenderableWidget(prefixBox);

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
                .bounds(cx - 130, 288, 126, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> reset())
                .bounds(cx + 4, 288, 126, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(cx - 130, 320, 260, 20).build());
    }

    private void save() {
        String alias = aliasBox.getValue().trim();
        if (alias.isEmpty()) {
            validation = "Display name cannot be empty.";
            return;
        }

        int color = parseColor(colorBox.getValue());
        int bracketColor = parseColor(bracketColorBox.getValue());
        int rankTextColor = parseColor(rankTextColorBox.getValue());
        int plusColor = parseColor(plusColorBox.getValue());
        if (color == Integer.MIN_VALUE || bracketColor == Integer.MIN_VALUE
                || rankTextColor == Integer.MIN_VALUE || plusColor == Integer.MIN_VALUE) {
            validation = "Colors must be empty or R,G,B (0-255).";
            return;
        }

        ZombiesConfig.NameProtectSettings settings = ZombiesConfig.getNameProtectSettings(realName);
        settings.setCustomName(alias);
        settings.setNameColor(color);
        settings.setBracketColor(bracketColor);
        settings.setRankTextColor(rankTextColor);
        settings.setPlusColor(plusColor);
        settings.setPrefix(prefixBox.getValue());
        ZombiesConfig.save();
        validation = "Saved";
    }

    private void reset() {
        ZombiesConfig.NameProtectSettings settings = ZombiesConfig.getNameProtectSettings(realName);
        settings.setRenameName(true);
        settings.setCustomName("");
        settings.setNameColor(-1);
        settings.setBracketColor(-1);
        settings.setRankTextColor(-1);
        settings.setPlusColor(-1);
        settings.setPrefix("");
        aliasBox.setValue(NameProtect.defaultAlias(index));
        renameButton.setMessage(renameLabel(true));
        colorBox.setValue("");
        bracketColorBox.setValue("");
        rankTextColorBox.setValue("");
        plusColorBox.setValue("");
        prefixBox.setValue("");
        ZombiesConfig.save();
        validation = "Restored default Player alias and original styles.";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int cx = this.width / 2;
        graphics.text(font, "Name Protect Settings", cx - font.width("Name Protect Settings") / 2, 18,
                Color.WHITE.getRGB(), true);
        graphics.text(font, "Real name: " + realName, cx - 130, 44, new Color(180, 180, 180).getRGB(), true);
        graphics.text(font, "Display name", cx - 130, 58, new Color(180, 180, 180).getRGB(), false);
        graphics.text(font, "Name color", cx - 130, 114, new Color(180, 180, 180).getRGB(), false);
        graphics.text(font, "Bracket color ([ and ])", cx - 130, 146, new Color(180, 180, 180).getRGB(), false);
        graphics.text(font, "Rank text color (VIP / MVP)", cx - 130, 178, new Color(180, 180, 180).getRGB(), false);
        graphics.text(font, "Plus color (+)", cx - 130, 210, new Color(180, 180, 180).getRGB(), false);
        graphics.text(font, "Hypixel rank prefix", cx - 130, 242, new Color(180, 180, 180).getRGB(), false);
        graphics.text(font, "[VIP] Name → [MVP+++] " + NameProtect.aliasFor(realName, index),
                cx - 130, 276, new Color(102, 204, 255).getRGB(), false);
        if (!validation.isEmpty()) {
            graphics.text(font, validation, cx - 130, 348,
                    validation.equals("Saved") ? new Color(102, 255, 102).getRGB() : new Color(255, 170, 85).getRGB(), true);
        }
    }

    private static int parseColor(String text) {
        if (text == null || text.trim().isEmpty()) return -1;
        String[] parts = text.trim().split(",");
        if (parts.length != 3) return Integer.MIN_VALUE;
        try {
            int red = Integer.parseInt(parts[0].trim());
            int green = Integer.parseInt(parts[1].trim());
            int blue = Integer.parseInt(parts[2].trim());
            if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
                return Integer.MIN_VALUE;
            }
            return new Color(red, green, blue).getRGB() & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private static String colorText(int color) {
        Color value = new Color(color);
        return value.getRed() + "," + value.getGreen() + "," + value.getBlue();
    }

    private static Component renameLabel(boolean enabled) {
        return Component.literal("Replace name: " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
