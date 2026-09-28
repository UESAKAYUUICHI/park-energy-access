package com.parkenergyaccess.service.device;

import com.parkenergyaccess.dto.MeterPayload;
import com.parkenergyaccess.entity.DiscoveredDevice;
import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.repository.DiscoveredDeviceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class DiscoveredDeviceService {
    private final DiscoveredDeviceRepository repository;

    public DiscoveredDeviceService(DiscoveredDeviceRepository repository) {
        this.repository = repository;
    }

    public void record(long gatewayId, MeterPayload meter, RawMessage raw, String reason) {
        if (meter == null || meter.deviceSn() == null || meter.deviceSn().isBlank()) {
            return;
        }
        repository.record(gatewayId, meter.deviceSn().trim(), meter.modbusAddr() == null ? null : String.valueOf(meter.modbusAddr()),
                raw.id(), shortReason(reason), raw.receiveTime() == null ? Instant.now() : raw.receiveTime());
    }

    public List<DiscoveredDevice> latest() {
        return repository.findLatest();
    }

    public DiscoveredDevice detail(long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("discovered device not found: " + id));
    }

    public DiscoveredDevice bind(long id, long deviceId, String remark) {
        DiscoveredDevice discovery = detail(id);
        if ("BOUND".equals(discovery.discoveryStatus()) && discovery.boundDeviceId() != null
                && discovery.boundDeviceId() != deviceId) {
            throw new IllegalArgumentException("discovered device is already bound to another device");
        }
        return repository.bind(id, deviceId, remark);
    }

    private String shortReason(String reason) {
        String value = reason == null || reason.isBlank() ? "device is not ready for data ingestion" : reason;
        return value.substring(0, Math.min(value.length(), 500));
    }
}
