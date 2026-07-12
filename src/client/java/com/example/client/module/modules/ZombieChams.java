package com.example.client.module.modules;

import com.example.client.ZombiesModClient;
import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import com.example.client.setting.annotation.SettingInfo;
import com.example.client.setting.settings.BooleanSetting;
import com.example.client.utils.PlayerUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;

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

    public static boolean isTarget(Entity entity) {
        if (ZombiesModClient.moduleManager == null) {
            return false;
        }

        AbstractModule module = ZombiesModClient.moduleManager.getModule("Zombie Chams");
        if (module == null || !module.isEnable()) {
            return false;
        }

        if (onlyGame.getValue() && !PlayerUtils.isInHypZombies()) {
            return false;
        }

        return entity instanceof Enemy || entity instanceof Wolf || entity instanceof IronGolem;
    }
}
