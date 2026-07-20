package com.parkenergyaccess.entity;

import com.parkenergyaccess.enums.CommandStatus;

import java.time.Instant;
import java.util.Map;

public record CommandRecord(
        long id,
        String commandId,
        Long gatewayId,
        String topicGatewayId,
        String targetType,
        Long targetId,
        String targetSn,
        String commandType,
        Map<String, Object> commandPayload,
        String mqttTopic,
        CommandStatus status,
        Long requestUserId,
        String requestUsername,
        Instant requestTime,
        Instant sendTime,
        Instant responseTime,
        Map<String, Object> responsePayload,
        String failReason
) {

    public CommandRecord withStatus(CommandStatus nextStatus, Instant nextSendTime, Instant nextResponseTime,
                                    Map<String, Object> nextResponsePayload, String nextFailReason) {
        return new CommandRecord(id, commandId, gatewayId, topicGatewayId, targetType, targetId, targetSn,
                commandType, commandPayload, mqttTopic, nextStatus, requestUserId, requestUsername, requestTime,
                nextSendTime, nextResponseTime, nextResponsePayload, nextFailReason);
    }
}
