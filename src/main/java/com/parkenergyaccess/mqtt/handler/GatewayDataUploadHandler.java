package com.parkenergyaccess.mqtt.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.dto.GatewayUploadPayload;
import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;
import com.parkenergyaccess.mq.RawDataProducer;
import com.parkenergyaccess.mq.message.AccessForwardMessage;
import com.parkenergyaccess.service.DeviceStatusService;
import com.parkenergyaccess.service.DeviceArchiveService;
import com.parkenergyaccess.service.GatewayArchiveService;
import com.parkenergyaccess.service.GatewayStatusService;
import com.parkenergyaccess.service.RawMessageService;
import org.springframework.stereotype.Component;

@Component
public class GatewayDataUploadHandler {

    private final ObjectMapper objectMapper;
    private final GatewayArchiveService gatewayArchiveService;
    private final DeviceArchiveService deviceArchiveService;
    private final GatewayStatusService gatewayStatusService;
    private final DeviceStatusService deviceStatusService;
    private final RawMessageService rawMessageService;
    private final RawDataProducer rawDataProducer;

    public GatewayDataUploadHandler(ObjectMapper objectMapper, GatewayArchiveService gatewayArchiveService,
                                    DeviceArchiveService deviceArchiveService,
                                    GatewayStatusService gatewayStatusService, DeviceStatusService deviceStatusService,
                                    RawMessageService rawMessageService, RawDataProducer rawDataProducer) {
        this.objectMapper = objectMapper;
        this.gatewayArchiveService = gatewayArchiveService;
        this.deviceArchiveService = deviceArchiveService;
        this.gatewayStatusService = gatewayStatusService;
        this.deviceStatusService = deviceStatusService;
        this.rawMessageService = rawMessageService;
        this.rawDataProducer = rawDataProducer;
    }

    public void handle(String topicGatewayId, String topic, String payload) {
        try {
            GatewayUploadPayload upload = objectMapper.readValue(payload, GatewayUploadPayload.class);
            GatewayArchive gateway = gatewayArchiveService.validateGateway(topicGatewayId, upload.gatewaySn());
            if (rawMessageService.isDuplicate(gateway.gatewayId(), upload.messageId())) {
                return;
            }

            RawMessage raw = rawMessageService.saveInbound(gateway, upload.messageId(), topic,
                    MqttMessageType.DATA_UPLOAD, payload);
            try {
                deviceArchiveService.validateMeters(gateway.gatewayId(), upload.meters());
            } catch (Exception ex) {
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.INVALID, ex.getMessage());
                return;
            }
            gatewayStatusService.markOnline(gateway);
            deviceStatusService.markMetersOnline(upload.meters());
            try {
                rawDataProducer.publish(new AccessForwardMessage(raw.id(), upload.messageId(), gateway.gatewayId(),
                        gateway.gatewaySn(), "DATA_UPLOAD", payload, raw.receiveTime()));
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.FORWARDED, null);
            } catch (Exception ex) {
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.MQ_FAILED, ex.getMessage());
            }
        } catch (Exception ex) {
            rawMessageService.saveInvalid(topic, MqttMessageType.DATA_UPLOAD, payload, ex.getMessage());
        }
    }
}
