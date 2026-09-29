package com.example.test;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.events.Event;
import com.example.client.setting.Setting;
import com.example.client.setting.SettingManager;
import com.example.client.setting.annotation.SettingInfo;

/** Checks reflective access to both static and instance members on supported JDKs. */
final class ReflectionCompatibilityCheck {
    private ReflectionCompatibilityCheck() {
    }

    static void run() throws IllegalAccessException {
        Listener listener = new Listener();
        try {
            EventManager.register(listener);
            checkDispatch(15);
            EventManager.unregister(listener);
            checkDispatch(0);

            EventManager.register(listener, TestEvent.class);
            checkDispatch(15);
            EventManager.unregister(listener, TestEvent.class);
            checkDispatch(0);
        } finally {
            EventManager.unregister(listener);
        }

        SettingsFixture fixture = new SettingsFixture();
        try {
            fixture.register();
            check("test.instance".equals(fixture.instanceSetting.getNameKey()),
                    "Private instance setting must receive its name");
            check("test.static".equals(SettingsFixture.staticSetting.getNameKey()),
                    "Private static setting must receive its name");
            check(SettingManager.getSettings(fixture).size() == 2,
                    "Both settings must remain registered");
        } finally {
            SettingManager.removeObj(fixture);
        }
    }

    private static void checkDispatch(int expected) {
        TestEvent event = new TestEvent();
        check(EventManager.call(event) == event, "Dispatch must return the original event");
        check(event.calls == expected, "Expected listener mask " + expected + ", got " + event.calls);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static final class TestEvent implements Event {
        private int calls;
    }

    public static final class Listener {
        @EventTarget
        private void privateInstance(TestEvent event) {
            event.calls += 1;
        }

        @EventTarget
        private static void privateStatic(TestEvent event) {
            event.calls += 2;
        }

        @EventTarget
        public void publicInstance(TestEvent event) {
            event.calls += 4;
        }

        @EventTarget
        public static void publicStatic(TestEvent event) {
            event.calls += 8;
        }

        @EventTarget
        private void invalidParameter(String value) {
            throw new AssertionError("Non-event listeners must be ignored");
        }

        @EventTarget
        private void missingParameter() {
            throw new AssertionError("Listeners without an event must be ignored");
        }

        @EventTarget
        private void extraParameter(TestEvent event, String value) {
            throw new AssertionError("Listeners with extra parameters must be ignored");
        }
    }

    private static final class SettingsFixture extends SettingManager {
        @SettingInfo(name = "test.instance")
        private final Setting<Boolean> instanceSetting = new Setting<>(true) {};

        @SettingInfo(name = "test.static")
        private static final Setting<Boolean> staticSetting = new Setting<>(false) {};

        private void register() throws IllegalAccessException {
            registerSetting(this, instanceSetting, staticSetting);
        }
    }
}
