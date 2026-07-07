package com.example.client.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import com.example.client.utils.record.HitResult;
import lombok.Getter;
import net.minecraft.network.chat.Component;

@Getter
public class ChatEvent extends EventCancellable {
    private final Component component;
    private final HitResult hitResult;

    public ChatEvent(Component component) {
        this(component, null);
    }

    public ChatEvent(Component component, HitResult hitResult) {
        this.component = component;
        this.hitResult = hitResult;
    }
}
