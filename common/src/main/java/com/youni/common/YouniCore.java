package com.youni.common;

import com.google.gson.Gson;
import com.youni.common.api.YouniApiClient;
import com.youni.common.config.YouniConfig;
import com.youni.common.manager.AuthManager;
import com.youni.common.manager.MessageManager;
import com.youni.common.platform.Platform;
import com.youni.common.transport.MessageTransport;
import com.youni.common.transport.TransportManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class YouniCore {
    private static final Logger LOGGER = Logger.getLogger(YouniCore.class.getName());
    private static final Gson GSON = new Gson();

    private final Platform platform;
    private YouniConfig config;
    private YouniApiClient apiClient;
    private AuthManager authManager;
    private TransportManager transportManager;
    private MessageManager messageManager;
    private volatile boolean initialized = false;

    public YouniCore(Platform platform) {
        this.platform = platform;
    }

    public void initialize(Path dataFolder) throws Exception {
        if (initialized) return;

        Files.createDirectories(dataFolder);
        config = loadConfig(dataFolder.resolve("config.json"));

        apiClient = new YouniApiClient(config);
        authManager = new AuthManager(config, apiClient);
        transportManager = new TransportManager(config, apiClient);

        boolean ok = authManager.authenticateServer();
        if (!ok) {
            platform.severe("[Youni] Server authentication failed! Check config.");
            return;
        }

        messageManager = new MessageManager(apiClient, transportManager, config.getServerId());
        transportManager.start();

        initialized = true;
        platform.info("[Youni] Initialized successfully. Server ID: " + config.getServerId()
                + ", Transport: " + config.getTransportMode());
    }

    public void shutdown() {
        if (transportManager != null) {
            try { transportManager.stop(); } catch (Exception ignored) {}
        }
        initialized = false;
        platform.info("[Youni] Shutdown complete.");
    }

    private YouniConfig loadConfig(Path path) throws IOException {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                return GSON.fromJson(reader, YouniConfig.class);
            }
        }
        YouniConfig defaultConfig = new YouniConfig();
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(defaultConfig, writer);
        }
        platform.info("[Youni] Default config created at " + path + ", please edit and restart.");
        return defaultConfig;
    }

    public Platform getPlatform() { return platform; }
    public YouniConfig getConfig() { return config; }
    public YouniApiClient getApiClient() { return apiClient; }
    public AuthManager getAuthManager() { return authManager; }
    public TransportManager getTransportManager() { return transportManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public boolean isInitialized() { return initialized; }
}
