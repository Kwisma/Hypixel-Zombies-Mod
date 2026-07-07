package com.example.client.mixin.render;

import com.example.client.utils.HideEntityState;
import com.example.client.utils.HidePlayerHelper;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL")
    )
    private void zombiesmod$makeOverlappingPlayerTranslucent(
            LivingEntity entity,
            LivingEntityRenderState state,
            float partialTicks,
            CallbackInfo ci
    ) {

        // 26.2 的实体渲染状态里，把玩家标记为 invisible 会直接导致玩家皮肤/body 不绘制。
        // 先禁用玩家的“半透明隐藏”路径；Full Hide 仍由 EntityRendererMixin 处理。
        boolean faded = !(entity instanceof Player)
                && HidePlayerHelper.shouldFade(entity)
                && !HidePlayerHelper.isFullHide(entity);
        if (state instanceof HideEntityState hideState) {
            hideState.zombiesmod$setFaded(faded);
        }
        if (!faded) {
            return;
        }
        //
        //让原版认为这个实体身体不可见，但对本地玩家仍可见
        //这样会走半透明渲染逻辑，而不是完全消失
        state.isInvisible = true;
        state.isInvisibleToPlayer = false;
    }
//
//    @Inject(
//            method = "getModelTint(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)I",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    private void zombiesmod$changeOverlappingPlayerAlpha(
//            LivingEntityRenderState state,
//            CallbackInfoReturnable<Integer> cir
//    ) {
//        if (!(state instanceof AvatarRenderState avatarState)) {
//            return;
//        }
//
//        if (!HidePlayerHelper.shouldFade(avatarState.id)) {
//            return;
//        }
//
//        cir.setReturnValue(HidePlayerHelper.alphaWhite(HideBlockingPlayer.fadePlayerAlpha.getValue().intValue()));
//    }
}
