package com.example.client.module.modules;

import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import com.example.client.setting.annotation.SettingInfo;
import com.example.client.setting.settings.BooleanSetting;

@ModuleInfo(name = {
        @Text(label = "Zombie Chams", language = Language.English),
        @Text(label = "僵尸穿墙显示", language = Language.Chinese)
}, enable = false)
public class ZombieChams extends AbstractModule {

    @SettingInfo(name = {
            @Text(label = "Only In Zombies", language = Language.English),
            @Text(label = "仅在僵尸末日里", language = Language.Chinese)
    })
    public static final BooleanSetting onlyGame = new BooleanSetting(true);

    public ZombieChams() {
        registerSetting(onlyGame);
    }
}
