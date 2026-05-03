package com.youni.paper.command;

import com.youni.common.YouniCore;
import com.youni.common.platform.Platform;
import com.youni.common.platform.Platform.CommandSender;

import java.util.UUID;

public class PaperCommandHandler {

    private final YouniCore core;
    private final Platform platform;

    public PaperCommandHandler(YouniCore core, Platform platform) {
        this.core = core;
        this.platform = platform;
    }

    public boolean onCommand(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "msg":
            case "message":
                return handleMsg(sender, args);
            case "reply":
            case "r":
                return handleReply(sender, args);
            case "status":
                return handleStatus(sender);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender, label);
                return true;
        }
    }

    private boolean handleMsg(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("[Youni] Usage: /youni msg <player> <message>");
            return true;
        }

        if (sender.getUniqueId() == null) {
            sender.sendMessage("[Youni] Only players can send messages.");
            return true;
        }

        String targetName = args[1];
        String content = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));

        UUID targetUuid = platform.getPlayerUUID(targetName);
        String targetUuidStr;
        if (targetUuid != null) {
            targetUuidStr = targetUuid.toString();
        } else {
            targetUuidStr = targetName;
        }

        String senderUuid = sender.getUniqueId().toString();
        String senderName = sender.getName();

        platform.runTaskAsync(() -> {
            try {
                core.getMessageManager().sendMessage(senderUuid, senderName, targetUuidStr, content);
                sender.sendMessage("[Youni] Message sent to " + targetName);
            } catch (Exception e) {
                sender.sendMessage("[Youni] Failed to send message: " + e.getMessage());
            }
        });

        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("[Youni] Usage: /youni reply <message>");
            return true;
        }

        if (sender.getUniqueId() == null) {
            sender.sendMessage("[Youni] Only players can reply.");
            return true;
        }

        sender.sendMessage("[Youni] Reply functionality requires frontend message tracking.");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("youni.admin")) {
            sender.sendMessage("[Youni] No permission.");
            return true;
        }

        platform.runTaskAsync(() -> {
            sender.sendMessage("[Youni] Status:");
            sender.sendMessage("  Server ID: " + core.getConfig().getServerId());
            sender.sendMessage("  Transport: " + core.getConfig().getTransportMode());
            sender.sendMessage("  Initialized: " + core.isInitialized());
            sender.sendMessage("  Authenticated: " + core.getAuthManager().isServerAuthenticated());
        });

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("youni.admin")) {
            sender.sendMessage("[Youni] No permission.");
            return true;
        }

        sender.sendMessage("[Youni] Reload is not supported. Please restart the server.");
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("[Youni] Commands:");
        sender.sendMessage("  /" + label + " msg <player> <message> - Send cross-server message");
        sender.sendMessage("  /" + label + " reply <message> - Reply to last message");
        sender.sendMessage("  /" + label + " status - Show connection status");
        sender.sendMessage("  /" + label + " reload - Reload configuration");
    }
}
