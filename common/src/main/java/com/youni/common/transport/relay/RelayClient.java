package com.youni.common.transport.relay;

import com.google.gson.JsonObject;
import com.youni.common.model.ChatMessage;
import com.youni.common.protocol.Frame;
import com.youni.common.protocol.FrameType;
import com.youni.common.transport.MessageHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RelayClient extends WebSocketClient {
    private static final Logger LOGGER = Logger.getLogger(RelayClient.class.getName());
    private static final long RECONNECT_DELAY_MS = 5000;
    private static final long MAX_RECONNECT_DELAY_MS = 60000;
    private static final long PING_INTERVAL_MS = 25000;

    private final String localServerId;
    private final String serverToken;
    private final String relayUrl;
    private final CompletableFuture<Boolean> registerFuture = new CompletableFuture<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "youni-relay-heartbeat");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean registered = false;
    private volatile boolean shouldReconnect = true;
    private long reconnectDelay = RECONNECT_DELAY_MS;
    private MessageHandler messageHandler;

    public RelayClient(String relayUrl, String localServerId, String serverToken) {
        super(URI.create(relayUrl));
        this.relayUrl = relayUrl;
        this.localServerId = localServerId;
        this.serverToken = serverToken;
        setConnectionLostTimeout(30);
    }

    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        LOGGER.info("[Relay] Connected to relay server");
        reconnectDelay = RECONNECT_DELAY_MS;
        sendRegister();
    }

    private void sendRegister() {
        JsonObject payload = new JsonObject();
        payload.addProperty("server_id", localServerId);
        payload.addProperty("token", serverToken);
        send(Frame.of(FrameType.REGISTER, payload).toJson());
    }

    @Override
    public void onMessage(String message) {
        try {
            Frame frame = Frame.fromJson(message);
            switch (frame.getType()) {
                case FrameType.REGISTER_ACK:
                    boolean success = frame.getPayload().get("success").getAsBoolean();
                    registered = success;
                    registerFuture.complete(success);
                    if (success) {
                        startPing();
                        LOGGER.info("[Relay] Registered successfully");
                    } else {
                        LOGGER.warning("[Relay] Registration failed");
                    }
                    break;
                case FrameType.MESSAGE:
                    handleIncomingMessage(frame);
                    break;
                case FrameType.MESSAGE_ACK:
                    break;
                case FrameType.PONG:
                    break;
                default:
                    LOGGER.warning("[Relay] Unknown frame type: " + frame.getType());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[Relay] Error processing message", e);
        }
    }

    private void handleIncomingMessage(Frame frame) {
        JsonObject payload = frame.getPayload();
        ChatMessage chatMsg = ChatMessage.fromJson(payload);
        if (messageHandler != null) {
            messageHandler.onMessage(chatMsg);
        }

        JsonObject ack = new JsonObject();
        ack.addProperty("msg_id", chatMsg.getMsgId());
        ack.addProperty("delivered", true);
        send(Frame.of(FrameType.MESSAGE_ACK, ack).toJson());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        registered = false;
        scheduler.shutdownNow();
        LOGGER.info("[Relay] Disconnected from relay server, reason: " + reason);
        if (shouldReconnect && remote) {
            scheduleReconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.log(Level.WARNING, "[Relay] Error", ex);
        registerFuture.complete(false);
    }

    private void startPing() {
        ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "youni-relay-ping");
            t.setDaemon(true);
            return t;
        });
        pingScheduler.scheduleAtFixedRate(() -> {
            if (isOpen()) {
                send(Frame.of(FrameType.PING).toJson());
            }
        }, PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                LOGGER.info("[Relay] Reconnecting in " + reconnectDelay + "ms...");
                Thread.sleep(reconnectDelay);
                reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY_MS);
                reconnect();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "[Relay] Reconnect failed", e);
            }
        }, "youni-relay-reconnect").start();
    }

    public void sendMessage(String targetServerId, ChatMessage message) {
        Frame frame = Frame.of(FrameType.MESSAGE, message.toJson());
        send(frame.toJson());
    }

    public boolean waitForRegister(long timeoutMs) throws Exception {
        return registerFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public boolean isRegistered() {
        return registered && isOpen();
    }

    public void shutdown() {
        shouldReconnect = false;
        scheduler.shutdownNow();
        close();
    }
}
