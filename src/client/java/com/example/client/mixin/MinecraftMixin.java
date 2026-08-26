package com.example.client.mixin;

import com.example.client.ZombiesModClient;
import com.example.client.module.AbstractModule;
import com.example.client.module.modules.TeammatesGlow;
import com.example.client.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(
            method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void zombiesmod$shouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ZombiesModClient.moduleManager == null) return;

        // 队友发光（玩家）
        AbstractModule glow = ZombiesModClient.moduleManager.getModule("Teammates Glow");
        if (glow != null && glow.isEnable()
                && entity instanceof Player
                && !(TeammatesGlow.onlyGame.getValue() && !PlayerUtils.isInHypZombies())) {
            cir.setReturnValue(true);
            return;
        }

    }
}
