package com.youni.paper;

import com.youni.common.YouniCore;
import com.youni.common.platform.Platform;
import com.youni.paper.command.PaperCommandHandler;
import com.youni.paper.handler.IncomingMessageHandler;
import com.youni.paper.listener.PaperEventListener;
import com.youni.paper.platform.PaperPlatform;
import org.bukkit.plugin.java.JavaPlugin;

public class YouniPaperPlugin extends JavaPlugin {

    private YouniCore core;
    private Platform platform;

    @Override
    public void onEnable() {
        platform = new PaperPlatform(this);
        core = new YouniCore(platform);

        try {
            core.initialize(getDataFolder().toPath());
        } catch (Exception e) {
            getLogger().severe("[Youni] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        IncomingMessageHandler messageHandler = new IncomingMessageHandler(core, platform);
        core.getMessageManager().setAppMessageHandler(messageHandler);

        platform.registerEvent(new PaperEventListener(core, platform));

        PaperCommandHandler cmdHandler = new PaperCommandHandler(core, platform);
        platform.registerCommand("youni", "/youni <msg|reply|status>", cmdHandler::onCommand);

        getLogger().info("[Youni] Plugin enabled. Server ID: " + core.getConfig().getServerId());
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.shutdown();
        }
        getLogger().info("[Youni] Plugin disabled.");
    }
}
