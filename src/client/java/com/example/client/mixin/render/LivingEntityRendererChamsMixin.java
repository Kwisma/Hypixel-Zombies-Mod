package com.example.client.mixin.render;

import com.example.client.ZombiesModClient;
import com.example.client.module.AbstractModule;
import com.example.client.module.modules.BadHeadshot;
import com.example.client.module.modules.ZombieChams;
import com.example.client.utils.BadHeadshotState;
import com.example.client.utils.ChamsState;
import com.example.client.utils.HideEntityState;
import com.example.client.utils.PlayerUtils;
import com.example.client.utils.render.ChamsRenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererChamsMixin {

    // 抽取阶段拿得到实体，判断是否 chams 目标，写进 render state
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void zombiesmod$flagChams(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
        int badHeadshotTint = BadHeadshot.tintFor(entity);
        if (state instanceof ChamsState cs) {
            cs.zombiesmod$setChams(zombiesmod$isChamsTarget(entity) || badHeadshotTint != 0);
        }
        if (state instanceof BadHeadshotState badHeadshotState) {
            badHeadshotState.zombiesmod$setBadHeadshotTint(badHeadshotTint);
        }
    }

    // 进入该实体 submit 时开启 chams 标记：期间所有走 RenderTypes 的层（本体/盔甲/头颅…）都会被换成无深度版
    @Inject(method = "submit", at = @At("HEAD"))
    private void zombiesmod$chamsSubmitStart(LivingEntityRenderState state, PoseStack poseStack,
                                             SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        ChamsRenderType.active = state instanceof ChamsState cs
                && cs.zombiesmod$isChams()
                && (!(state instanceof HideEntityState hideState) || !hideState.zombiesmod$isFaded());
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void zombiesmod$chamsSubmitEnd(LivingEntityRenderState state, PoseStack poseStack,
                                           SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        ChamsRenderType.active = false;
    }

    // 本体兜底：即使本体模型用了非 RenderTypes 的类型，也直接换成无深度
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void zombiesmod$chamsType(LivingEntityRenderState state, boolean isBodyVisible, boolean forceTransparent,
                                       boolean appearGlowing, CallbackInfoReturnable<RenderType> cir) {
        if (state instanceof ChamsState cs
                && cs.zombiesmod$isChams()
                && (!(state instanceof HideEntityState hideState) || !hideState.zombiesmod$isFaded())) {
            Identifier texture = ((LivingEntityRenderer) (Object) this).getTextureLocation(state);
            cir.setReturnValue(ChamsRenderType.noDepth(texture));
        }
    }

    @Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
    private void zombiesmod$badHeadshotTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (state instanceof BadHeadshotState badHeadshotState) {
            int tint = badHeadshotState.zombiesmod$getBadHeadshotTint();
            if (tint != 0) {
                cir.setReturnValue(tint);
            }
        }
    }

    @Unique
    private static boolean zombiesmod$isChamsTarget(LivingEntity entity) {
        if (ZombiesModClient.moduleManager == null) return false;
        AbstractModule m = ZombiesModClient.moduleManager.getModule("Zombie Chams");
        if (m == null || !m.isEnable()) return false;
        if (ZombieChams.onlyGame.getValue() && !PlayerUtils.isInHypZombies()) return false;
        return entity instanceof Enemy || entity instanceof Wolf || entity instanceof IronGolem;
    }
}
