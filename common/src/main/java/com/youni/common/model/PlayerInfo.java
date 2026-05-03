package com.youni.common.model;

public class PlayerInfo {
    private long id;
    private String uuid;
    private String username;
    private String displayName;
    private String lastServerId;
    private boolean banned;

    public PlayerInfo() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getLastServerId() { return lastServerId; }
    public void setLastServerId(String lastServerId) { this.lastServerId = lastServerId; }
    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
}
