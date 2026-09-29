package com.example.client.module.modules;

import com.example.client.ZombiesModClient;
import com.example.client.module.AbstractModule;
import com.example.client.module.annotation.ModuleInfo;

@ModuleInfo(name = "module.sidebar_modification", enable = true)
public class SidebarModification extends AbstractModule {
    public static boolean isActive() {
        if (ZombiesModClient.moduleManager == null) return false;
        AbstractModule module = ZombiesModClient.moduleManager.getModule("module.sidebar_modification");
        return module != null && module.isEnable();
    }
}
