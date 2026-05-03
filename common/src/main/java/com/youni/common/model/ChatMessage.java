package com.youni.common.model;

import com.google.gson.JsonObject;

public class ChatMessage {
    private String msgId;
    private String senderServer;
    private String senderUuid;
    private String senderName;
    private String targetServer;
    private String receiverUuid;
    private String content;
    private long timestamp;

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    public ChatMessage(String senderServer, String senderUuid, String senderName,
                       String targetServer, String receiverUuid, String content) {
        this.msgId = java.util.UUID.randomUUID().toString();
        this.senderServer = senderServer;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.targetServer = targetServer;
        this.receiverUuid = receiverUuid;
        this.content = content;
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }
    public String getSenderServer() { return senderServer; }
    public void setSenderServer(String senderServer) { this.senderServer = senderServer; }
    public String getSenderUuid() { return senderUuid; }
    public void setSenderUuid(String senderUuid) { this.senderUuid = senderUuid; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getTargetServer() { return targetServer; }
    public void setTargetServer(String targetServer) { this.targetServer = targetServer; }
    public String getReceiverUuid() { return receiverUuid; }
    public void setReceiverUuid(String receiverUuid) { this.receiverUuid = receiverUuid; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("msg_id", msgId);
        obj.addProperty("sender_server", senderServer);
        obj.addProperty("sender_uuid", senderUuid);
        obj.addProperty("sender_name", senderName);
        obj.addProperty("target_server", targetServer);
        obj.addProperty("receiver_uuid", receiverUuid);
        obj.addProperty("content", content);
        obj.addProperty("timestamp", timestamp);
        return obj;
    }

    public static ChatMessage fromJson(JsonObject obj) {
        ChatMessage msg = new ChatMessage();
        msg.msgId = obj.get("msg_id").getAsString();
        msg.senderServer = obj.get("sender_server").getAsString();
        msg.senderUuid = obj.get("sender_uuid").getAsString();
        msg.senderName = obj.get("sender_name").getAsString();
        msg.targetServer = obj.get("target_server").getAsString();
        msg.receiverUuid = obj.get("receiver_uuid").getAsString();
        msg.content = obj.get("content").getAsString();
        msg.timestamp = obj.get("timestamp").getAsLong();
        return msg;
    }
}
