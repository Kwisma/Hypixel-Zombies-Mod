package com.example.client.config;
import com.example.client.data.ZombiesGuns;
import com.example.client.utils.ZombiesUtils;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class AutoSwitchWeaponConfig {

    public static final EnumMap<ZombiesGuns, GunSwitchSetting> GUN_SETTINGS = new EnumMap<>(ZombiesGuns.class);

    static {
        for (ZombiesGuns gun : ZombiesGuns.values()) {
            GUN_SETTINGS.put(gun, createDefaultSetting(gun));
        }
    }

    public static GunSwitchSetting get(ZombiesGuns gun) {
        return GUN_SETTINGS.computeIfAbsent(gun, AutoSwitchWeaponConfig::createDefaultSetting);
    }

    private static GunSwitchSetting createDefaultSetting(ZombiesGuns gun) {
        GunSwitchSetting setting = new GunSwitchSetting(true, getDefaultDelayMs(gun, 0));
        for (int level = 1; level <= gun.getUltimateLevelCount(); level++) {
            setting.setDelayMs(level, getDefaultDelayMs(gun, level));
        }
        return setting;
    }

    /**
     * 默认射击冷却，来源：僵尸末日数据.xlsx / Sheet4 / 射击冷却，单位毫秒。
     * level 0 = Base，1..5 = Ultimate I..V。
     */
    private static int getDefaultDelayMs(ZombiesGuns gun, int level) {
        int[] delays = switch (gun) {
            case Pistol -> new int[]{500, 400};
            case Rifle -> new int[]{200, 200};
            case Rainbow_Rifle -> new int[]{400, 300, 300, 300};
            case Shotgun -> new int[]{1400, 1000};
            case Rocket_Launcher -> new int[]{2000, 1500};
            case Sniper -> new int[]{1000, 1000};
            case Flamethrower -> new int[]{100, 100};
            case Blow_Dart -> new int[]{500, 300};
            case Zombie_Soaker -> new int[]{200, 200};
            case Zombie_Zapper -> new int[]{500, 500};
            case Double_Barrel_Shotgun -> new int[]{300, 300, 300, 300};
            case Elder_Gun -> new int[]{900, 800};
            case Gold_Digger -> new int[]{500, 500, 400, 300, 300, 250};
        };
        int index = Math.max(0, Math.min(level, delays.length - 1));
        return delays[index];
    }

    public static boolean shouldSwitch(ItemStack stack) {
        ZombiesGuns gun = ZombiesGuns.getGunOrNull(stack);
        if (gun == null) return false;
        return get(gun).isEnabled();
    }

    public static int getSwitchDelay(ItemStack stack) {
        ZombiesGuns gun = ZombiesGuns.getGunOrNull(stack);
        if (gun == null) return 120;
        int ultimateLevel = ZombiesUtils.getUltimateLevel(stack, gun);
        return get(gun).getDelayMs(ultimateLevel);
    }

    public static void saveTo(JsonObject root) {
        JsonObject autoSwitchJson = new JsonObject();

        for (Map.Entry<ZombiesGuns, GunSwitchSetting> entry : GUN_SETTINGS.entrySet()) {
            JsonObject gunJson = new JsonObject();

            gunJson.addProperty("enabled", entry.getValue().isEnabled());
            gunJson.addProperty("delayMs", entry.getValue().getDelayMs());
            gunJson.addProperty("key", entry.getValue().getKey());

            JsonObject cooldownsJson = new JsonObject();
            int maxLevel = entry.getKey().getUltimateLevelCount();
            for (int level = 0; level <= maxLevel; level++) {
                cooldownsJson.addProperty(Integer.toString(level), entry.getValue().getDelayMs(level));
            }
            gunJson.add("cooldownsMs", cooldownsJson);

            autoSwitchJson.add(entry.getKey().name(), gunJson);
        }

        root.add("autoSwitchWeapon", autoSwitchJson);
    }

    public static void loadFrom(JsonObject root) {
        if (root == null || !root.has("autoSwitchWeapon") || !root.get("autoSwitchWeapon").isJsonObject()) {
            return;
        }

        JsonObject autoSwitchJson = root.getAsJsonObject("autoSwitchWeapon");

        for (ZombiesGuns gun : ZombiesGuns.values()) {
            if (!autoSwitchJson.has(gun.name()) || !autoSwitchJson.get(gun.name()).isJsonObject()) {
                continue;
            }

            JsonObject gunJson = autoSwitchJson.getAsJsonObject(gun.name());
            GunSwitchSetting setting = get(gun);

            if (gunJson.has("enabled")) {
                setting.setEnabled(gunJson.get("enabled").getAsBoolean());
            }

            if (gunJson.has("delayMs")) {
                setting.setDelayMs(gunJson.get("delayMs").getAsInt());
            }

            // 新格式：按基础/强化等级分别保存。旧配置只有 delayMs 时，所有等级回退到该值。
            if (gunJson.has("cooldownsMs") && gunJson.get("cooldownsMs").isJsonObject()) {
                JsonObject cooldownsJson = gunJson.getAsJsonObject("cooldownsMs");
                for (String levelKey : cooldownsJson.keySet()) {
                    try {
                        int level = Integer.parseInt(levelKey);
                        if (level >= 0) {
                            setting.setDelayMs(level, cooldownsJson.get(levelKey).getAsInt());
                        }
                    } catch (NumberFormatException ignored) {
                        // 忽略未知键，保证损坏的单项不会影响整份配置加载。
                    }
                }
            }

            if (gunJson.has("key")) {
                setting.setKey(gunJson.get("key").getAsInt());
            }
        }
    }

    @Getter
    @Setter
    public static class GunSwitchSetting {
        private boolean enabled;
        private int delayMs;
        private int key; // 绑定的开关键（GLFW 键码），0 = 未绑定
        private final Map<Integer, Integer> cooldownsMs = new HashMap<>();

        public GunSwitchSetting(boolean enabled, int delayMs) {
            this.enabled = enabled;
            this.delayMs = delayMs;
            this.cooldownsMs.put(0, delayMs);
        }

        /** 兼容旧调用：基础枪的 cooldown。 */
        public int getDelayMs() {
            return getDelayMs(0);
        }

        /** 未单独配置的强化等级继承基础 cooldown。 */
        public int getDelayMs(int ultimateLevel) {
            return cooldownsMs.getOrDefault(Math.max(0, ultimateLevel), delayMs);
        }

        /** 兼容旧配置：设置基础值，并作为尚未配置等级的回退值。 */
        public void setDelayMs(int delayMs) {
            this.delayMs = delayMs;
            this.cooldownsMs.put(0, delayMs);
        }

        public void setDelayMs(int ultimateLevel, int delayMs) {
            int level = Math.max(0, ultimateLevel);
            cooldownsMs.put(level, delayMs);
            if (level == 0) {
                this.delayMs = delayMs;
            }
        }
    }
}
