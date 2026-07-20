package com.parkenergyaccess.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Map;

public record MeterPayload(
        @JsonAlias("device_sn") String deviceSn,
        @JsonAlias("modbus_addr") Integer modbusAddr,
        @JsonAlias("collect_time") Long collectTime,
        Map<String, Number> registers,
        Integer quality
) {
}
