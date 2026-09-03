package com.parkenergyaccess.repository;

import com.parkenergyaccess.entity.DiscoveredDevice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcDiscoveredDeviceRepository implements DiscoveredDeviceRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<DiscoveredDevice> rowMapper = (rs, rowNum) -> new DiscoveredDevice(
            rs.getLong("id"), rs.getLong("gateway_id"), rs.getString("device_sn"),
            rs.getString("protocol_addr"), rs.getTimestamp("first_seen_time").toInstant(),
            rs.getTimestamp("last_seen_time").toInstant(), rs.getInt("seen_count"),
            rs.getObject("latest_raw_log_id", Long.class), rs.getString("discovery_status"),
            rs.getObject("bound_device_id", Long.class), rs.getString("fail_reason"), rs.getString("remark"));

    public JdbcDiscoveredDeviceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(long gatewayId, String deviceSn, String protocolAddr, long rawLogId, String failReason, Instant seenAt) {
        jdbcTemplate.update("""
                INSERT INTO access_discovered_device
                (gateway_id,device_sn,protocol_addr,first_seen_time,last_seen_time,seen_count,latest_raw_log_id,discovery_status,fail_reason)
                VALUES (?,?,?,?,?,1,?,'PENDING',?)
                ON DUPLICATE KEY UPDATE
                    protocol_addr=VALUES(protocol_addr), last_seen_time=VALUES(last_seen_time),
                    seen_count=seen_count+1, latest_raw_log_id=VALUES(latest_raw_log_id),
                    discovery_status=IF(discovery_status='BOUND','BOUND','PENDING'),
                    fail_reason=VALUES(fail_reason)
                """, gatewayId, deviceSn, protocolAddr, Timestamp.from(seenAt), Timestamp.from(seenAt), rawLogId, failReason);
    }

    @Override
    public List<DiscoveredDevice> findLatest() {
        return jdbcTemplate.query("""
                SELECT id,gateway_id,device_sn,protocol_addr,first_seen_time,last_seen_time,seen_count,
                       latest_raw_log_id,discovery_status,bound_device_id,fail_reason,remark
                FROM access_discovered_device
                ORDER BY FIELD(discovery_status,'PENDING','IGNORED','BOUND'), last_seen_time DESC, id DESC
                LIMIT 200
                """, rowMapper);
    }

    @Override
    public Optional<DiscoveredDevice> findById(long id) {
        List<DiscoveredDevice> rows = jdbcTemplate.query("""
                SELECT id,gateway_id,device_sn,protocol_addr,first_seen_time,last_seen_time,seen_count,
                       latest_raw_log_id,discovery_status,bound_device_id,fail_reason,remark
                FROM access_discovered_device WHERE id=?
                """, rowMapper, id);
        return rows.stream().findFirst();
    }

    @Override
    public DiscoveredDevice bind(long id, long deviceId, String remark) {
        jdbcTemplate.update("""
                UPDATE access_discovered_device
                SET discovery_status='BOUND', bound_device_id=?, remark=?
                WHERE id=?
                """, deviceId, remark, id);
        return findById(id).orElseThrow(() -> new IllegalArgumentException("discovered device not found: " + id));
    }
}
