package com.youni.common.platform;

import java.util.List;
import java.util.UUID;

public interface Platform {
    String getName();
    String getVersion();

    void runTask(Runnable task);
    void runTaskAsync(Runnable task);
    void runTaskLater(Runnable task, long delayTicks);

    void broadcastMessage(String message);
    void sendMessage(UUID playerUuid, String message);

    UUID getPlayerUUID(String playerName);
    String getPlayerName(UUID playerUuid);
    boolean isPlayerOnline(UUID playerUuid);
    List<UUID> getOnlinePlayers();

    void registerCommand(String name, String usage, CommandExecutor executor);
    void registerEvent(Object listener);

    void info(String message);
    void warn(String message);
    void severe(String message);

    @FunctionalInterface
    interface CommandExecutor {
        boolean onCommand(CommandSender sender, String label, String[] args);
    }

    interface CommandSender {
        String getName();
        UUID getUniqueId();
        boolean hasPermission(String permission);
        void sendMessage(String message);
    }
}
