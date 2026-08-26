package com.example.client.mixin.render;

import com.example.client.module.modules.BadHeadshot;
import com.example.client.utils.BadHeadshotOutlineState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class BadHeadshotRenderMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void zombiesmod$flagBadHeadshot(LivingEntity entity, LivingEntityRenderState state,
                                             float partialTicks, CallbackInfo ci) {
        if (state instanceof BadHeadshotOutlineState badHeadshotState) {
            badHeadshotState.zombiesmod$setBadHeadshotBoxColor(BadHeadshot.boxColor(entity));
        }
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void zombiesmod$submitBadHeadshotBox(LivingEntityRenderState state, PoseStack poseStack,
                                                  SubmitNodeCollector collector, CameraRenderState camera,
                                                  CallbackInfo ci) {
        int color = state instanceof BadHeadshotOutlineState badHeadshotState
                ? badHeadshotState.zombiesmod$getBadHeadshotBoxColor()
                : 0;
        if (color == 0) {
            return;
        }

        float width = Math.max(0.1F, state.boundingBoxWidth);
        float height = Math.max(0.1F, state.boundingBoxHeight);
        float halfWidth = width / 2.0F;
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.debugQuads(),
                (pose, consumer) -> zombiesmod$drawBox(
                        pose.pose(), consumer,
                        -halfWidth, 0.0F, -halfWidth,
                        halfWidth, height, halfWidth,
                        color
                )
        );
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
