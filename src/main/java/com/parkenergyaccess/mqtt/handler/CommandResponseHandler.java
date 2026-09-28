package com.parkenergyaccess.mqtt.handler;
import com.parkenergyaccess.service.message.CommandService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.dto.CommandResponsePayload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommandResponseHandler {

    private final ObjectMapper objectMapper;
    private final CommandService commandService;

    public CommandResponseHandler(ObjectMapper objectMapper, CommandService commandService) {
        this.objectMapper = objectMapper;
        this.commandService = commandService;
    }

    public void handle(String topicGatewayId, String topic, String payload) {
        try {
            CommandResponsePayload response = objectMapper.readValue(payload, CommandResponsePayload.class);
            Map<String, Object> rawResponse = objectMapper.readValue(payload, new TypeReference<>() {});
            commandService.handleResponse(topicGatewayId, response, rawResponse);
        } catch (Exception ignored) {
            // Invalid command responses are ignored because no raw data should enter data service from this topic.
        }
    }
}
