package com.youni.common.transport;

import com.youni.common.api.YouniApiClient;
import com.youni.common.api.dto.DiscoverPlayerResp;
import com.youni.common.config.YouniConfig;
import com.youni.common.model.ChatMessage;
import com.youni.common.transport.p2p.P2PTransport;
import com.youni.common.transport.relay.RelayTransport;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TransportManager implements MessageTransport {
    private static final Logger LOGGER = Logger.getLogger(TransportManager.class.getName());

    private final YouniConfig config;
    private final YouniApiClient apiClient;
    private final String serverId;
    private MessageTransport activeTransport;
    private MessageTransport p2pTransport;
    private MessageTransport relayTransport;
    private MessageHandler messageHandler;
    private final String mode;

    public TransportManager(YouniConfig config, YouniApiClient apiClient) {
        this.config = config;
        this.apiClient = apiClient;
        this.serverId = config.getServerId();
        this.mode = config.getTransportMode();
    }

    @Override
    public void start() throws Exception {
        if (messageHandler != null) {
            if ("p2p".equals(mode) || "auto".equals(mode)) {
                p2pTransport = new P2PTransport(config, serverId);
                p2pTransport.onMessage(messageHandler);
                p2pTransport.start();
                LOGGER.info("[Transport] P2P transport started on port " + config.getP2p().getPort());
            }
            if ("relay".equals(mode) || "auto".equals(mode)) {
                relayTransport = new RelayTransport(config, serverId, apiClient);
                relayTransport.onMessage(messageHandler);
                relayTransport.start();
                LOGGER.info("[Transport] Relay transport connected to " + config.getRelay().getUrl());
            }
        }
        if ("p2p".equals(mode)) {
            activeTransport = p2pTransport;
        } else if ("relay".equals(mode)) {
            activeTransport = relayTransport;
        }
    }

    @Override
    public void stop() throws Exception {
        if (p2pTransport != null) p2pTransport.stop();
        if (relayTransport != null) relayTransport.stop();
    }

    @Override
    public void send(String targetServerId, ChatMessage message) throws Exception {
        if ("auto".equals(mode)) {
            sendAuto(targetServerId, message);
        } else {
            activeTransport.send(targetServerId, message);
        }
    }

    private void sendAuto(String targetServerId, ChatMessage message) throws Exception {
        DiscoverPlayerResp discovery = apiClient.discoverPlayer(message.getReceiverUuid());
        if (!discovery.isOnline()) {
            apiClient.storeOfflineMessage(
                    message.getMsgId(), message.getSenderUuid(), message.getSenderName(),
                    message.getReceiverUuid(), message.getContent()
            );
            LOGGER.info("[Transport] Player offline, message stored as offline message");
            return;
        }

        String targetTransportMode = discovery.getTransportMode();
        if ("p2p".equals(targetTransportMode) && p2pTransport != null) {
            try {
                p2pTransport.send(targetServerId, message);
                return;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[Transport] P2P send failed, falling back to relay", e);
            }
        }

        if (relayTransport != null) {
            relayTransport.send(targetServerId, message);
        } else {
            throw new IllegalStateException("No available transport for server: " + targetServerId);
        }
    }

    @Override
    public void onMessage(MessageHandler handler) {
        this.messageHandler = handler;
    }

    @Override
    public boolean isConnected(String targetServerId) {
        if (p2pTransport != null && p2pTransport.isConnected(targetServerId)) return true;
        if (relayTransport != null && relayTransport.isConnected(targetServerId)) return true;
        return false;
    }

    @Override
    public String getType() {
        return "manager";
    }

    public MessageTransport getP2pTransport() { return p2pTransport; }
    public MessageTransport getRelayTransport() { return relayTransport; }
}
