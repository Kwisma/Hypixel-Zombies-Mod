package com.example.client.mixin.render;

import com.example.client.utils.ChamsState;
import com.example.client.utils.BadHeadshotState;
import com.example.client.utils.HideEntityState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements ChamsState, HideEntityState, BadHeadshotState {

    @Unique
    private boolean zombiesmod$chams;

    @Unique
    private boolean zombiesmod$faded;

    @Unique
    private int zombiesmod$badHeadshotTint;

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

    @Override
    public int zombiesmod$getBadHeadshotTint() {
        return this.zombiesmod$badHeadshotTint;
    }

    @Override
    public void zombiesmod$setBadHeadshotTint(int tint) {
        this.zombiesmod$badHeadshotTint = tint;
    }
}
