package com.youni.common.config;

public class YouniConfig {
    private String centralUrl = "http://localhost:8080";
    private String serverId = "";
    private String serverSecret = "";
    private String transportMode = "auto";

    private P2PConfig p2p = new P2PConfig();
    private RelayConfig relay = new RelayConfig();

    public static class P2PConfig {
        private String address = "0.0.0.0";
        private int port = 9876;

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

    public static class RelayConfig {
        private String url = "ws://localhost:9877";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public String getCentralUrl() { return centralUrl; }
    public void setCentralUrl(String centralUrl) { this.centralUrl = centralUrl; }
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public String getServerSecret() { return serverSecret; }
    public void setServerSecret(String serverSecret) { this.serverSecret = serverSecret; }
    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
    public P2PConfig getP2p() { return p2p; }
    public void setP2p(P2PConfig p2p) { this.p2p = p2p; }
    public RelayConfig getRelay() { return relay; }
    public void setRelay(RelayConfig relay) { this.relay = relay; }
}
