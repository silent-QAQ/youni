package com.youni.common.api.dto;

public class RegisterServerResp {
    private String serverId;
    private String serverSecret;

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public String getServerSecret() { return serverSecret; }
    public void setServerSecret(String serverSecret) { this.serverSecret = serverSecret; }
}
