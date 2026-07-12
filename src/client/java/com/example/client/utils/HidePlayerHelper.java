package com.example.client.utils;

import com.example.client.ZombiesModClient;
import com.example.client.module.modules.HideBlockingPlayer;
import com.example.client.module.modules.HideZombies;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class HidePlayerHelper implements IMinecraft {
    public static boolean shouldFade(Player target) {
        HideBlockingPlayer hideBlockingPlayer = ((HideBlockingPlayer) ZombiesModClient.moduleManager.getModule("Hide Blocking Player"));
        if(hideBlockingPlayer == null) return false;
        if(!hideBlockingPlayer.isEnable()) return false;

        LocalPlayer self = mc.player;

        if (self == null || mc.level == null) {
            return false;
        }

        if (target == self) {
            return false;
        }

        if (target.isInvisible()) {
            return false;
        }

        return overlapsSelf(target, HideBlockingPlayer.fadeOverlapExpand.getValue().doubleValue());
    }

    public static boolean shouldFade(Zombie target) {
        return shouldFadeZombieLike(target);
    }

    public static boolean shouldFade(Slime target) {
        return shouldFadeZombieLike(target);
    }

    public static boolean shouldFade(IronGolem target) {
        return shouldFadeZombieLike(target);
    }

    public static boolean shouldFade(Wolf target) {
        return shouldFadeZombieLike(target);
    }

    private static boolean shouldFadeZombieLike(LivingEntity target) {
        HideZombies hideZombies = (HideZombies) ZombiesModClient.moduleManager.getModule("Hide Zombies");
        if (hideZombies == null || !hideZombies.isEnable()) {
            return false;
        }

        return overlapsSelf(target, HideZombies.fadeOverlapExpand.getValue().doubleValue());
    }

    public static boolean shouldFade(LivingEntity target) {
        if (target instanceof Player player) {
            return shouldFade(player);
        }
        if (isHideZombieTarget(target)) {
            return shouldFadeZombieLike(target);
        }
        return false;
    }

    public static boolean shouldFullyHide(Entity target) {
        if (!(target instanceof LivingEntity livingEntity) || !shouldFade(livingEntity)) {
            return false;
        }

        if (livingEntity instanceof Player) {
            return HideBlockingPlayer.fullHide.getValue();
        }
        if (isHideZombieTarget(livingEntity)) {
            return HideZombies.fullHide.getValue();
        }
        return false;
    }

    public static boolean isFullHide(LivingEntity target) {
        if (target instanceof Player) {
            return HideBlockingPlayer.fullHide.getValue();
        }
        if (isHideZombieTarget(target)) {
            return HideZombies.fullHide.getValue();
        }
        return false;
    }

    public static boolean isHideZombieTarget(LivingEntity target) {
        return target instanceof Zombie
                || target instanceof Slime
                || target instanceof IronGolem
                || target instanceof Wolf;
    }

    private static boolean overlapsSelf(LivingEntity target, double expand) {
        LocalPlayer self = mc.player;

        if (self == null || mc.level == null || target == self || target.isInvisible()) {
            return false;
        }

        AABB selfBox = self.getBoundingBox().inflate(expand, 0.1, expand);
        AABB targetBox = target.getBoundingBox();

        return selfBox.intersects(targetBox);
    }

    public static int alphaWhite(int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | 0xFFFFFF;
    }
}
