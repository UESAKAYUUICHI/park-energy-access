package com.parkenergyaccess.entity;

import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;

import java.time.Instant;

public record RawMessage(
        long id,
        Long gatewayId,
        String gatewaySn,
        String messageId,
        String topic,
        MqttMessageType messageType,
        String payload,
        Instant receiveTime,
        RawMessageStatus status,
        String failReason
) {

    public RawMessage withStatus(RawMessageStatus nextStatus, String nextFailReason) {
        return new RawMessage(id, gatewayId, gatewaySn, messageId, topic, messageType, payload, receiveTime,
                nextStatus, nextFailReason);
    }
}
