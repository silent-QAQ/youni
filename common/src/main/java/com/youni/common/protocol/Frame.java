package com.youni.common.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Frame {
    private static final Gson GSON = new Gson();

    private String type;
    private JsonObject payload;

    public Frame() {}

    public Frame(String type, JsonObject payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public JsonObject getPayload() { return payload; }
    public void setPayload(JsonObject payload) { this.payload = payload; }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static Frame fromJson(String json) {
        return GSON.fromJson(json, Frame.class);
    }

    public static Frame of(String type, JsonObject payload) {
        return new Frame(type, payload);
    }

    public static Frame of(String type) {
        return new Frame(type, new JsonObject());
    }
}
