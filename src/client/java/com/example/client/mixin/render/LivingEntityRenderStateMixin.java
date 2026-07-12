package com.example.client.mixin.render;

import com.example.client.utils.ChamsState;
import com.example.client.utils.HideEntityState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements ChamsState, HideEntityState {

    @Unique
    private boolean zombiesmod$chams;

    @Unique
    private boolean zombiesmod$faded;

    @Override
    public boolean zombiesmod$isChams() {
        return this.zombiesmod$chams;
    }

    @Override
    public void zombiesmod$setChams(boolean chams) {
        this.zombiesmod$chams = chams;
    }

    @Override
    public boolean zombiesmod$isFaded() {
        return this.zombiesmod$faded;
    }

    @Override
    public void zombiesmod$setFaded(boolean faded) {
        this.zombiesmod$faded = faded;
    }

}
