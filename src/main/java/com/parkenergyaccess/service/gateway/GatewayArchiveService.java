package com.parkenergyaccess.service.gateway;

import com.parkenergyaccess.common.BusinessException;
import com.parkenergyaccess.common.ErrorCode;
import com.parkenergyaccess.entity.GatewayArchive;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GatewayArchiveService {

    private final JdbcTemplate jdbcTemplate;

    public GatewayArchiveService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public GatewayArchive validateGateway(String topicGatewayId, String gatewaySn) {
        Long parsedTopicGatewayId;
        try {
            parsedTopicGatewayId = Long.valueOf(topicGatewayId);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.GATEWAY_INVALID,
                    "topic gateway id is invalid: " + topicGatewayId);
        }
        if (gatewaySn == null || gatewaySn.isBlank()) {
            throw new BusinessException(ErrorCode.GATEWAY_INVALID, "gatewaySn is required");
        }
        List<GatewayArchive> gateways = jdbcTemplate.query("""
                        select id, gateway_sn, status
                        from dev_gateway
                        where gateway_sn = cast(? as char character set utf8mb4) collate utf8mb4_general_ci
                        limit 1
                        """,
                (rs, rowNum) -> new GatewayArchive(
                        rs.getLong("id"),
                        topicGatewayId,
                        rs.getString("gateway_sn"),
                        rs.getInt("status") == 1
                ),
                gatewaySn);
        GatewayArchive gateway = gateways.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.GATEWAY_INVALID,
                        "gateway does not exist: " + gatewaySn));
        if (!gateway.enabled()) {
            throw new BusinessException(ErrorCode.GATEWAY_INVALID, "gateway is disabled: " + gatewaySn);
        }
        if (!gateway.gatewayId().equals(parsedTopicGatewayId)) {
            throw new BusinessException(ErrorCode.GATEWAY_INVALID,
                    "topic gateway id does not match gatewaySn: " + topicGatewayId + " / " + gatewaySn);
        }
        return gateway;
    }

    public GatewayArchive validateGatewayId(Long gatewayId) {
        if (gatewayId == null) {
            throw new BusinessException(ErrorCode.GATEWAY_INVALID, "gatewayId is required");
        }
        List<GatewayArchive> gateways = jdbcTemplate.query("""
                        select id, gateway_sn, status
                        from dev_gateway
                        where id = ?
                        limit 1
                        """,
                (rs, rowNum) -> new GatewayArchive(
                        rs.getLong("id"),
                        String.valueOf(rs.getLong("id")),
                        rs.getString("gateway_sn"),
                        rs.getInt("status") == 1
                ),
                gatewayId);
        GatewayArchive gateway = gateways.stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.GATEWAY_INVALID,
                        "gateway does not exist: " + gatewayId));
        if (!gateway.enabled()) {
            throw new BusinessException(ErrorCode.GATEWAY_INVALID, "gateway is disabled: " + gatewayId);
        }
        return gateway;
    }
}
