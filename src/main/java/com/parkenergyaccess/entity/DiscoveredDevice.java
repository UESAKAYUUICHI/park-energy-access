package com.parkenergyaccess.entity;

import java.time.Instant;

/** A device observed on a verified gateway before it has completed asset onboarding. */
public record DiscoveredDevice(
        long id,
        long gatewayId,
        String deviceSn,
        String protocolAddr,
        Instant firstSeenTime,
        Instant lastSeenTime,
        int seenCount,
        Long latestRawLogId,
        String discoveryStatus,
        Long boundDeviceId,
        String failReason,
        String remark
) {
}
