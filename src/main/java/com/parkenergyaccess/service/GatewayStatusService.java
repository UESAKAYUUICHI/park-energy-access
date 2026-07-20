package com.parkenergyaccess.service;

import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.vo.GatewayStatusVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
