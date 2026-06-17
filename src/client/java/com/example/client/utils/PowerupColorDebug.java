package com.example.client.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 调试：记录附近盔甲架名牌颜色的变化。
 * 只在颜色变化/出现/消失时打印——静态标签只记一次，道具快消失时的"白↔原色"闪烁会成行打出来，
 * 正好能抓到闪烁的两个颜色。
 *
 * 用法：在 zombies 里每 tick 调一次 {@link #tick()}（比如 ServerTracker.onTick 里）。
 */
public final class PowerupColorDebug implements IMinecraft {

    private static final Map<Integer, Integer> lastColor = new HashMap<>();

    private PowerupColorDebug() {}

    public static void tick() {
        if (mc.level == null || mc.player == null) return;

        Set<Integer> seen = new HashSet<>();
        //#AA00AA
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ArmorStand stand)) continue;
            if (stand.distanceToSqr(mc.player) > 256 * 256) continue; // 仅 16 格内，减噪
            Component name = stand.getCustomName();
            if (name == null) continue;

            int id = stand.getId();
            seen.add(id);

            int rgb = firstColor(name);
            Integer prev = lastColor.get(id);

            if (prev == null) {
                lastColor.put(id, rgb);
                log(id, "APPEAR", rgb, stand, name);
            } else if (prev != rgb) {
                lastColor.put(id, rgb);
                log(id, "COLOR ", rgb, stand, name); // 颜色变了 → 很可能就是闪烁
            }
        }

        // 已消失的：打印 GONE 并清理
        lastColor.keySet().removeIf(id -> {
            if (!seen.contains(id)) {
                ChatUtils.print("[STAND " + id + "] GONE");
                System.out.println("[STAND " + id + "] GONE");
                return true;
            }
            return false;
        });
    }

    private static void log(int id, String tag, int rgb, ArmorStand stand, Component name) {
        System.out.println("[STAND " + id + "] " + tag + " "
                + (rgb == -1 ? "none" : String.format("#%06X", rgb))
                + "  visible=" + stand.isCustomNameVisible()
                + "  '" + name.getString() + "'");
        ChatUtils.print("检测到！！！！！！！！！！！！！！");
    }

    /** 递归取第一个非空颜色（颜色常挂在子节点上）。 */
    private static int firstColor(Component c) {
        if (c.getStyle().getColor() != null) {
            return c.getStyle().getColor().getValue();
        }
        for (Component sib : c.getSiblings()) {
            int v = firstColor(sib);
            if (v != -1) return v;
        }
        return -1;
    }
}
