package com.parkenergyaccess.mqtt.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.dto.GatewayHeartbeatPayload;
import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.service.GatewayArchiveService;
import com.parkenergyaccess.service.GatewayStatusService;
import com.parkenergyaccess.service.RawMessageService;
import org.springframework.stereotype.Component;

@Component
public class GatewayHeartbeatHandler {

    private final ObjectMapper objectMapper;
    private final GatewayArchiveService gatewayArchiveService;
    private final GatewayStatusService gatewayStatusService;
    private final RawMessageService rawMessageService;

    public GatewayHeartbeatHandler(ObjectMapper objectMapper, GatewayArchiveService gatewayArchiveService,
                                   GatewayStatusService gatewayStatusService, RawMessageService rawMessageService) {
        this.objectMapper = objectMapper;
        this.gatewayArchiveService = gatewayArchiveService;
        this.gatewayStatusService = gatewayStatusService;
        this.rawMessageService = rawMessageService;
    }

    public void handle(String topicGatewayId, String topic, String payload) {
        try {
            GatewayHeartbeatPayload heartbeat = objectMapper.readValue(payload, GatewayHeartbeatPayload.class);
            GatewayArchive gateway = gatewayArchiveService.validateGateway(topicGatewayId, heartbeat.gatewaySn());
            rawMessageService.saveInbound(gateway, heartbeat.messageId(), topic, MqttMessageType.HEARTBEAT, payload);
            gatewayStatusService.markOnline(gateway);
        } catch (Exception ex) {
            rawMessageService.saveInvalid(topic, MqttMessageType.HEARTBEAT, payload, ex.getMessage());
        }
    }
}
