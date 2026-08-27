package com.example.client.mixin;

import com.example.client.ZombiesModClient;
import com.example.client.module.AbstractModule;
import com.example.client.module.modules.NoFireEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(
            method = "submitFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void zombiesmod$lowFire(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            TextureAtlasSprite sprite,
            CallbackInfo ci
    ) {
        AbstractModule noFire = ZombiesModClient.moduleManager.getModule("module.no_fire_effect");

        if (noFire == null || !noFire.isEnable()) {
            return;
        }

        ci.cancel();

        float alpha = NoFireEffect.fireAlpha.getValue().floatValue();

        if (alpha <= 0.0F) {
            return;
        }

        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.fireScreenEffect(sprite.atlasLocation()),
                (pose, builder) -> renderLowFire(pose.pose(), builder, sprite, alpha)
        );
    }

    private static void renderLowFire(
            Matrix4f basePose,
            VertexConsumer builder,
            TextureAtlasSprite sprite,
            float alpha
    ) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        alpha = Math.max(0.0F, Math.min(1.0F, alpha));

        for (int i = 0; i < 2; ++i) {
            Matrix4f pose = new Matrix4f(basePose);
            pose.translate((float) (-(i * 2 - 1)) * 0.24F, -0.65F, 0.0F);
            pose.rotate(Axis.YP.rotationDegrees((float) (i * 2 - 1) * 10.0F));

            builder.addVertex(pose, -0.5F, -0.5F, -0.5F)
                    .setUv(u1, v1)
                    .setColor(1.0F, 1.0F, 1.0F, alpha);

            builder.addVertex(pose, 0.5F, -0.5F, -0.5F)
                    .setUv(u0, v1)
                    .setColor(1.0F, 1.0F, 1.0F, alpha);

            builder.addVertex(pose, 0.5F, 0.5F, -0.5F)
                    .setUv(u0, v0)
                    .setColor(1.0F, 1.0F, 1.0F, alpha);

            builder.addVertex(pose, -0.5F, 0.5F, -0.5F)
                    .setUv(u1, v0)
                    .setColor(1.0F, 1.0F, 1.0F, alpha);
        }
    }
}
