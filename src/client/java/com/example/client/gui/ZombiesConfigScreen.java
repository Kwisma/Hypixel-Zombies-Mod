package com.example.client.gui;

import com.example.client.config.ZombiesConfig;
import com.example.client.ZombiesModClient;
import com.example.client.module.AbstractModule;
import com.example.client.setting.Setting;
import com.example.client.setting.SettingManager;
import com.example.client.setting.settings.BooleanSetting;
import com.example.client.setting.settings.ButtonSetting;
import com.example.client.setting.settings.ModeSetting;
import com.example.client.setting.settings.NumberSetting;
import com.example.client.utils.render.DoubleSliderButton;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class ZombiesConfigScreen extends Screen {
    public static ZombiesConfigScreen instance = null;

    private Screen parent;

    public void setParent(Screen parent) {
        this.parent = parent;
    }

    private ScrollPanelWidget scrollPanel;

    private Button bindButton;
    private boolean listeningForKey = false;

    /** 正在为哪个模块绑定按键（null = 没在绑） */
    private AbstractModule listeningModule = null;

    private static final int PANEL_WIDTH = 300;
    private static final int WIDGET_WIDTH = 220;

    private static final int TOP = 80;
    private static final int BOTTOM_SPACE = 45;

    private static final int ITEM_HEIGHT = 25;
    private static final int MODULE_PADDING = 8;
    private static final int MODULE_GAP = 10;

    public ZombiesConfigScreen(Screen parent) {
        super(Component.literal("Zombies Mod Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        this.listeningForKey = false;
        this.listeningModule = null;
        this.bindButton = Button.builder(
                bindText(),
                button -> {
                    this.listeningForKey = true;
                    button.setMessage(Component.literal("Gui Bind: ")
                            .append(Component.literal("<按任意键…>").withStyle(ChatFormatting.YELLOW)));
                }
        ).bounds(centerX - 100, 50, 200, 20).build();
        this.addRenderableWidget(this.bindButton);

        int panelX = centerX - PANEL_WIDTH / 2;
        int panelY = TOP;
        int panelH = this.height - TOP - BOTTOM_SPACE;

        this.scrollPanel = new ScrollPanelWidget(
                panelX,
                panelY,
                PANEL_WIDTH,
                panelH
        );

        buildModuleContent();

        this.addRenderableWidget(this.scrollPanel);

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                button -> {
                    ZombiesConfig.save();
                    this.onClose();
                }
        ).bounds(centerX - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (this.listeningForKey) {
            int key = event.key();
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                ZombiesModClient.guiKey = key;
                ZombiesConfig.save();
            }
            this.listeningForKey = false;
            if (this.bindButton != null) {
                this.bindButton.setMessage(bindText());
            }
            return true;
        }

        if (this.listeningModule != null) {
            int key = event.key();
            this.listeningModule.setKey(key == GLFW.GLFW_KEY_ESCAPE ? 0 : key); // ESC解绑
            this.listeningModule = null;
            ZombiesConfig.save();
            rebuildContent(); // 刷新按钮上显示的键名
            return true;
        }
        return super.keyPressed(event);
    }

    /** 绑定按钮文字：显示当前打开 GUI 的按键名。 */
    private static Component bindText() {
        String keyName = InputConstants.Type.KEYSYM
                .getOrCreate(ZombiesModClient.guiKey)
                .getDisplayName()
                .getString();
        return Component.literal("Gui Bind: ")
                .append(Component.literal(keyName).withStyle(ChatFormatting.AQUA));
    }

    /** 模块绑定按钮文字：显示该模块的开关键名，未绑定显示 None。 */
    private static Component moduleKeyText(AbstractModule module) {
        int key = module.getKey();
        if (key <= 0) {
            return Component.literal("None").withStyle(ChatFormatting.GRAY);
        }
        String keyName = InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
        return Component.literal(keyName).withStyle(ChatFormatting.AQUA);
    }

    /** 设置变化后重建滚动面板内容，让子设置的显示/隐藏立即生效（无需重开 GUI），并保持滚动条位置。 */
    private void rebuildContent() {
        int savedOffset = this.scrollPanel.getScrollOffset();
        buildModuleContent();
        this.scrollPanel.setScrollOffset(savedOffset); // buildModuleContent 已更新内容高度，这里会按新高度夹紧
    }

    private void buildModuleContent() {
        this.scrollPanel.clearContent();

        int y = 10;

        for (AbstractModule module : ZombiesModClient.moduleManager.getModuleList()) {
            int moduleStartY = y;


            y += MODULE_PADDING + 12;

            // 模块开关（左），与下方设置项左右对齐（设置项 x=40、宽220）
            this.scrollPanel.addScrollWidget(Button.builder(
                    boolText(module.getName(), module.isEnable()),
                    button -> {
                        module.toggle();
                        button.setMessage(boolText(module.getName(), module.isEnable()));
                        ZombiesConfig.save();
                    }
            ).bounds(0, 0, 155, 20).build(), 40, y);

            // 模块按键绑定（右）：点一下进入监听，按下的键即为该模块的开关键；ESC 解绑
            this.scrollPanel.addScrollWidget(Button.builder(
                    moduleKeyText(module),
                    button -> {
                        this.listeningModule = module;
                        button.setMessage(Component.literal("<…>").withStyle(ChatFormatting.YELLOW));
                    }
            ).bounds(0, 0, 60, 20).build(), 200, y);

            y += ITEM_HEIGHT;

            for (Setting<?> setting : SettingManager.getSettings(module)) {
                if (!setting.isDisplay()) continue;

                switch (setting) {
                    case BooleanSetting booleanSetting -> {
                        this.scrollPanel.addScrollWidget(Button.builder(
                                boolText(setting.getName(), Boolean.TRUE.equals(booleanSetting.getValue())),
                                button -> {
                                    boolean newValue = !Boolean.TRUE.equals(booleanSetting.getValue());

                                    booleanSetting.setValue(newValue);
                                    button.setMessage(boolText(setting.getName(), newValue));

                                    ZombiesConfig.save();

                                    rebuildContent();//刷新
                                }
                        ).bounds(0, 0, WIDGET_WIDTH, 20).build(), y);

                        y += ITEM_HEIGHT;
                    }
                    case NumberSetting numberSetting -> {
                        this.scrollPanel.addScrollWidget(new DoubleSliderButton(
                                0,
                                0,
                                WIDGET_WIDTH,
                                20,
                                setting.getName(),
                                numberSetting.getMin(),
                                numberSetting.getMax(),
                                numberSetting.getValue().doubleValue(),
                                stepFromFormat(numberSetting.getPrecisePattern()),
                                value -> {
                                    numberSetting.setValue(value);
                                    ZombiesConfig.save();
                                }
                        ), y);

                        y += ITEM_HEIGHT;
                    }
                    case ModeSetting modeSetting -> {
                        this.scrollPanel.addScrollWidget(Button.builder(
                                modeText(setting.getName(), modeSetting.getValue()),
                                button -> {
                                    String newMode = modeSetting.next();

                                    button.setMessage(modeText(setting.getName(), newMode));

                                    ZombiesConfig.save();
                                    rebuildContent(); // 刷新
                                }
                        ).bounds(0, 0, WIDGET_WIDTH, 20).build(), y);
                        y += ITEM_HEIGHT;
                    }
                    case ButtonSetting buttonSetting -> {
                        this.scrollPanel.addScrollWidget(Button.builder(
                                Component.literal(setting.getName()),
                                button ->
                                        buttonSetting.onClickedButton()
                        ).bounds(0, 0, WIDGET_WIDTH, 20).build(), y);
                        y += ITEM_HEIGHT;

                    }
                    default -> {
                    }
                }
            }

            y += MODULE_PADDING;

            int moduleHeight = y - moduleStartY;

            this.scrollPanel.addModuleBox(
                    module.getName(),
                    moduleStartY,
                    moduleHeight
            );

            y += MODULE_GAP;
        }

        this.scrollPanel.setContentHeight(y);
    }

    private static double stepFromFormat(String pattern) {
        int dot = pattern.indexOf('.');

        if (dot == -1) {
            return 1.0;
        }

        int decimals = pattern.length() - dot - 1;

        return Math.pow(10, -decimals);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(
                this.font,
                "Zombies Mod Settings",
                this.width / 2 - 55,
                25,
                0xFFFFFFFF,
                true
        );
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    private static Component boolText(String name, boolean value) {
        return Component.literal(name + ": ")
                .append(Component.literal(value ? "ON" : "OFF")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static Component modeText(String name, Object value) {
        return Component.literal(name + ": ")
                .append(Component.literal(String.valueOf(value))
                        .withStyle(ChatFormatting.AQUA));
    }
}