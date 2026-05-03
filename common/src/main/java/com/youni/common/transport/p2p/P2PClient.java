package com.youni.common.transport.p2p;

import com.google.gson.JsonObject;
import com.youni.common.model.ChatMessage;
import com.youni.common.protocol.Frame;
import com.youni.common.protocol.FrameType;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PClient extends WebSocketClient {
    private static final Logger LOGGER = Logger.getLogger(P2PClient.class.getName());

    private final String localServerId;
    private final String serverToken;
    private final CompletableFuture<Boolean> handshakeFuture = new CompletableFuture<>();
    private volatile boolean authenticated = false;

    public P2PClient(URI serverUri, String localServerId, String serverToken) {
        super(serverUri);
        this.localServerId = localServerId;
        this.serverToken = serverToken;
        setConnectionLostTimeout(30);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        LOGGER.info("[P2PClient] Connected to " + getURI());
        sendHandshake();
    }

    private void sendHandshake() {
        JsonObject payload = new JsonObject();
        payload.addProperty("server_id", localServerId);
        payload.addProperty("token", serverToken);
        send(Frame.of(FrameType.HANDSHAKE, payload).toJson());
    }

    @Override
    public void onMessage(String message) {
        try {
            Frame frame = Frame.fromJson(message);
            switch (frame.getType()) {
                case FrameType.HANDSHAKE_ACK:
                    boolean success = frame.getPayload().get("success").getAsBoolean();
                    authenticated = success;
                    handshakeFuture.complete(success);
                    LOGGER.info("[P2PClient] Handshake " + (success ? "success" : "failed"));
                    break;
                case FrameType.PONG:
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[P2PClient] Error processing message", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        authenticated = false;
        LOGGER.info("[P2PClient] Disconnected from " + getURI() + " reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.log(Level.WARNING, "[P2PClient] Error connecting to " + getURI(), ex);
        handshakeFuture.complete(false);
    }

    public boolean waitForHandshake(long timeoutMs) throws Exception {
        return handshakeFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public boolean isAuthenticated() {
        return authenticated && isOpen();
    }

    public void sendMessage(ChatMessage message) {
        Frame frame = Frame.of(FrameType.MESSAGE, message.toJson());
        send(frame.toJson());
    }

    public void sendPing() {
        send(Frame.of(FrameType.PING).toJson());
    }
}
