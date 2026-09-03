package com.parkenergyaccess.repository;

import com.parkenergyaccess.entity.DiscoveredDevice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DiscoveredDeviceRepository {
    void record(long gatewayId, String deviceSn, String protocolAddr, long rawLogId, String failReason, Instant seenAt);

    List<DiscoveredDevice> findLatest();

    Optional<DiscoveredDevice> findById(long id);

    DiscoveredDevice bind(long id, long deviceId, String remark);
}
