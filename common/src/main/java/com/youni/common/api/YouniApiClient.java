package com.youni.common.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.youni.common.api.dto.*;
import com.youni.common.api.exception.ApiException;
import com.youni.common.config.YouniConfig;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class YouniApiClient {
    private static final Gson GSON = new Gson();

    private final String baseUrl;
    private String accessToken;

    public YouniApiClient(YouniConfig config) {
        this.baseUrl = config.getCentralUrl();
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    public RegisterServerResp registerServer(RegisterServerReq req) throws Exception {
        String body = GSON.toJson(req);
        String resp = post("/api/v1/server/register", body, null);
        return parseData(resp, RegisterServerResp.class);
    }

    public AuthServerResp authServer(String serverId, String serverSecret) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("server_id", serverId);
        obj.addProperty("server_secret", serverSecret);
        String resp = post("/api/v1/server/auth", GSON.toJson(obj), null);
        AuthServerResp authResp = parseData(resp, AuthServerResp.class);
        this.accessToken = authResp.getAccessToken();
        return authResp;
    }

    public PlayerLoginResp playerLogin(String uuid, String username) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", uuid);
        obj.addProperty("username", username);
        String resp = post("/api/v1/player/login", GSON.toJson(obj), accessToken);
        return parseData(resp, PlayerLoginResp.class);
    }

    public void playerLogout(String uuid) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", uuid);
        post("/api/v1/player/logout", GSON.toJson(obj), accessToken);
    }

    public DiscoverPlayerResp discoverPlayer(String uuid) throws Exception {
        String resp = get("/api/v1/discovery/player/" + uuid, accessToken);
        return parseData(resp, DiscoverPlayerResp.class);
    }

    public void storeOfflineMessage(String msgId, String senderUuid, String senderName,
                                    String receiverUuid, String content) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("msg_id", msgId);
        obj.addProperty("sender_uuid", senderUuid);
        obj.addProperty("sender_name", senderName);
        obj.addProperty("receiver_uuid", receiverUuid);
        obj.addProperty("content", content);
        post("/api/v1/discovery/offline-message", GSON.toJson(obj), accessToken);
    }

    public List<OfflineMessageItem> getOfflineMessages(String playerUuid) throws Exception {
        String resp = get("/api/v1/discovery/offline-messages/" + playerUuid, accessToken);
        JsonObject obj = GSON.fromJson(resp, JsonObject.class);
        JsonObject data = obj.getAsJsonObject("data");
        Type listType = new TypeToken<List<OfflineMessageItem>>() {}.getType();
        return GSON.fromJson(data.getAsJsonArray("messages"), listType);
    }

    public void heartbeat(int onlineCount, List<String> playerList) throws Exception {
        JsonObject obj = new JsonObject();
        obj.addProperty("online_count", onlineCount);
        obj.add("player_list", GSON.toJsonTree(playerList));
        post("/api/v1/server/heartbeat", GSON.toJson(obj), accessToken);
    }

    private String get(String path, String token) throws Exception {
        HttpURLConnection conn = openConnection(path, "GET", token);
        int code = conn.getResponseCode();
        String body = readResponseBody(conn);
        if (code >= 400) {
            throwApiException(code, body);
        }
        return body;
    }

    private String post(String path, String jsonBody, String token) throws Exception {
        HttpURLConnection conn = openConnection(path, "POST", token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();
        int code = conn.getResponseCode();
        String body = readResponseBody(conn);
        if (code >= 400) {
            throwApiException(code, body);
        }
        return body;
    }

    private HttpURLConnection openConnection(String path, String method, String token) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        return conn;
    }

    private String readResponseBody(HttpURLConnection conn) throws IOException {
        InputStream is;
        if (conn.getResponseCode() >= 400) {
            is = conn.getErrorStream();
        } else {
            is = conn.getInputStream();
        }
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private void throwApiException(int code, String body) throws ApiException {
        String msg = "unknown error";
        try {
            JsonObject obj = GSON.fromJson(body, JsonObject.class);
            if (obj != null && obj.has("msg")) {
                msg = obj.get("msg").getAsString();
            }
        } catch (Exception ignored) {
        }
        throw new ApiException(code, msg);
    }

    private <T> T parseData(String json, Class<T> clazz) {
        JsonObject obj = GSON.fromJson(json, JsonObject.class);
        return GSON.fromJson(obj.get("data"), clazz);
    }
}
