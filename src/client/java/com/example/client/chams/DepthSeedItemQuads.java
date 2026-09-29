package com.example.client.chams;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;

import java.util.ArrayList;
import java.util.Collection;

/** Marker list used to distinguish the Chams item depth-seed submission from the normal item submission. */
public final class DepthSeedItemQuads extends ArrayList<BakedQuad> {
    public DepthSeedItemQuads(Collection<? extends BakedQuad> quads) {
        super(quads);
    }

    public static ItemQuads wrap(ItemQuads quads) {
        // Both render queues must retain the marker after 26.3 splits item geometry.
        return new ItemQuads(new DepthSeedItemQuads(quads.all()),
                new DepthSeedItemQuads(quads.solid()), new DepthSeedItemQuads(quads.translucent()));
    }
}
