package com.youni.common.transport.p2p;

import com.youni.common.api.dto.DiscoverPlayerResp;
import com.youni.common.config.YouniConfig;
import com.youni.common.model.ChatMessage;
import com.youni.common.transport.MessageHandler;
import com.youni.common.transport.MessageTransport;

import java.util.logging.Logger;

public class P2PTransport implements MessageTransport {
    private static final Logger LOGGER = Logger.getLogger(P2PTransport.class.getName());

    private final YouniConfig config;
    private final String serverId;
    private P2PServer server;
    private P2PPool pool;
    private MessageHandler messageHandler;

    public P2PTransport(YouniConfig config, String serverId) {
        this.config = config;
        this.serverId = serverId;
    }

    @Override
    public void start() throws Exception {
        String host = config.getP2p().getAddress();
        int port = config.getP2p().getPort();

        server = new P2PServer(host, port, serverId, "");
        if (messageHandler != null) {
            server.setMessageHandler(messageHandler);
        }
        server.start();

        pool = new P2PPool(serverId, "");
        LOGGER.info("[P2P] Transport started, listening on " + host + ":" + port);
    }

    @Override
    public void stop() throws Exception {
        if (pool != null) pool.shutdown();
        if (server != null) server.stop(1000);
    }

    @Override
    public void send(String targetServerId, ChatMessage message) throws Exception {
        P2PClient client = pool.getOrCreate(
                targetServerId,
                message.getTargetServer(),
                config.getP2p().getPort()
        );
        client.sendMessage(message);
        LOGGER.info("[P2P] Sent message to " + targetServerId);
    }

    @Override
    public void onMessage(MessageHandler handler) {
        this.messageHandler = handler;
        if (server != null) {
            server.setMessageHandler(handler);
        }
    }

    @Override
    public boolean isConnected(String targetServerId) {
        return server != null && server.isPeerConnected(targetServerId)
                || pool != null && pool.isConnected(targetServerId);
    }

    @Override
    public String getType() {
        return "p2p";
    }
}
