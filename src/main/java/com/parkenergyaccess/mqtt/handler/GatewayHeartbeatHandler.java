package com.parkenergyaccess.mqtt.handler;
import com.parkenergyaccess.service.gateway.GatewayStatusService;
import com.parkenergyaccess.service.gateway.GatewayArchiveService;
import com.parkenergyaccess.service.message.RawMessageService;
import com.parkenergyaccess.service.device.DeviceStatusService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.dto.GatewayHeartbeatPayload;
import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.enums.MqttMessageType;
import org.springframework.stereotype.Component;

@Component
public class GatewayHeartbeatHandler {

    private final ObjectMapper objectMapper;
    private final GatewayArchiveService gatewayArchiveService;
    private final GatewayStatusService gatewayStatusService;
    private final RawMessageService rawMessageService;
    private final DeviceStatusService deviceStatusService;

    public GatewayHeartbeatHandler(ObjectMapper objectMapper, GatewayArchiveService gatewayArchiveService,
                                   GatewayStatusService gatewayStatusService, RawMessageService rawMessageService,
                                   DeviceStatusService deviceStatusService) {
        this.objectMapper = objectMapper;
        this.gatewayArchiveService = gatewayArchiveService;
        this.gatewayStatusService = gatewayStatusService;
        this.rawMessageService = rawMessageService;
        this.deviceStatusService = deviceStatusService;
    }

    public void handle(String topicGatewayId, String topic, String payload) {
        try {
            GatewayHeartbeatPayload heartbeat = objectMapper.readValue(payload, GatewayHeartbeatPayload.class);
            GatewayArchive gateway = gatewayArchiveService.validateGateway(topicGatewayId, heartbeat.gatewaySn());
            rawMessageService.saveInbound(gateway, heartbeat.messageId(), topic, MqttMessageType.HEARTBEAT, payload);
            gatewayStatusService.markOnline(gateway);
            deviceStatusService.applyHeartbeat(gateway.gatewayId(), heartbeat.devices());
        } catch (Exception ex) {
            rawMessageService.saveInvalid(topic, MqttMessageType.HEARTBEAT, payload, ex.getMessage());
        }
    }
}
