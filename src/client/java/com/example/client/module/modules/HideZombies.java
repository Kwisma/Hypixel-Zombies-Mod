package com.example.client.module.modules;

import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import com.example.client.setting.annotation.SettingInfo;
import com.example.client.setting.settings.BooleanSetting;
import com.example.client.setting.settings.NumberSetting;

@ModuleInfo(name = {
        @Text(label = "Hide Zombies", language = Language.English),
        @Text(label = "隐藏阻挡僵尸", language = Language.Chinese)
}, enable = true)
public class HideZombies extends AbstractModule {
    @SettingInfo(name = {
            @Text(label = "Hide Overlap Expand", language = Language.English),
            @Text(label = "隐藏重叠扩展", language = Language.Chinese)
    })
    public static final NumberSetting fadeOverlapExpand = new NumberSetting(0.15d, 0, 1d, "#.0");

    @SettingInfo(name = {
            @Text(label = "Full Hide", language = Language.English),
            @Text(label = "完全隐藏", language = Language.Chinese)
    })
    public static final BooleanSetting fullHide = new BooleanSetting(false);

    public HideZombies() {
        registerSetting(fadeOverlapExpand, fullHide);
    }
}
