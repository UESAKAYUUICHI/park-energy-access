package com.parkenergyaccess.mq.message;

import java.time.Instant;

public record AccessForwardMessage(
        long rawLogId,
        String messageId,
        Long gatewayId,
        String gatewaySn,
        String payloadType,
        String rawPayload,
        Instant receivedAt
) {
}
