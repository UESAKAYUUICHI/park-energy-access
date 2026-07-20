package com.parkenergyaccess.service;

import com.parkenergyaccess.dto.MeterPayload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DeviceStatusService {

    private final ConcurrentMap<String, Instant> deviceLastSeen = new ConcurrentHashMap<>();

    public void markMetersOnline(List<MeterPayload> meters) {
        if (meters == null) {
            return;
        }
        Instant now = Instant.now();
        meters.stream()
                .filter(meter -> meter.deviceSn() != null && !meter.deviceSn().isBlank())
                .forEach(meter -> deviceLastSeen.put(meter.deviceSn(), now));
    }
}
