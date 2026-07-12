package com.example.client.mixin.render;

import com.example.client.ZombiesModClient;
import com.example.client.module.AbstractModule;
import com.example.client.module.modules.BadHeadshot;
import com.example.client.module.modules.ZombieChams;
import com.example.client.utils.BadHeadshotOutlineState;
import com.example.client.utils.ChamsState;
import com.example.client.utils.HideEntityState;
import com.example.client.utils.PlayerUtils;
import com.example.client.utils.render.ChamsRenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import org.joml.Matrix4f;
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
        if (state instanceof ChamsState cs) {
            cs.zombiesmod$setChams(zombiesmod$isChamsTarget(entity));
        }
        if (state instanceof BadHeadshotOutlineState badHeadshotState) {
            badHeadshotState.zombiesmod$setBadHeadshotBoxColor(BadHeadshot.boxColor(entity));
        }
    }

    // 进入该实体 submit 时开启 chams 标记：期间所有走 RenderTypes 的层（本体/盔甲/头颅…）都会被换成无深度版
    @Inject(method = "submit", at = @At("HEAD"))
    private void zombiesmod$chamsSubmitStart(LivingEntityRenderState state, PoseStack poseStack,
                                             SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        ChamsRenderType.active = state instanceof ChamsState cs
                && cs.zombiesmod$isChams()
                && (!(state instanceof HideEntityState hideState) || !hideState.zombiesmod$isFaded());

        int badHeadshotColor = state instanceof BadHeadshotOutlineState badHeadshotState
                ? badHeadshotState.zombiesmod$getBadHeadshotBoxColor()
                : 0;
        if (badHeadshotColor != 0) {
            float width = Math.max(0.1F, state.boundingBoxWidth);
            float height = Math.max(0.1F, state.boundingBoxHeight);
            double halfWidth = width / 2.0D;
            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.debugQuads(),
                    (pose, consumer) -> zombiesmod$drawBox(
                            pose.pose(),
                            consumer,
                            (float) -halfWidth,
                            0.0F,
                            (float) -halfWidth,
                            (float) halfWidth,
                            height,
                            (float) halfWidth,
                            badHeadshotColor
                    )
            );
        }
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

    @Unique
    private static boolean zombiesmod$isChamsTarget(LivingEntity entity) {
        if (ZombiesModClient.moduleManager == null) return false;
        AbstractModule m = ZombiesModClient.moduleManager.getModule("Zombie Chams");
        if (m == null || !m.isEnable()) return false;
        if (ZombieChams.onlyGame.getValue() && !PlayerUtils.isInHypZombies()) return false;
        return entity instanceof Enemy || entity instanceof Wolf || entity instanceof IronGolem;
    }

    @Unique
    private static void zombiesmod$drawBox(Matrix4f pose, VertexConsumer consumer,
                                           float minX, float minY, float minZ,
                                           float maxX, float maxY, float maxZ,
                                           int color) {
        float thickness = 0.018F;

        zombiesmod$xEdge(pose, consumer, minX, maxX, minY, minZ, thickness, color);
        zombiesmod$xEdge(pose, consumer, minX, maxX, minY, maxZ, thickness, color);
        zombiesmod$xEdge(pose, consumer, minX, maxX, maxY, minZ, thickness, color);
        zombiesmod$xEdge(pose, consumer, minX, maxX, maxY, maxZ, thickness, color);

        zombiesmod$zEdge(pose, consumer, minZ, maxZ, minX, minY, thickness, color);
        zombiesmod$zEdge(pose, consumer, minZ, maxZ, maxX, minY, thickness, color);
        zombiesmod$zEdge(pose, consumer, minZ, maxZ, minX, maxY, thickness, color);
        zombiesmod$zEdge(pose, consumer, minZ, maxZ, maxX, maxY, thickness, color);

        zombiesmod$yEdge(pose, consumer, minY, maxY, minX, minZ, thickness, color);
        zombiesmod$yEdge(pose, consumer, minY, maxY, maxX, minZ, thickness, color);
        zombiesmod$yEdge(pose, consumer, minY, maxY, minX, maxZ, thickness, color);
        zombiesmod$yEdge(pose, consumer, minY, maxY, maxX, maxZ, thickness, color);
    }

    @Unique
    private static void zombiesmod$xEdge(Matrix4f pose, VertexConsumer consumer,
                                         float minX, float maxX, float y, float z,
                                         float thickness, int color) {
        zombiesmod$cuboid(pose, consumer,
                minX, y - thickness, z - thickness,
                maxX, y + thickness, z + thickness,
                color);
    }

    @Unique
    private static void zombiesmod$yEdge(Matrix4f pose, VertexConsumer consumer,
                                         float minY, float maxY, float x, float z,
                                         float thickness, int color) {
        zombiesmod$cuboid(pose, consumer,
                x - thickness, minY, z - thickness,
                x + thickness, maxY, z + thickness,
                color);
    }

    @Unique
    private static void zombiesmod$zEdge(Matrix4f pose, VertexConsumer consumer,
                                         float minZ, float maxZ, float x, float y,
                                         float thickness, int color) {
        zombiesmod$cuboid(pose, consumer,
                x - thickness, y - thickness, minZ,
                x + thickness, y + thickness, maxZ,
                color);
    }

    @Unique
    private static void zombiesmod$cuboid(Matrix4f pose, VertexConsumer consumer,
                                          float minX, float minY, float minZ,
                                          float maxX, float maxY, float maxZ,
                                          int color) {
        zombiesmod$quad(pose, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, color);
        zombiesmod$quad(pose, consumer, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
        zombiesmod$quad(pose, consumer, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, color);
        zombiesmod$quad(pose, consumer, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        zombiesmod$quad(pose, consumer, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, color);
        zombiesmod$quad(pose, consumer, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
    }

    @Unique
    private static void zombiesmod$quad(Matrix4f pose, VertexConsumer consumer,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float x3, float y3, float z3,
                                        float x4, float y4, float z4,
                                        int color) {
        consumer.addVertex(pose, x1, y1, z1).setColor(color);
        consumer.addVertex(pose, x2, y2, z2).setColor(color);
        consumer.addVertex(pose, x3, y3, z3).setColor(color);
        consumer.addVertex(pose, x4, y4, z4).setColor(color);
    }
}
