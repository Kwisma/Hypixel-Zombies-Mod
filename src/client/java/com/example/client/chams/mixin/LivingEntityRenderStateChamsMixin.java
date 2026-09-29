package com.example.client.chams.mixin;

import com.example.client.chams.ChamsState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateChamsMixin implements ChamsState {
    @Unique
    private boolean zombiesmod$chams;

    @Override
    public boolean zombiesmod$isChams() {
        return zombiesmod$chams;
    }

    @Override
    public void zombiesmod$setChams(boolean chams) {
        zombiesmod$chams = chams;
    }
}
