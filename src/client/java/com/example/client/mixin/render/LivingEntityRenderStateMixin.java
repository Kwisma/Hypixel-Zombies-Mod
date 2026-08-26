package com.example.client.mixin.render;

import com.example.client.utils.BadHeadshotOutlineState;
import com.example.client.utils.HideEntityState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements HideEntityState, BadHeadshotOutlineState {

    @Unique
    private boolean zombiesmod$faded;

    @Unique
    private int zombiesmod$badHeadshotBoxColor;

    @Override
    public boolean zombiesmod$isFaded() {
        return this.zombiesmod$faded;
    }

    @Override
    public void zombiesmod$setFaded(boolean faded) {
        this.zombiesmod$faded = faded;
    }

    @Override
    public int zombiesmod$getBadHeadshotBoxColor() {
        return this.zombiesmod$badHeadshotBoxColor;
    }

    @Override
    public void zombiesmod$setBadHeadshotBoxColor(int color) {
        this.zombiesmod$badHeadshotBoxColor = color;
    }
}
