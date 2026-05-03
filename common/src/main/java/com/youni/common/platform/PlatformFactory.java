package com.youni.common.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class PlatformFactory {
    private static final Map<String, Platform> platforms = new HashMap<>();

    public static void register(Platform platform) {
        platforms.put(platform.getName().toLowerCase(), platform);
    }

    public static Platform get(String name) {
        return platforms.get(name.toLowerCase());
    }

    public static Platform detect() {
        try {
            Class.forName("org.bukkit.Bukkit");
            return platforms.get("paper");
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return platforms.get("fabric");
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("net.neoforged.neoforge.common.NeoForge");
            return platforms.get("neoforge");
        } catch (ClassNotFoundException ignored) {}

        try {
            Class.forName("net.minecraftforge.common.MinecraftForge");
            return platforms.get("forge");
        } catch (ClassNotFoundException ignored) {}

        return null;
    }
}
