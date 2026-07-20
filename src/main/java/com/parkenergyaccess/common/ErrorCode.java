package com.parkenergyaccess.common;

public enum ErrorCode {
    BAD_REQUEST(400),
    GATEWAY_INVALID(1001),
    MQTT_NOT_CONNECTED(1002),
    RABBIT_PUBLISH_FAILED(1003),
    COMMAND_NOT_FOUND(1004);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
