package com.parkenergyaccess.service;

import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;
import com.parkenergyaccess.repository.RawMessageRepository;
import com.parkenergyaccess.vo.RawMessageVO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RawMessageService {

    private final RawMessageRepository repository;
    private final IdGenerator idGenerator;

    public RawMessageService(RawMessageRepository repository, IdGenerator idGenerator) {
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    public RawMessage saveInbound(GatewayArchive gateway, String messageId, String topic, MqttMessageType messageType,
                                  String payload) {
        RawMessage message = new RawMessage(idGenerator.nextRawMessageId(), gateway.gatewayId(), gateway.gatewaySn(),
                messageId, topic, messageType, payload, Instant.now(), RawMessageStatus.RECEIVED, null);
        return repository.save(message);
    }

    public RawMessage saveInvalid(String topic, MqttMessageType messageType, String payload, String failReason) {
        RawMessage message = new RawMessage(idGenerator.nextRawMessageId(), null, null, null, topic, messageType,
                payload, Instant.now(), RawMessageStatus.INVALID, failReason);
        return repository.save(message);
    }

    public boolean isDuplicate(Long gatewayId, String messageId) {
        return repository.findByGatewayIdAndMessageId(gatewayId, messageId).isPresent();
    }

    public RawMessage updateStatus(long rawLogId, RawMessageStatus status, String failReason) {
        RawMessage old = repository.findById(rawLogId).orElseThrow();
        return repository.save(old.withStatus(status, normalizeFailReason(status, failReason)));
    }

    public List<RawMessage> mqFailed(int limit) {
        return repository.findMqFailed(limit);
    }

    public List<RawMessageVO> latest() {
        return repository.findLatest().stream().map(this::toVO).toList();
    }

    private RawMessageVO toVO(RawMessage message) {
        return new RawMessageVO(message.id(), message.gatewayId(), message.gatewaySn(), message.messageId(),
                message.topic(), message.messageType(), message.status(), message.failReason(), message.receiveTime());
    }

    private String normalizeFailReason(RawMessageStatus status, String failReason) {
        if (status == RawMessageStatus.MQ_FAILED) {
            String reason = failReason == null || failReason.isBlank() ? "unknown" : failReason;
            return reason.startsWith("MQ_FAILED:") ? reason : "MQ_FAILED:" + reason;
        }
        return failReason;
    }
}
