package com.parkenergyaccess.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record GatewayUploadPayload(
        @JsonAlias("message_id") String messageId,
        @JsonAlias("gateway_sn") String gatewaySn,
        Long timestamp,
        String type,
        List<MeterPayload> meters
) {
}
