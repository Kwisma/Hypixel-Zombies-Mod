package com.example.client.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 MouseHandler 私有的 onButton，用来模拟"真实鼠标按键事件"。
 * 走这条路 CPS 计数器（钩在 onButton 上的）才看得到连点器的点击。
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
    @Invoker("onButton")
    void zombiesmod$onButton(long handle, MouseButtonInfo info, int action);
}
