package com.example.client.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import net.minecraft.world.entity.Entity;

public class EntityLoadEvent extends EventCancellable {
    private final Entity entity;

    public EntityLoadEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
