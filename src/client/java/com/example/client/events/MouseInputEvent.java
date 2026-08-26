package com.example.client.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Getter;

@Getter
public class MouseInputEvent extends EventCancellable {
    private final int button;
    private final int action;
    private final int modifiers;

    public MouseInputEvent(int button, int action, int modifiers) {
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
    }
}
