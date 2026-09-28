package com.parkenergyaccess.mqtt.handler;
import com.parkenergyaccess.service.gateway.GatewayStatusService;
import com.parkenergyaccess.service.gateway.GatewayArchiveService;
import com.parkenergyaccess.service.message.RawMessageService;
import com.parkenergyaccess.service.device.DiscoveredDeviceService;
import com.parkenergyaccess.service.device.DeviceStatusService;
import com.parkenergyaccess.service.device.DeviceArchiveService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.dto.GatewayUploadPayload;
import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;
import com.parkenergyaccess.mq.RawDataProducer;
import com.parkenergyaccess.mq.message.AccessForwardMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GatewayDataUploadHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayDataUploadHandler.class);

    private final ObjectMapper objectMapper;
    private final GatewayArchiveService gatewayArchiveService;
    private final DeviceArchiveService deviceArchiveService;
    private final DiscoveredDeviceService discoveredDeviceService;
    private final GatewayStatusService gatewayStatusService;
    private final DeviceStatusService deviceStatusService;
    private final RawMessageService rawMessageService;
    private final RawDataProducer rawDataProducer;

    public GatewayDataUploadHandler(ObjectMapper objectMapper, GatewayArchiveService gatewayArchiveService,
                                    DeviceArchiveService deviceArchiveService,
                                    DiscoveredDeviceService discoveredDeviceService,
                                    GatewayStatusService gatewayStatusService, DeviceStatusService deviceStatusService,
                                    RawMessageService rawMessageService, RawDataProducer rawDataProducer) {
        this.objectMapper = objectMapper;
        this.gatewayArchiveService = gatewayArchiveService;
        this.deviceArchiveService = deviceArchiveService;
        this.discoveredDeviceService = discoveredDeviceService;
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
            List<com.parkenergyaccess.dto.MeterPayload> acceptedMeters = new ArrayList<>();
            List<String> rejectedReasons = new ArrayList<>();
            if (upload.meters() == null || upload.meters().isEmpty()) {
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.INVALID, "meters is required");
                return;
            }
            for (com.parkenergyaccess.dto.MeterPayload meter : upload.meters()) {
                DeviceArchiveService.MeterValidation validation = deviceArchiveService.inspectMeter(gateway.gatewayId(), meter);
                if (validation.accepted()) {
                    acceptedMeters.add(meter);
                } else {
                    discoveredDeviceService.record(gateway.gatewayId(), meter, raw, validation.reason());
                    rejectedReasons.add(validation.reason());
                }
            }
            if (acceptedMeters.isEmpty()) {
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.INVALID,
                        shortReason("No registered devices in message: " + String.join("; ", rejectedReasons)));
                return;
            }
            gatewayStatusService.markOnline(gateway);
            deviceStatusService.markMetersOnline(gateway.gatewayId(), acceptedMeters);
            try {
                GatewayUploadPayload forwardPayload = new GatewayUploadPayload(upload.messageId(), upload.gatewaySn(),
                        upload.timestamp(), upload.type(), upload.sampleIntervalSeconds(),
                        upload.reportWindowSeconds(), acceptedMeters, upload.schemaVersion());
                rawDataProducer.publish(new AccessForwardMessage(raw.id(), upload.messageId(), gateway.gatewayId(),
                        gateway.gatewaySn(), "DATA_UPLOAD", objectMapper.writeValueAsString(forwardPayload), raw.receiveTime()));
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.FORWARDED, null);
            } catch (Exception ex) {
                log.warn("Forward raw data to RabbitMQ failed, rawLogId={}, messageId={}",
                        raw.id(), upload.messageId(), ex);
                try {
                    rawMessageService.updateStatus(raw.id(), RawMessageStatus.MQ_FAILED, ex.getMessage());
                } catch (Exception updateException) {
                    log.error("Update raw message status failed after RabbitMQ forward failure, rawLogId={}",
                            raw.id(), updateException);
                }
            }
        } catch (Exception ex) {
            log.warn("Handle gateway data upload failed, topic={}, topicGatewayId={}", topic, topicGatewayId, ex);
            try {
                GatewayArchive topicGateway = gatewayArchiveService.validateGatewayId(Long.valueOf(topicGatewayId));
                rawMessageService.saveInvalid(topicGateway, topic, MqttMessageType.DATA_UPLOAD, payload, ex.getMessage());
            } catch (Exception ignored) {
                rawMessageService.saveInvalid(topic, MqttMessageType.DATA_UPLOAD, payload, ex.getMessage());
            }
        }
    }

    private String shortReason(String reason) {
        if (reason == null || reason.isBlank()) return "invalid data upload";
        return reason.substring(0, Math.min(reason.length(), 240));
    }
}
