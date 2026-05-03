package com.youni.common.manager;

import com.youni.common.api.YouniApiClient;
import com.youni.common.api.dto.DiscoverPlayerResp;
import com.youni.common.api.dto.OfflineMessageItem;
import com.youni.common.model.ChatMessage;
import com.youni.common.transport.MessageHandler;
import com.youni.common.transport.MessageTransport;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageManager {
    private static final Logger LOGGER = Logger.getLogger(MessageManager.class.getName());

    private final YouniApiClient apiClient;
    private final MessageTransport transport;
    private final String localServerId;
    private MessageHandler appMessageHandler;

    public MessageManager(YouniApiClient apiClient, MessageTransport transport, String localServerId) {
        this.apiClient = apiClient;
        this.transport = transport;
        this.localServerId = localServerId;

        transport.onMessage(this::handleIncomingMessage);
    }

    public void setAppMessageHandler(MessageHandler handler) {
        this.appMessageHandler = handler;
    }

    public void sendMessage(String senderUuid, String senderName,
                            String receiverUuid, String content) throws Exception {
        DiscoverPlayerResp discovery = apiClient.discoverPlayer(receiverUuid);

        ChatMessage msg = new ChatMessage(
                localServerId, senderUuid, senderName,
                discovery.getServerId(), receiverUuid, content
        );

        if (!discovery.isOnline()) {
            apiClient.storeOfflineMessage(msg.getMsgId(), senderUuid, senderName, receiverUuid, content);
            LOGGER.info("[Message] Player offline, stored as offline message");
            return;
        }

        transport.send(discovery.getServerId(), msg);
        LOGGER.info("[Message] Sent message from " + senderName + " to " + receiverUuid);
    }

    public List<OfflineMessageItem> fetchOfflineMessages(String playerUuid) throws Exception {
        return apiClient.getOfflineMessages(playerUuid);
    }

    private void handleIncomingMessage(ChatMessage message) {
        LOGGER.info("[Message] Received message from " + message.getSenderName()
                + " (" + message.getSenderUuid() + "): " + message.getContent());
        if (appMessageHandler != null) {
            appMessageHandler.onMessage(message);
        }
    }
}
