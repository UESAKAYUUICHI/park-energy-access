package com.parkenergyaccess.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CommandCreateRequest(
        @NotNull Long gatewayId,
        String topicGatewayId,
        @NotBlank String targetType,
        Long targetId,
        String targetSn,
        @NotBlank String commandType,
        Map<String, Object> commandPayload,
        Long requestUserId,
        String requestUsername
) {
}
