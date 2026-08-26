package com.example.client.chams;

import net.minecraft.client.resources.model.geometry.BakedQuad;

import java.util.ArrayList;
import java.util.Collection;

/** Marker list used to distinguish the Chams item depth-seed submission from the normal item submission. */
public final class DepthSeedItemQuads extends ArrayList<BakedQuad> {
    public DepthSeedItemQuads(Collection<? extends BakedQuad> quads) {
        super(quads);
    }
}
