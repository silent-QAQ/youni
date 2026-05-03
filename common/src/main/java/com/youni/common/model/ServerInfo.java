package com.youni.common.model;

public class ServerInfo {
    private String serverId;
    private String serverName;
    private String ownerUuid;
    private String serverType;
    private String gameAddress;
    private String transportMode;
    private String p2pAddress;
    private int p2pPort;
    private String relayUrl;
    private int maxPlayers;
    private boolean online;

    public ServerInfo() {}

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public String getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(String ownerUuid) { this.ownerUuid = ownerUuid; }
    public String getServerType() { return serverType; }
    public void setServerType(String serverType) { this.serverType = serverType; }
    public String getGameAddress() { return gameAddress; }
    public void setGameAddress(String gameAddress) { this.gameAddress = gameAddress; }
    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
    public String getP2pAddress() { return p2pAddress; }
    public void setP2pAddress(String p2pAddress) { this.p2pAddress = p2pAddress; }
    public int getP2pPort() { return p2pPort; }
    public void setP2pPort(int p2pPort) { this.p2pPort = p2pPort; }
    public String getRelayUrl() { return relayUrl; }
    public void setRelayUrl(String relayUrl) { this.relayUrl = relayUrl; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
}
