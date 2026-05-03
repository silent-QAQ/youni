package com.youni.common.transport.p2p;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PPool {
    private static final Logger LOGGER = Logger.getLogger(P2PPool.class.getName());
    private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000;
    private static final long CLEANUP_INTERVAL_MS = 60 * 1000;

    private final String localServerId;
    private final String serverToken;
    private final Map<String, PoolEntry> connections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "youni-p2p-pool");
        t.setDaemon(true);
        return t;
    });

    private static class PoolEntry {
        P2PClient client;
        long lastUsed;

        PoolEntry(P2PClient client) {
            this.client = client;
            this.lastUsed = System.currentTimeMillis();
        }
    }

    public P2PPool(String localServerId, String serverToken) {
        this.localServerId = localServerId;
        this.serverToken = serverToken;
        scheduler.scheduleAtFixedRate(this::cleanup, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public P2PClient getOrCreate(String targetServerId, String host, int port) throws Exception {
        PoolEntry entry = connections.get(targetServerId);
        if (entry != null && entry.client.isAuthenticated()) {
            entry.lastUsed = System.currentTimeMillis();
            return entry.client;
        }

        P2PClient client = new P2PClient(
                URI.create("ws://" + host + ":" + port),
                localServerId,
                serverToken
        );
        client.connectBlocking(10, TimeUnit.SECONDS);
        boolean ok = client.waitForHandshake(5000);
        if (!ok) {
            client.close();
            throw new RuntimeException("Handshake failed with " + targetServerId);
        }

        connections.put(targetServerId, new PoolEntry(client));
        LOGGER.info("[P2PPool] Created connection to " + targetServerId + " at " + host + ":" + port);
        return client;
    }

    public boolean isConnected(String targetServerId) {
        PoolEntry entry = connections.get(targetServerId);
        return entry != null && entry.client.isAuthenticated();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        connections.entrySet().removeIf(e -> {
            PoolEntry entry = e.getValue();
            if (!entry.client.isOpen() || (now - entry.lastUsed > IDLE_TIMEOUT_MS)) {
                try { entry.client.close(); } catch (Exception ignored) {}
                LOGGER.info("[P2PPool] Removed idle connection to " + e.getKey());
                return true;
            }
            return false;
        });
    }

    public void shutdown() {
        scheduler.shutdown();
        connections.forEach((id, entry) -> {
            try { entry.client.close(); } catch (Exception ignored) {}
        });
        connections.clear();
    }
}
