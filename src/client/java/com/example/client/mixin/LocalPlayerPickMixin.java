package com.example.client.mixin;

import com.example.client.module.modules.HologramFix;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(LocalPlayer.class)
public class LocalPlayerPickMixin {

    @ModifyArg(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"
            ),
            index = 4
    )
    private static Predicate<Entity> zombiesmod$ignoreHolograms(Predicate<Entity> original) {
        if (!HologramFix.isActiveInCurrentGame()) return original;
        // 在原过滤器（EntitySelector.CAN_BE_PICKED）基础上再排除所有盔甲架（隐形全息字）。
        return original.and(e -> !(e instanceof ArmorStand));
    }
}
