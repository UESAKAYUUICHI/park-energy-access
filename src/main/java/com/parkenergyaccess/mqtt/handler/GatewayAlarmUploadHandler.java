package com.parkenergyaccess.mqtt.handler;
import com.parkenergyaccess.service.gateway.GatewayStatusService;
import com.parkenergyaccess.service.gateway.GatewayArchiveService;
import com.parkenergyaccess.service.message.RawMessageService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.dto.GatewayAlarmPayload;
import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;
import com.parkenergyaccess.mq.RawDataProducer;
import com.parkenergyaccess.mq.message.AccessForwardMessage;
import com.parkenergyaccess.mqtt.publisher.CommandMqttPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GatewayAlarmUploadHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayAlarmUploadHandler.class);
    private final ObjectMapper objectMapper;
    private final GatewayArchiveService gatewayArchiveService;
    private final GatewayStatusService gatewayStatusService;
    private final RawMessageService rawMessageService;
    private final RawDataProducer rawDataProducer;
    private final CommandMqttPublisher mqttPublisher;

    public GatewayAlarmUploadHandler(ObjectMapper objectMapper, GatewayArchiveService gatewayArchiveService,
                                     GatewayStatusService gatewayStatusService, RawMessageService rawMessageService,
                                     RawDataProducer rawDataProducer, CommandMqttPublisher mqttPublisher) {
        this.objectMapper = objectMapper;
        this.gatewayArchiveService = gatewayArchiveService;
        this.gatewayStatusService = gatewayStatusService;
        this.rawMessageService = rawMessageService;
        this.rawDataProducer = rawDataProducer;
        this.mqttPublisher = mqttPublisher;
    }

    public void handle(String topicGatewayId, String topic, String rawPayload) {
        try {
            GatewayAlarmPayload payload = objectMapper.readValue(rawPayload, GatewayAlarmPayload.class);
            GatewayArchive gateway = gatewayArchiveService.validateGateway(topicGatewayId, payload.gatewaySn());
            validate(payload);
            if (rawMessageService.isDuplicate(gateway.gatewayId(), payload.messageId())) return;
            RawMessage raw = rawMessageService.saveInbound(gateway, payload.messageId(), topic,
                    MqttMessageType.ALARM_UPLOAD, rawPayload);
            try {
                rawDataProducer.publish(new AccessForwardMessage(raw.id(), payload.messageId(), gateway.gatewayId(),
                        gateway.gatewaySn(), "ALARM_UPLOAD", rawPayload, raw.receiveTime()));
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.FORWARDED, null);
                gatewayStatusService.markOnline(gateway);
                publishReceipt(gateway.gatewayId(), payload, "ACCESS_FORWARDED", null);
            } catch (Exception exception) {
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.MQ_FAILED, exception.getMessage());
            }
        } catch (Exception exception) {
            log.warn("Handle gateway alarm upload failed, topic={}", topic, exception);
            rawMessageService.saveInvalid(topic, MqttMessageType.ALARM_UPLOAD, rawPayload, exception.getMessage());
        }
    }

    private void publishReceipt(long gatewayId, GatewayAlarmPayload payload, String status, String error) {
        try {
            Map<String, Object> receipt = new LinkedHashMap<>();
            receipt.put("messageId", payload.messageId());
            receipt.put("eventId", payload.eventId());
            receipt.put("status", status);
            receipt.put("error", error);
            receipt.put("processedAt", Instant.now().toEpochMilli());
            mqttPublisher.publish("gateway/" + gatewayId + "/alarm/ack", receipt);
        } catch (Exception exception) {
            log.warn("Publish access alarm receipt failed, messageId={}", payload.messageId(), exception);
        }
    }

    private void validate(GatewayAlarmPayload payload) {
        if (payload.messageId() == null || payload.messageId().isBlank()) throw new IllegalArgumentException("messageId is required");
        if (payload.eventId() == null || payload.eventId().isBlank()) throw new IllegalArgumentException("eventId is required");
        if (!"RAISED".equals(payload.action()) && !"RECOVERED".equals(payload.action())) {
            throw new IllegalArgumentException("action must be RAISED or RECOVERED");
        }
    }
}
