package com.parkenergyaccess.service.device;

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

    public void markMetersOnline(Long gatewayId, List<MeterPayload> meters) {
        if (meters == null) {
            return;
        }
        Instant now = Instant.now();
        meters.stream()
                .filter(meter -> meter.deviceSn() != null && !meter.deviceSn().isBlank())
                .forEach(meter -> {
                    String deviceSn = meter.deviceSn().trim();
                    deviceLastSeen.put(deviceSn, now);
                    jdbcTemplate.update("""
                            UPDATE dev_device
                            SET online_status=1, last_online_time=NOW()
                            WHERE gateway_id=? AND device_sn=? AND status=1
                            """, gatewayId, deviceSn);
                });
    }

    /** Compatibility overload for callers that do not have the validated gateway id. */
    public void markMetersOnline(List<MeterPayload> meters) {
        if (meters == null) return;
        Instant now = Instant.now();
        meters.stream()
                .filter(meter -> meter.deviceSn() != null && !meter.deviceSn().isBlank())
                .forEach(meter -> deviceLastSeen.put(meter.deviceSn().trim(), now));
    }

    /** 心跳只允许更新当前网关已绑定的子设备，未知 SN 不会被自动建档。 */
    public void applyHeartbeat(Long gatewayId, List<GatewayHeartbeatPayload.DeviceHeartbeat> devices) {
        if (gatewayId == null || devices == null) return;
        for (GatewayHeartbeatPayload.DeviceHeartbeat device : devices) {
            if (device == null || device.deviceSn() == null || device.deviceSn().isBlank()) continue;
            boolean online = Boolean.TRUE.equals(device.online());
            String deviceSn = device.deviceSn().trim();
            if (online) {
                Instant seen = Instant.now();
                jdbcTemplate.update("""
                        UPDATE dev_device SET online_status=1, last_online_time=?
                        WHERE gateway_id=? AND device_sn=? AND status=1
                        """, Timestamp.from(seen), gatewayId, deviceSn);
                deviceLastSeen.put(deviceSn, seen);
            } else {
                jdbcTemplate.update("""
                        UPDATE dev_device SET online_status=0
                        WHERE gateway_id=? AND device_sn=? AND status=1
                        """, gatewayId, deviceSn);
            }
        }
    }
}
