package com.example.client.mixin.render;

import com.example.client.utils.ChamsState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements ChamsState {

    @Unique
    private boolean zombiesmod$chams;

    @Override
    public boolean zombiesmod$isChams() {
        return this.zombiesmod$chams;
    }

    @Override
    public void zombiesmod$setChams(boolean chams) {
        this.zombiesmod$chams = chams;
    }
}
