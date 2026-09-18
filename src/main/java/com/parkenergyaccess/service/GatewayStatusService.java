package com.parkenergyaccess.service;

import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.vo.GatewayStatusVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.List;

@Service
public class GatewayStatusService {

    private final JdbcTemplate jdbcTemplate;

    public GatewayStatusService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void markOnline(GatewayArchive gateway) {
        jdbcTemplate.update("""
                        update dev_gateway
                        set online_status = 1, last_online_time = now()
                        where id = ?
                        """,
                gateway.gatewayId());
    }

    @Scheduled(fixedDelayString = "${park.mqtt.offline-check-ms:30000}", initialDelay = 30000)
    public void markTimedOutGatewaysOffline() {
        jdbcTemplate.update("""
                UPDATE dev_gateway
                SET online_status = 0
                WHERE status = 1 AND online_status = 1
                  AND (last_online_time IS NULL OR TIMESTAMPDIFF(SECOND, last_online_time, NOW())
                       > COALESCE(NULLIF(heartbeat_interval, 0), 30) * 3)
                """);
        jdbcTemplate.update("""
                UPDATE dev_device d JOIN dev_gateway g ON g.id=d.gateway_id
                SET d.online_status=0
                WHERE d.status=1 AND g.online_status=0
                """);
        jdbcTemplate.update("""
                UPDATE dev_device d
                LEFT JOIN dev_gateway g ON g.id=d.gateway_id
                SET d.online_status=0
                WHERE d.status=1
                  AND (d.last_online_time IS NULL
                       OR TIMESTAMPDIFF(SECOND,d.last_online_time,NOW()) >
                          GREATEST(
                              COALESCE(NULLIF(d.collect_interval_seconds,0), 300) * 3,
                              COALESCE(NULLIF(g.heartbeat_interval,0), 30) * 3
                          ))
                """);
    }

    public List<GatewayStatusVO> list() {
        return jdbcTemplate.query("""
                        select id, gateway_sn, online_status, last_online_time
                        from dev_gateway
                        order by last_online_time desc, id desc
                        limit 100
                        """,
                (rs, rowNum) -> {
                    var lastOnlineTime = rs.getTimestamp("last_online_time");
                    Instant lastSeen = lastOnlineTime == null ? null : lastOnlineTime.toInstant();
                    return new GatewayStatusVO(rs.getLong("id"), String.valueOf(rs.getLong("id")),
                            rs.getString("gateway_sn"),
                            rs.getInt("online_status") == 1 ? "ONLINE" : "OFFLINE",
                            lastSeen);
                });
    }
}
