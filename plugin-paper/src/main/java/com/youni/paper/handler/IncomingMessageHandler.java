package com.youni.paper.handler;

import com.youni.common.YouniCore;
import com.youni.common.model.ChatMessage;
import com.youni.common.platform.Platform;
import com.youni.common.transport.MessageHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IncomingMessageHandler implements MessageHandler {

    private final YouniCore core;
    private final Platform platform;

    private final Map<UUID, ChatMessage> lastMessages = new ConcurrentHashMap<>();

    public IncomingMessageHandler(YouniCore core, Platform platform) {
        this.core = core;
        this.platform = platform;
    }

    @Override
    public void onMessage(ChatMessage message) {
        lastMessages.put(UUID.fromString(message.getReceiverUuid()), message);

        UUID receiverUuid = UUID.fromString(message.getReceiverUuid());
        boolean online = platform.isPlayerOnline(receiverUuid);

        if (online) {
            deliverMessage(message);
        } else {
            storeOffline(message);
        }
    }

    private void deliverMessage(ChatMessage message) {
        UUID receiverUuid = UUID.fromString(message.getReceiverUuid());

        platform.runTask(() -> {
            String formatted = formatIncomingMessage(message);
            platform.sendMessage(receiverUuid, formatted);
        });
    }

    private void storeOffline(ChatMessage message) {
        platform.runTaskAsync(() -> {
            try {
                core.getApiClient().storeOfflineMessage(
                        message.getMsgId(),
                        message.getSenderUuid(),
                        message.getSenderName(),
                        message.getReceiverUuid(),
                        message.getContent()
                );
            } catch (Exception e) {
                platform.warn("[Youni] Failed to store offline message: " + e.getMessage());
            }
        });
    }

    private String formatIncomingMessage(ChatMessage message) {
        return "[Youni] " + message.getSenderName() + ": " + message.getContent();
    }

    public ChatMessage getLastMessage(UUID playerUuid) {
        return lastMessages.get(playerUuid);
    }

    public void clearLastMessage(UUID playerUuid) {
        lastMessages.remove(playerUuid);
    }
}
