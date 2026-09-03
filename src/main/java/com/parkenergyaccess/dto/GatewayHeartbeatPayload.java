package com.parkenergyaccess.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record GatewayHeartbeatPayload(
        @JsonAlias("message_id") String messageId,
        @JsonAlias("gateway_sn") String gatewaySn,
        Long timestamp,
        String type,
        String status,
        List<DeviceHeartbeat> devices,
        @JsonAlias("outbox_pending") Long outboxPending
) {
    public record DeviceHeartbeat(
            @JsonAlias("device_sn") String deviceSn,
            Boolean online,
            @JsonAlias("last_read_time") Long lastReadTime,
            Integer quality
    ) {}
}
