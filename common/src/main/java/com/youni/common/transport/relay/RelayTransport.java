package com.youni.common.transport.relay;

import com.youni.common.api.YouniApiClient;
import com.youni.common.config.YouniConfig;
import com.youni.common.model.ChatMessage;
import com.youni.common.transport.MessageHandler;
import com.youni.common.transport.MessageTransport;

import java.util.logging.Logger;

public class RelayTransport implements MessageTransport {
    private static final Logger LOGGER = Logger.getLogger(RelayTransport.class.getName());

    private final YouniConfig config;
    private final String serverId;
    private final YouniApiClient apiClient;
    private RelayClient client;
    private MessageHandler messageHandler;

    public RelayTransport(YouniConfig config, String serverId, YouniApiClient apiClient) {
        this.config = config;
        this.serverId = serverId;
        this.apiClient = apiClient;
    }

    @Override
    public void start() throws Exception {
        client = new RelayClient(config.getRelay().getUrl(), serverId, "");
        if (messageHandler != null) {
            client.setMessageHandler(messageHandler);
        }
        client.connectBlocking(10, java.util.concurrent.TimeUnit.SECONDS);
        boolean ok = client.waitForRegister(5000);
        if (!ok) {
            throw new RuntimeException("Failed to register with relay server");
        }
        LOGGER.info("[Relay] Transport started, connected to " + config.getRelay().getUrl());
    }

    @Override
    public void stop() throws Exception {
        if (client != null) client.shutdown();
    }

    @Override
    public void send(String targetServerId, ChatMessage message) throws Exception {
        if (!client.isRegistered()) {
            throw new IllegalStateException("Relay client not connected");
        }
        client.sendMessage(targetServerId, message);
        LOGGER.info("[Relay] Sent message to " + targetServerId + " via relay");
    }

    @Override
    public void onMessage(MessageHandler handler) {
        this.messageHandler = handler;
        if (client != null) {
            client.setMessageHandler(handler);
        }
    }

    @Override
    public boolean isConnected(String targetServerId) {
        return client != null && client.isRegistered();
    }

    @Override
    public String getType() {
        return "relay";
    }
}
