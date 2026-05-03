package com.youni.common.manager;

import com.youni.common.api.YouniApiClient;
import com.youni.common.api.dto.AuthServerResp;
import com.youni.common.api.dto.PlayerLoginResp;
import com.youni.common.api.dto.RegisterServerReq;
import com.youni.common.api.dto.RegisterServerResp;
import com.youni.common.config.YouniConfig;

import java.util.logging.Logger;

public class AuthManager {
    private static final Logger LOGGER = Logger.getLogger(AuthManager.class.getName());

    private final YouniConfig config;
    private final YouniApiClient apiClient;
    private volatile boolean serverAuthenticated = false;

    public AuthManager(YouniConfig config, YouniApiClient apiClient) {
        this.config = config;
        this.apiClient = apiClient;
    }

    public RegisterServerResp registerServer(String serverName, String serverType,
                                              String ownerUuid, String gameAddress) throws Exception {
        RegisterServerReq req = new RegisterServerReq(
                serverName, serverType, ownerUuid, gameAddress,
                config.getTransportMode(),
                config.getP2p().getAddress(), config.getP2p().getPort(),
                20
        );
        RegisterServerResp resp = apiClient.registerServer(req);
        config.setServerId(resp.getServerId());
        config.setServerSecret(resp.getServerSecret());
        LOGGER.info("[Auth] Server registered: " + resp.getServerId());
        return resp;
    }

    public boolean authenticateServer() throws Exception {
        if (config.getServerId().isEmpty() || config.getServerSecret().isEmpty()) {
            LOGGER.warning("[Auth] Server ID or Secret not configured");
            return false;
        }
        AuthServerResp resp = apiClient.authServer(config.getServerId(), config.getServerSecret());
        apiClient.setAccessToken(resp.getAccessToken());
        serverAuthenticated = true;
        LOGGER.info("[Auth] Server authenticated, token expires in " + resp.getExpiresIn() + "s");
        return true;
    }

    public PlayerLoginResp playerLogin(String uuid, String username) throws Exception {
        if (!serverAuthenticated) {
            throw new IllegalStateException("Server not authenticated");
        }
        PlayerLoginResp resp = apiClient.playerLogin(uuid, username);
        LOGGER.info("[Auth] Player login: " + username + " (" + uuid + ") new=" + resp.isNewPlayer());
        return resp;
    }

    public void playerLogout(String uuid) throws Exception {
        apiClient.playerLogout(uuid);
        LOGGER.info("[Auth] Player logout: " + uuid);
    }

    public boolean isServerAuthenticated() {
        return serverAuthenticated;
    }
}
