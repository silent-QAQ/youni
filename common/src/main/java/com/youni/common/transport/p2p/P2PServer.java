package com.youni.common.transport.p2p;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.youni.common.model.ChatMessage;
import com.youni.common.protocol.Frame;
import com.youni.common.protocol.FrameType;
import com.youni.common.transport.MessageHandler;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PServer extends WebSocketServer {
    private static final Logger LOGGER = Logger.getLogger(P2PServer.class.getName());
    private static final Gson GSON = new Gson();

    private final String localServerId;
    private final String serverToken;
    private final Map<String, WebSocket> authenticatedPeers = new ConcurrentHashMap<>();
    private MessageHandler messageHandler;

    public P2PServer(String host, int port, String localServerId, String serverToken) {
        super(new InetSocketAddress(host, port));
        this.localServerId = localServerId;
        this.serverToken = serverToken;
        setReuseAddr(true);
    }

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("[P2PServer] New connection from " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Frame frame = Frame.fromJson(message);
            switch (frame.getType()) {
                case FrameType.HANDSHAKE:
                    handleHandshake(conn, frame);
                    break;
                case FrameType.MESSAGE:
                    handleMessage(conn, frame);
                    break;
                case FrameType.MESSAGE_ACK:
                    break;
                case FrameType.PING:
                    conn.send(Frame.of(FrameType.PONG).toJson());
                    break;
                default:
                    LOGGER.warning("[P2PServer] Unknown frame type: " + frame.getType());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[P2PServer] Error processing message", e);
        }
    }

    private void handleHandshake(WebSocket conn, Frame frame) {
        JsonObject payload = frame.getPayload();
        String peerServerId = payload.get("server_id").getAsString();
        String token = payload.get("token").getAsString();

        // TODO: validate JWT token with backend or local cache
        authenticatedPeers.put(peerServerId, conn);
        LOGGER.info("[P2PServer] Peer authenticated: " + peerServerId);

        JsonObject ackPayload = new JsonObject();
        ackPayload.addProperty("success", true);
        ackPayload.addProperty("server_id", localServerId);
        conn.send(Frame.of(FrameType.HANDSHAKE_ACK, ackPayload).toJson());
    }

    private void handleMessage(WebSocket conn, Frame frame) {
        JsonObject payload = frame.getPayload();
        String targetServer = payload.get("target_server").getAsString();

        if (!localServerId.equals(targetServer)) {
            LOGGER.warning("[P2PServer] Message not for this server, ignoring");
            return;
        }

        ChatMessage chatMsg = ChatMessage.fromJson(payload);
        if (messageHandler != null) {
            messageHandler.onMessage(chatMsg);
        }

        JsonObject ack = new JsonObject();
        ack.addProperty("msg_id", chatMsg.getMsgId());
        ack.addProperty("delivered", true);
        conn.send(Frame.of(FrameType.MESSAGE_ACK, ack).toJson());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        authenticatedPeers.values().remove(conn);
        LOGGER.info("[P2PServer] Connection closed: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.log(Level.WARNING, "[P2PServer] Error", ex);
    }

    @Override
    public void onStart() {
        LOGGER.info("[P2PServer] Started on " + getAddress());
    }

    public boolean isPeerConnected(String serverId) {
        WebSocket conn = authenticatedPeers.get(serverId);
        return conn != null && conn.isOpen();
    }
}
