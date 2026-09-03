package com.parkenergyaccess.service;

import com.parkenergyaccess.common.BusinessException;
import com.parkenergyaccess.dto.MeterPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceArchiveServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DeviceArchiveService service;

    @Test
    void acceptsJsonDeviceWithoutModbusAddress() {
        when(jdbcTemplate.queryForList(anyString(), eq(3L), eq("JSON-METER-1")))
                .thenReturn(List.of(Map.of("protocol_type", "JSON", "protocol_addr", "")));

        assertDoesNotThrow(() -> service.validateMeters(3L,
                List.of(new MeterPayload("JSON-METER-1", null, null, null, null, 0, null))));
    }

    @Test
    void requiresMatchingAddressForModbusDevice() {
        when(jdbcTemplate.queryForList(anyString(), eq(3L), eq("MODBUS-METER-1")))
                .thenReturn(List.of(Map.of("protocol_type", "MODBUS_RTU", "protocol_addr", "8")));

        assertThrows(BusinessException.class, () -> service.validateMeters(3L,
                List.of(new MeterPayload("MODBUS-METER-1", null, null, null, null, 0, null))));
        assertThrows(BusinessException.class, () -> service.validateMeters(3L,
                List.of(new MeterPayload("MODBUS-METER-1", 7, null, null, null, 0, null))));
        assertDoesNotThrow(() -> service.validateMeters(3L,
                List.of(new MeterPayload("MODBUS-METER-1", 8, null, null, null, 0, null))));
    }
}
