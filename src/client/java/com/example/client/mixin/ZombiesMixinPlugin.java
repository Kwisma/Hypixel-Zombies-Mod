package com.example.client.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * 守卫：依赖 Fabric Indigo 内部类的 mixin，只在目标类真的存在时才应用。
 * Lunar 等客户端剥掉了 Indigo 渲染器（没有 AltModelBlockRendererImpl），
 * 这时跳过对应 mixin，避免目标缺失导致的问题；其余 mixin 照常应用。
 */
public class ZombiesMixinPlugin implements IMixinConfigPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("AltModelBlockRendererTransparencyMixin")) {
            return classExists("net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl");
        }
        return true;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, ZombiesMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
