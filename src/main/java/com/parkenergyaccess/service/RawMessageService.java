package com.parkenergyaccess.service;

import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;
import com.parkenergyaccess.repository.RawMessageRepository;
import com.parkenergyaccess.mq.RawDataProducer;
import com.parkenergyaccess.mq.message.AccessForwardMessage;
import com.parkenergyaccess.vo.RawMessageVO;
import com.parkenergyaccess.vo.RawMessageDetailVO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RawMessageService {

    private final RawMessageRepository repository;
    private final IdGenerator idGenerator;
    private final RawDataProducer rawDataProducer;

    public RawMessageService(RawMessageRepository repository, IdGenerator idGenerator, RawDataProducer rawDataProducer) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.rawDataProducer = rawDataProducer;
    }

    public RawMessage saveInbound(GatewayArchive gateway, String messageId, String topic, MqttMessageType messageType,
                                  String payload) {
        RawMessage message = new RawMessage(idGenerator.nextRawMessageId(), gateway.gatewayId(), gateway.gatewaySn(),
                messageId, topic, messageType, payload, Instant.now(), RawMessageStatus.RECEIVED, null, null);
        return repository.save(message);
    }

    public RawMessage saveInvalid(String topic, MqttMessageType messageType, String payload, String failReason) {
        RawMessage message = new RawMessage(idGenerator.nextRawMessageId(), null, null, null, topic, messageType,
                payload, Instant.now(), RawMessageStatus.INVALID, failureCode(failReason), safeReason(failReason));
        return repository.save(message);
    }

    public RawMessage saveInvalid(GatewayArchive gateway, String topic, MqttMessageType messageType,
                                  String payload, String failReason) {
        long generatedId = idGenerator.nextRawMessageId();
        RawMessage message = new RawMessage(generatedId, gateway.gatewayId(), gateway.gatewaySn(),
                "INVALID-" + generatedId, topic, messageType, payload, Instant.now(),
                RawMessageStatus.RECEIVED, failureCode(failReason), safeReason(failReason));
        RawMessage saved = repository.save(message);
        return updateStatus(saved.id(), RawMessageStatus.INVALID, failReason);
    }

    public boolean isDuplicate(Long gatewayId, String messageId) {
        return repository.existsByGatewayIdAndMessageId(gatewayId, messageId);
    }

    public RawMessage updateStatus(long rawLogId, RawMessageStatus status, String failReason) {
        RawMessage old = repository.findById(rawLogId).orElseThrow();
        String reason = normalizeFailReason(status, failReason);
        return repository.save(old.withFailure(status, failureCode(reason), safeReason(reason)));
    }

    public List<RawMessage> pendingForward(int limit) {
        return repository.findPendingForward(limit);
    }

    public void replay(long rawLogId) {
        RawMessage raw = repository.findById(rawLogId).orElseThrow(() -> new IllegalArgumentException("raw message not found: " + rawLogId));
        if (raw.gatewayId() == null || raw.topic() == null ||
                !(raw.topic().endsWith("/data/upload") || raw.topic().endsWith("/alarm/up"))) {
            throw new IllegalArgumentException("only gateway data/alarm upload messages can be replayed");
        }
        try {
            String replayMessageId = "REPLAY-" + raw.id() + "-" + Instant.now().toEpochMilli();
            rawDataProducer.publish(new AccessForwardMessage(raw.id(), replayMessageId, raw.gatewayId(), raw.gatewaySn(),
                    raw.topic().endsWith("/alarm/up") ? "ALARM_UPLOAD" : "DATA_UPLOAD",
                    raw.payload(), raw.receiveTime()));
            updateStatus(raw.id(), RawMessageStatus.FORWARDED, null);
        } catch (Exception exception) {
            updateStatus(raw.id(), RawMessageStatus.MQ_FAILED, exception.getMessage());
            throw new IllegalStateException("raw message replay forward failed", exception);
        }
    }

    public List<RawMessageVO> latest() {
        return repository.findLatest().stream().map(this::toVO).toList();
    }

    public RawMessageDetailVO detail(long rawLogId) {
        RawMessage message = repository.findById(rawLogId)
                .orElseThrow(() -> new IllegalArgumentException("raw message not found: " + rawLogId));
        return new RawMessageDetailVO(message.id(), message.gatewayId(), message.gatewaySn(), message.messageId(),
                message.topic(), message.messageType(), message.status(), message.failCode(), message.failReason(), message.receiveTime(),
                message.payload());
    }

    private RawMessageVO toVO(RawMessage message) {
        return new RawMessageVO(message.id(), message.gatewayId(), message.gatewaySn(), message.messageId(),
                message.topic(), message.messageType(), message.status(), message.failCode(), message.failReason(), message.receiveTime());
    }

    private String normalizeFailReason(RawMessageStatus status, String failReason) {
        if (status == RawMessageStatus.MQ_FAILED) {
            String reason = failReason == null || failReason.isBlank() ? "unknown" : failReason;
            return reason.startsWith("MQ_FAILED:") ? reason : "MQ_FAILED:" + reason;
        }
        return failReason;
    }

    private String failureCode(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String value = reason == null ? "" : reason.toUpperCase();
        if (value.contains("MQ_FAILED") || value.contains("RABBIT")) return "MQ_PUBLISH_FAILED";
        if (value.contains("REQUIRED POINT") || value.contains("MISSING")) return "REQUIRED_POINT_MISSING";
        if (value.contains("RATE LIMIT")) return "RATE_LIMITED";
        if (value.contains("NO DEVICE SAMPLE") || (value.contains("DEVICE") && (value.contains("NOT FOUND") || value.contains("NOT ACCEPTED") || value.contains("WERE ACCEPTED")))) return "DEVICE_SAMPLE_REJECTED";
        if (value.contains("SQL") || value.contains("DATABASE")) return "DB_WRITE_FAILED";
        return "RAW_PAYLOAD_INVALID";
    }

    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String compact = reason.replaceAll("\\s+", " ").trim();
        return compact.length() <= 460 ? compact : compact.substring(0, 457) + "...";
    }
}
