package com.youni.paper.platform;

import com.youni.common.platform.Platform;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PaperPlatform implements Platform {

    private final JavaPlugin plugin;

    public PaperPlatform(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "paper";
    }

    @Override
    public String getVersion() {
        return Bukkit.getVersion();
    }

    @Override
    public void runTask(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runTaskAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runTaskLater(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void broadcastMessage(String message) {
        Bukkit.broadcastMessage(message);
    }

    @Override
    public void sendMessage(UUID playerUuid, String message) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }

    @Override
    public UUID getPlayerUUID(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        return player != null ? player.getUniqueId() : null;
    }

    @Override
    public String getPlayerName(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        return player != null ? player.getName() : null;
    }

    @Override
    public boolean isPlayerOnline(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        return player != null && player.isOnline();
    }

    @Override
    public List<UUID> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toList());
    }

    @Override
    public void registerCommand(String name, String usage, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command != null) {
            command.setExecutor((sender, cmd, label, args) -> {
                CommandSender wrappedSender = new PaperCommandSender(sender);
                return executor.onCommand(wrappedSender, label, args);
            });
            command.setUsage(usage);
        }
    }

    @Override
    public void registerEvent(Object listener) {
        Bukkit.getPluginManager().registerEvents(
                (org.bukkit.event.Listener) listener, plugin);
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void warn(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public void severe(String message) {
        plugin.getLogger().severe(message);
    }

    private static class PaperCommandSender implements CommandSender {
        private final org.bukkit.command.CommandSender sender;

        PaperCommandSender(org.bukkit.command.CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public String getName() {
            return sender.getName();
        }

        @Override
        public UUID getUniqueId() {
            if (sender instanceof Player) {
                return ((Player) sender).getUniqueId();
            }
            return null;
        }

        @Override
        public boolean hasPermission(String permission) {
            return sender.hasPermission(permission);
        }

        @Override
        public void sendMessage(String message) {
            sender.sendMessage(message);
        }
    }
}
