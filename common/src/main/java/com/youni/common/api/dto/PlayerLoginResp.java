package com.youni.common.api.dto;

public class PlayerLoginResp {
    private long playerId;
    private boolean isNewPlayer;
    private boolean hasOfflineMessages;
    private long offlineMessageCount;

    public long getPlayerId() { return playerId; }
    public void setPlayerId(long playerId) { this.playerId = playerId; }
    public boolean isNewPlayer() { return isNewPlayer; }
    public void setNewPlayer(boolean newPlayer) { isNewPlayer = newPlayer; }
    public boolean isHasOfflineMessages() { return hasOfflineMessages; }
    public void setHasOfflineMessages(boolean hasOfflineMessages) { this.hasOfflineMessages = hasOfflineMessages; }
    public long getOfflineMessageCount() { return offlineMessageCount; }
    public void setOfflineMessageCount(long offlineMessageCount) { this.offlineMessageCount = offlineMessageCount; }
}
