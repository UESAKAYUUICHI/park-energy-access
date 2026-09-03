package com.parkenergyaccess.vo;

import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;

import java.time.Instant;

/** Full raw-message view for authorized onboarding and diagnostics workflows. */
public record RawMessageDetailVO(long id, Long gatewayId, String gatewaySn, String messageId, String topic,
                                 MqttMessageType messageType, RawMessageStatus status, String failCode, String failReason,
                                 Instant receiveTime, String payload) {
}
