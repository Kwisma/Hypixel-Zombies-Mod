package com.example.client.chams;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;

import java.util.List;

/** Routes every submission to one explicit feature-render order. */
public final class FixedOrderSubmitNodeCollector implements SubmitNodeCollector {
    private final OrderedSubmitNodeCollector delegate;

    public FixedOrderSubmitNodeCollector(OrderedSubmitNodeCollector delegate) {
        this.delegate = delegate;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return delegate;
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        delegate.submitShadow(poseStack, radius, pieces);
    }

    @Override
    public void submitNameTag(PoseStack poseStack, Vec3 offset, int light, Component text,
                              boolean discrete, int backgroundColor, CameraRenderState camera) {
        delegate.submitNameTag(poseStack, offset, light, text, discrete, backgroundColor, camera);
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text,
                           boolean dropShadow, Font.DisplayMode mode, int color, int backgroundColor,
                           int light, int packedOverlay) {
        delegate.submitText(poseStack, x, y, text, dropShadow, mode, color, backgroundColor, light, packedOverlay);
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf rotation) {
        delegate.submitFlame(poseStack, state, rotation);
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState state) {
        delegate.submitLeash(poseStack, state);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                int light, int overlay, int color, TextureAtlasSprite sprite, int outlineColor,
                                ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        delegate.submitModel(model, state, poseStack, renderType, light, overlay, color, sprite,
                outlineColor, crumblingOverlay);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state, int light) {
        delegate.submitMovingBlock(poseStack, state, light);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType,
                                 List<BlockStateModelPart> parts, int[] tints,
                                 int light, int overlay, int outlineColor) {
        delegate.submitBlockModel(poseStack, renderType, parts, tints, light, overlay, outlineColor);
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int stage) {
        delegate.submitBreakingBlockModel(poseStack, parts, stage);
    }

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType,
                                   int color, float lineWidth, boolean afterTerrain) {
        delegate.submitShapeOutline(poseStack, shape, renderType, color, lineWidth, afterTerrain);
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext,
                           int light, int overlay, int outlineColor, int[] tints,
                           List<net.minecraft.client.resources.model.geometry.BakedQuad> quads,
                           ItemStackRenderState.FoilType foilType) {
        delegate.submitItem(poseStack, displayContext, light, overlay, outlineColor, tints,
                new DepthSeedItemQuads(quads), foilType);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType,
                                     SubmitNodeCollector.CustomGeometryRenderer renderer) {
        // Chams 深度种子只需要实体模型；调试框等自定义几何不能跟随递归提交重复绘制。
    }

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState state) {
        delegate.submitQuadParticleGroup(state);
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group,
                                      CameraRenderState camera, boolean alwaysOnTop) {
        delegate.submitGizmoPrimitives(group, camera, alwaysOnTop);
    }
}
