package com.parkenergyaccess.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record CommandResponsePayload(
        @JsonAlias("command_id") String commandId,
        String status,
        String message,
        Long timestamp
) {
}
