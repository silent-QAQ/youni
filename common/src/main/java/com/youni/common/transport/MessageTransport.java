package com.youni.common.transport;

import com.youni.common.model.ChatMessage;

public interface MessageTransport {
    void start() throws Exception;
    void stop() throws Exception;
    void send(String targetServerId, ChatMessage message) throws Exception;
    void onMessage(MessageHandler handler);
    boolean isConnected(String targetServerId);
    String getType();
}
