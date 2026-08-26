package com.example.client.chams.mixin;

import com.example.client.chams.ChamsRenderType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * chams 激活期间（见 {@link ChamsRenderType#active}），把实体/盔甲等层请求的 RenderType
 * 统一换成无深度版，让整只僵尸（含盔甲、头颅等所有走 RenderTypes 的层）一起穿墙。
 * 用各层自己的贴图，所以外观正常，只是深度被关掉。
 */
@Mixin(RenderTypes.class)
public class RenderTypesChamsMixin {

    @Inject(method = "armorCutoutNoCull", at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$armorCutout(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));
    }

    @Inject(method = "armorTranslucent", at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$armorTranslucent(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));
    }

    @Inject(method = "entitySolid", at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$entitySolid(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));

    }

    @Inject(method = "entityCutoutCull", at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$entityCutoutCull(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));
    }

    @Inject(method = "entityCutoutZOffset(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$entityCutoutZOffset(Identifier texture, boolean affectsOutline,
                                                        CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));
    }

    @Inject(method = "entityCutout(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$entityCutout(Identifier texture, boolean affectsOutline, CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));
    }

    @Inject(method = "entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$entityTranslucent(Identifier texture, boolean affectsOutline, CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));
    }

    // ===== 手持武器/物品：物品层用的 RenderType（同为 ENTITY 顶点格式，可复用无深度类型）=====
    @Inject(method = "itemCutout", at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$itemCutout(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));
    }

    @Inject(method = "itemTranslucent", at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$itemTranslucent(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));
    }

    @Inject(method = "entityTranslucentCullItemTarget", at = @At("HEAD"), cancellable = true)
    private static void zombiesmod$entityTranslucentCullItemTarget(Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (ChamsRenderType.active) cir.setReturnValue(ChamsRenderType.noDepth(texture));
    }
}
