package com.example.client.gui;

import com.example.client.api.HypixelStats;
import com.example.client.config.ZombiesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatsQueryScreen extends Screen {

    private final Screen parent;
    private EditBox apiKeyBox;
    private boolean apiKeyHidden = true;

    private record Entry(String name, UUID uuid) {}
    private final List<Entry> players = new ArrayList<>();

    private ScrollPanelWidget scrollPanel;

    private static final int ROW_HEIGHT = 30;
    private static final int LIST_TOP = 96;
    private static final int BOTTOM_SPACE = 44;

    public StatsQueryScreen(Screen parent) {
        super(Component.literal("Stats Query"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        Minecraft mc = Minecraft.getInstance();

        // API key 输入框
        this.apiKeyBox = new EditBox(this.font, cx - 150, 40, 274, 20, Component.literal("API Key"));
        this.apiKeyBox.setMaxLength(64);
        this.apiKeyBox.setResponder(s -> { if (!apiKeyHidden) ZombiesConfig.apiKey = s; });
        applyMask();
        this.addRenderableWidget(this.apiKeyBox);

        // 显示/隐藏切换
        this.addRenderableWidget(Button.builder(eyeLabel(), b -> {
            apiKeyHidden = !apiKeyHidden;
            applyMask();
            b.setMessage(eyeLabel());
        }).bounds(cx + 130, 40, 20, 20).build());

        // 玩家列表（TAB 在场玩家）
        players.clear();
        if (mc.getConnection() != null) {
            for (PlayerInfo pi : mc.getConnection().getOnlinePlayers()) {
                players.add(new Entry(pi.getProfile().name(), pi.getProfile().id()));
            }
        }

        // 一键查询
        this.addRenderableWidget(Button.builder(Component.literal("一键查询 (Query All)"), b -> {
            for (Entry e : players) HypixelStats.query(e.name(), e.uuid());
        }).bounds(cx - 100, 66, 200, 20).build());

        // 滚动面板（复用 ScrollPanelWidget，自带滚动条/拖动/滚轮）
        int panelW = 320;
        this.scrollPanel = new ScrollPanelWidget(cx - panelW / 2, LIST_TOP, panelW, this.height - LIST_TOP - BOTTOM_SPACE);
        for (int i = 0; i < players.size(); i++) {
            Entry e = players.get(i);
            PlayerRowWidget row = new PlayerRowWidget(panelW - 16, ROW_HEIGHT, e.name(), e.uuid());
            this.scrollPanel.addScrollWidget(row, 4, i * ROW_HEIGHT + 4);
        }
        this.scrollPanel.setContentHeight(players.size() * ROW_HEIGHT + 8);
        this.addRenderableWidget(this.scrollPanel);

        // Done
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx - 100, this.height - 28, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int cx = this.width / 2;
        graphics.text(this.font, "Stats Query", cx - 30, 16, 0xFFFFFFFF, true);
        graphics.text(this.font, "API Key:", cx - 150, 30, 0xFFAAAAAA, true);
    }

    private Component eyeLabel() {
        return Component.literal(apiKeyHidden ? "*" : "A");
    }

    private void applyMask() {
        if (apiKeyHidden) {
            this.apiKeyBox.setEditable(false);
            this.apiKeyBox.setValue("*".repeat(ZombiesConfig.apiKey.length()));
        } else {
            this.apiKeyBox.setEditable(true);
            this.apiKeyBox.setValue(ZombiesConfig.apiKey);
        }
    }

    @Override
    public void onClose() {
        ZombiesConfig.save();
        Minecraft.getInstance().setScreen(parent);
    }

    /** 一个玩家行：自己每帧读战绩画出来，点击即查询。交给 ScrollPanelWidget 负责滚动。 */
    private static final class PlayerRowWidget extends AbstractWidget {
        private final String name;
        private final UUID uuid;
        private final Font font = Minecraft.getInstance().font;

        PlayerRowWidget(int width, int height, String name, UUID uuid) {
            super(0, 0, width, height, Component.literal(name));
            this.name = name;
            this.uuid = uuid;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            boolean hover = mouseX >= getX() && mouseX < getX() + width
                    && mouseY >= getY() && mouseY < getY() + height;
            if (hover) {
                g.fill(getX(), getY(), getX() + width, getY() + height, 0x33FFFFFF);
            }

            g.text(font, name, getX() + 4, getY() + 2, 0xFFFFFFFF, true);

            HypixelStats.Result r = HypixelStats.get(uuid);
            if (r != null) {
                int c = r.error() ? 0xFFFF5555 : (r.loading() ? 0xFFAAAAAA : 0xFF66FF66);
                g.text(font, r.text(), getX() + 4, getY() + 13, c, true);
            }

            String tag = "[查询]";
            g.text(font, tag, getX() + width - font.width(tag) - 6, getY() + 2, 0xFF55FF55, true);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.active && this.visible && event.button() == 0
                    && event.x() >= getX() && event.x() < getX() + width
                    && event.y() >= getY() && event.y() < getY() + height) {
                HypixelStats.query(name, uuid);
                return true;
            }
            return false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {}
    }
}
