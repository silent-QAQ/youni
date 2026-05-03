package com.youni.common.protocol;

public final class FrameType {
    public static final String HANDSHAKE = "handshake";
    public static final String HANDSHAKE_ACK = "handshake_ack";
    public static final String REGISTER = "register";
    public static final String REGISTER_ACK = "register_ack";
    public static final String MESSAGE = "message";
    public static final String MESSAGE_ACK = "message_ack";
    public static final String PING = "ping";
    public static final String PONG = "pong";
    public static final String ERROR = "error";

    private FrameType() {}
}
