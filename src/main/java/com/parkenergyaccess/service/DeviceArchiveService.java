package com.parkenergyaccess.service;

import com.parkenergyaccess.common.BusinessException;
import com.parkenergyaccess.common.ErrorCode;
import com.parkenergyaccess.dto.MeterPayload;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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
            validateMeter(gatewayId, meter);
        }
    }

    private void validateMeter(Long gatewayId, MeterPayload meter) {
        if (meter.deviceSn() == null || meter.deviceSn().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "deviceSn is required");
        }
        Integer count = jdbcTemplate.queryForObject("""
                        select count(1)
                        from dev_device
                        where gateway_id = ? and device_sn = ? and status = 1
                        """,
                Integer.class, gatewayId, meter.deviceSn());
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "device does not exist or is disabled: " + meter.deviceSn());
        }
    }
}
