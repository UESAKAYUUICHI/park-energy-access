package com.parkenergyaccess.vo;

import java.time.Instant;

public record GatewayStatusVO(Long gatewayId, String topicGatewayId, String gatewaySn, String status,
                              Instant lastSeenTime) {
}
