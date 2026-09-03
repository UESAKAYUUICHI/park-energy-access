package com.parkenergyaccess.service;

import com.parkenergyaccess.common.BusinessException;
import com.parkenergyaccess.common.ErrorCode;
import com.parkenergyaccess.dto.MeterPayload;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DeviceArchiveService {

    private final JdbcTemplate jdbcTemplate;

    public DeviceArchiveService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void validateMeters(Long gatewayId, List<MeterPayload> meters) {
        if (meters == null || meters.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "meters is required");
        }
        for (MeterPayload meter : meters) {
            MeterValidation validation = inspectMeter(gatewayId, meter);
            if (!validation.accepted()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, validation.reason());
            }
        }
    }

    public MeterValidation inspectMeter(Long gatewayId, MeterPayload meter) {
        if (meter.deviceSn() == null || meter.deviceSn().isBlank()) {
            return MeterValidation.rejected("deviceSn is required");
        }
        List<Map<String, Object>> devices = jdbcTemplate.queryForList("""
                        select d.protocol_addr, t.protocol_type
                        from dev_device d
                        join dev_device_type t on t.id = d.device_type_id
                        where d.gateway_id = ?
                          and BINARY d.device_sn = BINARY ?
                          and d.status = 1
                        limit 1
                        """, gatewayId, meter.deviceSn());
        if (devices.isEmpty()) {
            return MeterValidation.rejected("device does not exist, is disabled, or is not bound to gateway: " + meter.deviceSn());
        }
        Map<String, Object> device = devices.get(0);
        String protocolType = String.valueOf(device.getOrDefault("protocol_type", ""))
                .trim().toUpperCase(Locale.ROOT);
        if (!protocolType.startsWith("MODBUS")) {
            return MeterValidation.valid();
        }
        if (meter.modbusAddr() == null || meter.modbusAddr() <= 0) {
            return MeterValidation.rejected("modbusAddr is required for Modbus device: " + meter.deviceSn());
        }
        String expectedAddress = String.valueOf(device.getOrDefault("protocol_addr", "")).trim();
        if (!expectedAddress.equals(String.valueOf(meter.modbusAddr()))) {
            return MeterValidation.rejected("modbusAddr does not match device archive: " + meter.deviceSn());
        }
        return MeterValidation.valid();
    }

    public record MeterValidation(boolean accepted, String reason) {
        public static MeterValidation valid() {
            return new MeterValidation(true, null);
        }

        public static MeterValidation rejected(String reason) {
            return new MeterValidation(false, reason);
        }
    }
}
