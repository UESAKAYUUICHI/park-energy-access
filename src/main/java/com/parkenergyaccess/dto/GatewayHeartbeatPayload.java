package com.parkenergyaccess.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GatewayHeartbeatPayload(
        @JsonAlias("message_id") String messageId,
        @JsonAlias("gateway_sn") String gatewaySn,
        Long timestamp,
        String type,
        String status
) {
}
