package com.parkenergyaccess.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MeterPayload(
        @JsonAlias("device_sn") String deviceSn,
        @JsonAlias("modbus_addr") Integer modbusAddr,
        @JsonAlias("channel_id") String channelId,
        @JsonAlias("profile_key") String profileKey,
        @JsonAlias("model_version") String modelVersion,
        @JsonAlias("config_revision") String configRevision,
        @JsonAlias("collect_time") Long collectTime,
        @JsonAlias("sample_interval_seconds") Integer sampleIntervalSeconds,
        JsonNode registers,
        JsonNode points,
        Integer quality,
        JsonNode payload
) {
    public MeterPayload(String deviceSn, Integer modbusAddr, Long collectTime, JsonNode registers,
                        JsonNode points, Integer quality, JsonNode payload) {
        this(deviceSn, modbusAddr, null, null, null, null, collectTime, null, registers, points, quality, payload);
    }

    public MeterPayload(String deviceSn, Integer modbusAddr, Long collectTime, Integer sampleIntervalSeconds,
                        JsonNode registers, JsonNode points, Integer quality, JsonNode payload) {
        this(deviceSn, modbusAddr, null, null, null, null, collectTime, sampleIntervalSeconds,
                registers, points, quality, payload);
    }
}
