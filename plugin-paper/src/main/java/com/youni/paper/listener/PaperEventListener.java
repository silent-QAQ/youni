package com.youni.paper.listener;

import com.youni.common.YouniCore;
import com.youni.common.api.dto.PlayerLoginResp;
import com.youni.common.api.dto.OfflineMessageItem;
import com.youni.common.platform.Platform;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

public class PaperEventListener implements Listener {

    private final YouniCore core;
    private final Platform platform;

    public PaperEventListener(YouniCore core, Platform platform) {
        this.core = core;
        this.platform = platform;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        platform.runTaskAsync(() -> {
            try {
                PlayerLoginResp resp = core.getAuthManager().playerLogin(uuid.toString(), name);

                if (resp.isHasOfflineMessages()) {
                    platform.runTask(() -> {
                        platform.sendMessage(uuid,
                                "[Youni] You have " + resp.getOfflineMessageCount() + " offline message(s).");
                    });

                    List<OfflineMessageItem> messages = core.getMessageManager().fetchOfflineMessages(uuid.toString());
                    for (OfflineMessageItem msg : messages) {
                        platform.runTask(() -> {
                            platform.sendMessage(uuid,
                                    "[Youni] [Offline] " + msg.getSenderName() + ": " + msg.getContent());
                        });
                    }
                }
            } catch (Exception e) {
                platform.warn("[Youni] Player login failed for " + name + ": " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        platform.runTaskAsync(() -> {
            try {
                core.getAuthManager().playerLogout(uuid.toString());
            } catch (Exception e) {
                platform.warn("[Youni] Player logout failed for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Backend logic only - no frontend handling
        // Chat event is captured for future use (e.g., cross-server channel)
    }
}
