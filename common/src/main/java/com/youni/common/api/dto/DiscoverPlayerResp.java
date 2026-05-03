package com.youni.common.api.dto;

public class DiscoverPlayerResp {
    private boolean online;
    private String serverId;
    private String serverName;
    private String transportMode;
    private String p2pAddress;
    private int p2pPort;

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
    public String getP2pAddress() { return p2pAddress; }
    public void setP2pAddress(String p2pAddress) { this.p2pAddress = p2pAddress; }
    public int getP2pPort() { return p2pPort; }
    public void setP2pPort(int p2pPort) { this.p2pPort = p2pPort; }
}
