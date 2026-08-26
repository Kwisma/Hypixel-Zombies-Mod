package com.example.client.module.modules;

import com.darkmagician6.eventapi.EventTarget;
import com.example.client.events.ChatEvent;
import com.example.client.events.TickEvent;
import com.example.client.integration.skillshare.SkillShareIntegration;
import com.example.client.language.Language;
import com.example.client.language.Text;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * SkillShare-MICx v1 protocol integration.
 *
 * <p>The module is opt-in because joining a room sends the player's name,
 * Zombies roster, map, round, position and skill cooldown to the configured
 * SkillShare server.</p>
 */
@ModuleInfo(name = {
        @Text(label = "Skill Share", language = Language.English),
        @Text(label = "技能共享", language = Language.Chinese)
}, enable = false)
public class SkillShare extends AbstractModule {

    private final SkillShareIntegration integration = new SkillShareIntegration();

    public SkillShare() {
        // FabricEvents intentionally skips TickEvent when no world is loaded,
        // so close the external socket explicitly when leaving a server.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> integration.onGameDisconnect());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        integration.tick();
    }

    @EventTarget
    public void onChat(ChatEvent event) {
        integration.onChat(event.getComponent().getString());
    }

    @Override
    protected void onEnable() {
        // AbstractModule invokes hooks from its constructor, before subclass
        // fields are initialized. The first real user toggle sees a non-null value.
        if (integration != null) {
            integration.start();
        }
    }

    @Override
    protected void onDisable() {
        if (integration != null) {
            integration.stop();
        }
    }
}
