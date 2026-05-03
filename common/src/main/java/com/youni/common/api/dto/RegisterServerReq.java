package com.youni.common.api.dto;

import com.google.gson.JsonObject;

public class RegisterServerReq {
    private String serverName;
    private String serverType;
    private String ownerUuid;
    private String gameAddress;
    private String transportMode;
    private String p2pAddress;
    private int p2pPort;
    private int maxPlayers;

    public RegisterServerReq(String serverName, String serverType, String ownerUuid,
                             String gameAddress, String transportMode,
                             String p2pAddress, int p2pPort, int maxPlayers) {
        this.serverName = serverName;
        this.serverType = serverType;
        this.ownerUuid = ownerUuid;
        this.gameAddress = gameAddress;
        this.transportMode = transportMode;
        this.p2pAddress = p2pAddress;
        this.p2pPort = p2pPort;
        this.maxPlayers = maxPlayers;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("server_name", serverName);
        obj.addProperty("server_type", serverType);
        obj.addProperty("owner_uuid", ownerUuid);
        obj.addProperty("game_address", gameAddress);
        obj.addProperty("transport_mode", transportMode);
        obj.addProperty("p2p_address", p2pAddress);
        obj.addProperty("p2p_port", p2pPort);
        obj.addProperty("max_players", maxPlayers);
        return obj;
    }
}
