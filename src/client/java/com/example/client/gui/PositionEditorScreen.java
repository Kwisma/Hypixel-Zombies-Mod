package com.example.client.gui;

import com.example.client.config.ZombiesConfig;
import com.example.client.setting.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class PositionEditorScreen extends Screen {
    private final Screen parent;
    private final NumberSetting posX;
    private final NumberSetting posY;
    private boolean dragging;

    private static final int PREVIEW_WIDTH = 180;
    private static final int PREVIEW_HEIGHT = 42;

    public PositionEditorScreen(Screen parent, NumberSetting posX, NumberSetting posY) {
        super(Component.translatable("zombies-mod.gui.position_editor"));
        this.parent = parent;
        this.posX = posX;
        this.posY = posY;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Component title = Component.translatable("zombies-mod.gui.position_editor");
        graphics.text(this.font, title, this.width / 2 - this.font.width(title) / 2, 12, 0xFFFFFFFF, true);

        int x = previewX();
        int y = previewY();
        graphics.fill(x, y, x + PREVIEW_WIDTH, y + PREVIEW_HEIGHT, 0xDD26384A);
        graphics.fill(x, y, x + PREVIEW_WIDTH, y + 1, 0xFF66CCFF);
        graphics.fill(x, y + PREVIEW_HEIGHT - 1, x + PREVIEW_WIDTH, y + PREVIEW_HEIGHT, 0xFF66CCFF);
        graphics.fill(x, y, x + 1, y + PREVIEW_HEIGHT, 0xFF66CCFF);
        graphics.fill(x + PREVIEW_WIDTH - 1, y, x + PREVIEW_WIDTH, y + PREVIEW_HEIGHT, 0xFF66CCFF);

        Component coordinates = Component.literal(String.format("X: %.2f  Y: %.2f",
                posX.getValue().doubleValue(), posY.getValue().doubleValue()));
        graphics.text(this.font, coordinates, 12, this.height - 28, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.translatable("zombies-mod.gui.drag_to_move"),
                this.width - this.font.width(Component.translatable("zombies-mod.gui.drag_to_move")) - 12,
                this.height - 28, 0xFFAAAAAA, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isInsidePreview(event.x(), event.y())) {
            dragging = true;
            updatePosition(event.x(), event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging && event.button() == 0) {
            updatePosition(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && dragging) {
            dragging = false;
            ZombiesConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        ZombiesConfig.save();
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private int previewX() {
        return Math.clamp((int) (this.width * posX.getValue().doubleValue()), 0,
                Math.max(0, this.width - PREVIEW_WIDTH));
    }

    private int previewY() {
        return Math.clamp((int) (this.height * posY.getValue().doubleValue()), 0,
                Math.max(0, this.height - PREVIEW_HEIGHT));
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        return mouseX >= previewX() && mouseX <= previewX() + PREVIEW_WIDTH
                && mouseY >= previewY() && mouseY <= previewY() + PREVIEW_HEIGHT;
    }

    private void updatePosition(double mouseX, double mouseY) {
        double x = Math.clamp(mouseX / Math.max(1, this.width), 0.0D, 1.0D);
        double y = Math.clamp(mouseY / Math.max(1, this.height), 0.0D, 1.0D);
        posX.setValue(x);
        posY.setValue(y);
    }
}