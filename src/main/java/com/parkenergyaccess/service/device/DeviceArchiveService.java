package com.parkenergyaccess.service.device;

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
                        select d.protocol_addr, d.edge_channel_id, t.protocol_type,
                               pp.profile_code as profile_key, v.version_name as model_version,
                               s.desired_revision, s.applied_revision, s.desired_checksum
                        from dev_device d
                        join dev_device_type t on t.id = d.device_type_id
                        left join dev_device_model_version v on v.id = d.model_version_id
                        left join dev_protocol_profile_version pv on pv.id=v.protocol_profile_version_id
                        left join dev_protocol_profile pp on pp.id=pv.profile_id
                        left join dev_gateway_config_state s on s.gateway_id = d.gateway_id
                        where d.gateway_id = ?
                          and BINARY d.device_sn = BINARY ?
                          and d.status = 1
                        limit 1
                        """, gatewayId, meter.deviceSn());
        if (devices.isEmpty()) {
            return MeterValidation.rejected("device does not exist, is disabled, or is not bound to gateway: " + meter.deviceSn());
        }
        Map<String, Object> device = devices.get(0);
        if (meter.points() == null || !meter.points().isObject()) {
            return MeterValidation.rejected("canonical points object is required: " + meter.deviceSn());
        }
        if (meter.registers() != null && !meter.registers().isNull()) {
            return MeterValidation.rejected("raw registers are not accepted; decode them on the gateway: " + meter.deviceSn());
        }
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
        String actualChannel = meter.channelId() == null ? "" : meter.channelId().trim();
        String expectedChannel = valueAsText(device.get("edge_channel_id"));
        if (actualChannel.isBlank()) {
            return MeterValidation.rejected("channelId is required for Modbus device: " + meter.deviceSn());
        }
        if (!expectedChannel.equals(actualChannel)) {
            return MeterValidation.rejected("channelId does not match device archive: " + meter.deviceSn());
        }
        String expectedProfile = valueAsText(device.get("profile_key"));
        String actualProfile = valueAsText(meter.profileKey());
        if (actualProfile.isBlank() || !expectedProfile.equals(actualProfile)) {
            return MeterValidation.rejected("profileKey does not match device archive: " + meter.deviceSn());
        }
        String expectedModelVersion = valueAsText(device.get("model_version"));
        String actualModelVersion = valueAsText(meter.modelVersion());
        if (actualModelVersion.isBlank() || !expectedModelVersion.equals(actualModelVersion)) {
            return MeterValidation.rejected("modelVersion does not match device archive: " + meter.deviceSn());
        }
        String appliedRevision = valueAsText(device.get("applied_revision"));
        String desiredRevision = valueAsText(device.get("desired_revision"));
        String sampleRevision = valueAsText(meter.configRevision());
        if (!sampleRevision.isBlank() && sampleRevision.equals(desiredRevision)
                && !sampleRevision.equals(appliedRevision)) {
            markImplicitlyApplied(gatewayId, sampleRevision);
            appliedRevision = sampleRevision;
        }
        if (!appliedRevision.isBlank() && !"0".equals(appliedRevision)
                && !appliedRevision.equals(sampleRevision)) {
            return MeterValidation.rejected("configRevision is not the gateway applied revision: " + meter.deviceSn());
        }
        return MeterValidation.valid();
    }

    private void markImplicitlyApplied(Long gatewayId, String revision) {
        if (gatewayId == null || revision == null || revision.isBlank()) return;
        jdbcTemplate.update("""
                UPDATE dev_gateway_config_state
                SET applied_revision=desired_revision,
                    applied_checksum=desired_checksum,
                    apply_status='APPLIED',
                    last_error=NULL,
                    last_sync_time=NOW(),
                    update_time=NOW()
                WHERE gateway_id=?
                  AND CAST(desired_revision AS CHAR)=?
                  AND CAST(applied_revision AS CHAR)<>?
                """, gatewayId, revision, revision);
        jdbcTemplate.update("""
                UPDATE dev_gateway_config_release
                SET release_status='APPLIED',
                    error_message=NULL,
                    applied_time=COALESCE(applied_time,NOW()),
                    update_time=NOW()
                WHERE gateway_id=? AND revision=?
                """, gatewayId, revision);
    }

    private String valueAsText(Object value) {
        if (value == null) return "";
        return String.valueOf(value).trim();
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
