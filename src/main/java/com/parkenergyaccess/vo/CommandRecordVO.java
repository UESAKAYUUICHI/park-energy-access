package com.parkenergyaccess.vo;

import com.parkenergyaccess.enums.CommandStatus;

import java.time.Instant;
import java.util.Map;

public record CommandRecordVO(
        long id,
        String commandId,
        Long gatewayId,
        String targetType,
        Long targetId,
        String targetSn,
        String commandType,
        String mqttTopic,
        CommandStatus status,
        Instant requestTime,
        Instant sendTime,
        Instant responseTime,
        Map<String, Object> responsePayload,
        String failReason
) {
}
