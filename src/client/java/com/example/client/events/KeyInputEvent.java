package com.example.client.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Getter;

@Getter
public class KeyInputEvent extends EventCancellable {
    private final int key;
    private final int keyCode;
    private final int action;
    private final int modifiers;

    public KeyInputEvent(int key, int keyCode, int action, int modifiers) {
        this.key = key;
        this.keyCode = keyCode;
        this.action = action;
        this.modifiers = modifiers;
    }
}
