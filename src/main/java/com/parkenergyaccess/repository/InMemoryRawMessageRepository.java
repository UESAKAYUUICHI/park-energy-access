package com.parkenergyaccess.repository;

import com.parkenergyaccess.entity.RawMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@ConditionalOnMissingBean(RawMessageRepository.class)
public class InMemoryRawMessageRepository implements RawMessageRepository {

    private final ConcurrentMap<Long, RawMessage> messages = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> dedupIndex = new ConcurrentHashMap<>();

    @Override
    public RawMessage save(RawMessage message) {
        messages.put(message.id(), message);
        if (message.gatewayId() != null && message.messageId() != null && !message.messageId().isBlank()) {
            dedupIndex.putIfAbsent(dedupKey(message.gatewayId(), message.messageId()), message.id());
        }
        return message;
    }

    @Override
    public Optional<RawMessage> findByGatewayIdAndMessageId(Long gatewayId, String messageId) {
        Long id = dedupIndex.get(dedupKey(gatewayId, messageId));
        return id == null ? Optional.empty() : Optional.ofNullable(messages.get(id));
    }

    @Override
    public Optional<RawMessage> findById(long id) {
        return Optional.ofNullable(messages.get(id));
    }

    @Override
    public List<RawMessage> findLatest() {
        return messages.values().stream()
                .sorted(Comparator.comparing(RawMessage::receiveTime).reversed())
                .limit(100)
                .toList();
    }

    @Override
    public List<RawMessage> findPendingForward(int limit) {
        return messages.values().stream()
                .filter(message -> message.messageType() == com.parkenergyaccess.enums.MqttMessageType.DATA_UPLOAD)
                .filter(message -> message.status() == com.parkenergyaccess.enums.RawMessageStatus.RECEIVED
                        || message.status() == com.parkenergyaccess.enums.RawMessageStatus.MQ_FAILED)
                .sorted(Comparator.comparing(RawMessage::receiveTime))
                .limit(limit)
                .toList();
    }

    private String dedupKey(Long gatewayId, String messageId) {
        return gatewayId + ":" + messageId;
    }
}
