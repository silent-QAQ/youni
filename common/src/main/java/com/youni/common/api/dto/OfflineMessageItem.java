package com.youni.common.api.dto;

public class OfflineMessageItem {
    private String msgId;
    private String senderUuid;
    private String senderName;
    private String content;
    private String createdAt;

    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }
    public String getSenderUuid() { return senderUuid; }
    public void setSenderUuid(String senderUuid) { this.senderUuid = senderUuid; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
