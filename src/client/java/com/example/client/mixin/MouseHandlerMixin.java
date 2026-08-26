package com.example.client.mixin;

import com.darkmagician6.eventapi.EventManager;
import com.example.client.events.MouseInputEvent;
import com.example.client.utils.IMinecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin implements IMinecraft {

    @Inject(method = "onButton", at = @At("HEAD"))
    private void zombiesmod$mouseButton(long handle, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (handle != mc.getWindow().handle()) {
            return;
        }
        EventManager.call(new MouseInputEvent(info.button(), action, info.modifiers()));
    }
}
