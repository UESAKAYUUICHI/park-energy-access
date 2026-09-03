package com.parkenergyaccess.service;

import com.parkenergyaccess.dto.MeterPayload;
import com.parkenergyaccess.dto.GatewayHeartbeatPayload;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DeviceStatusService {

    private final ConcurrentMap<String, Instant> deviceLastSeen = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbcTemplate;

    public DeviceStatusService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void markMetersOnline(List<MeterPayload> meters) {
        if (meters == null) {
            return;
        }
        Instant now = Instant.now();
        meters.stream()
                .filter(meter -> meter.deviceSn() != null && !meter.deviceSn().isBlank())
                .forEach(meter -> deviceLastSeen.put(meter.deviceSn(), now));
    }

    /** 心跳只允许更新当前网关已绑定的子设备，未知 SN 不会被自动建档。 */
    public void applyHeartbeat(Long gatewayId, List<GatewayHeartbeatPayload.DeviceHeartbeat> devices) {
        if (gatewayId == null || devices == null) return;
        for (GatewayHeartbeatPayload.DeviceHeartbeat device : devices) {
            if (device == null || device.deviceSn() == null || device.deviceSn().isBlank()) continue;
            boolean online = Boolean.TRUE.equals(device.online());
            Instant seen = device.lastReadTime() == null || device.lastReadTime() <= 0
                    ? Instant.now() : Instant.ofEpochMilli(device.lastReadTime());
            jdbcTemplate.update("""
                    UPDATE dev_device SET online_status=?, last_online_time=?
                    WHERE gateway_id=? AND device_sn=? AND status=1
                    """, online ? 1 : 0, Timestamp.from(seen), gatewayId, device.deviceSn().trim());
            if (online) deviceLastSeen.put(device.deviceSn().trim(), seen);
        }
    }
}
