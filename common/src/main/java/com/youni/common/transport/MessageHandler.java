package com.youni.common.transport;

import com.youni.common.model.ChatMessage;

@FunctionalInterface
public interface MessageHandler {
    void onMessage(ChatMessage message);
}
