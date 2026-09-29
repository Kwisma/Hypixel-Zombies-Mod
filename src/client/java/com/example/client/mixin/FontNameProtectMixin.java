package com.example.client.mixin;

import com.example.client.config.ZombiesConfig;
import com.example.client.module.modules.NameProtect;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 名字保护——单点全包。
 * 26.1 里所有文字（GUI 的聊天/TAB/计分板/HUD，以及世界里的名牌）最终都经过 Font.prepareText 生成字形，
 * 所以 hook prepareText 的两个重载即可覆盖一切；drawInBatch8xOutline 不走 prepareText，单独接。
 * width 同步替换，保证居中/对齐不偏。名单为空时原样返回（零开销）。
 */
@Mixin(Font.class)
public class FontNameProtectMixin {

    // ===== 文字生成总入口 prepareText =====

    @ModifyVariable(method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true)
    private String zombiesmod$prepString(String text) {
        return NameProtect.apply(text);
    }

    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence zombiesmod$prepFcs(FormattedCharSequence text) {
        return NameProtect.protect(text);
    }

    // ===== width（替换后宽度要一致）=====

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String zombiesmod$widthString(String str) {
        return NameProtect.apply(str);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedText zombiesmod$widthText(FormattedText text) {
        if (ZombiesConfig.protectedNames.isEmpty() || text == null) return text;
        if (text instanceof Component comp) {
            return NameProtect.protect(comp);
        }
        String plain = text.getString();
        String applied = NameProtect.apply(plain);
        return applied.equals(plain) ? text : FormattedText.of(applied);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence zombiesmod$widthFcs(FormattedCharSequence text) {
        return NameProtect.protect(text);
    }
}
