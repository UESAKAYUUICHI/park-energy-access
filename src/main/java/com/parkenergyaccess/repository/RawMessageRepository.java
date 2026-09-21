package com.parkenergyaccess.repository;

import com.parkenergyaccess.entity.RawMessage;

import java.util.List;
import java.util.Optional;

public interface RawMessageRepository {

    RawMessage save(RawMessage message);

    boolean existsByGatewayIdAndMessageId(Long gatewayId, String messageId);

    Optional<RawMessage> findByGatewayIdAndMessageId(Long gatewayId, String messageId);

    Optional<RawMessage> findById(long id);

    List<RawMessage> findLatest();

    List<RawMessage> findPendingForward(int limit);
}
