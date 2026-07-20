package com.parkenergyaccess.vo;

import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;

import java.time.Instant;

public record RawMessageVO(long id, Long gatewayId, String gatewaySn, String messageId, String topic,
                           MqttMessageType messageType, RawMessageStatus status, String failReason,
                           Instant receiveTime) {
}
