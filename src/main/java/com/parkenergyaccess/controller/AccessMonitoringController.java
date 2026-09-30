package com.parkenergyaccess.controller;

import com.parkenergyaccess.common.ApiResponse;
import com.parkenergyaccess.mqtt.MqttConnectionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.parkenergyaccess.service.gateway.GatewayStatusService;
import com.parkenergyaccess.service.message.RawMessageService;
import com.parkenergyaccess.service.device.DiscoveredDeviceService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal, dependency-free view of the ingestion edge for the platform dashboard. */
@RestController
@RequestMapping("/api/access/monitoring")
public class AccessMonitoringController {
    private final GatewayStatusService gatewayStatusService;
    private final RawMessageService rawMessageService;
    private final DiscoveredDeviceService discoveredDeviceService;

    private final MqttConnectionManager mqtt;
    private final JdbcTemplate jdbc;

    public AccessMonitoringController(GatewayStatusService gatewayStatusService,
                                      RawMessageService rawMessageService,
                                      DiscoveredDeviceService discoveredDeviceService,
                                      MqttConnectionManager mqtt,
                                      JdbcTemplate jdbc) {
        this.gatewayStatusService = gatewayStatusService;
        this.rawMessageService = rawMessageService;
        this.discoveredDeviceService = discoveredDeviceService;
        this.mqtt = mqtt;
        this.jdbc = jdbc;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gateways", gatewayStatusService.list());
        result.put("raw", rawMessageService.latest());
        result.put("discovered", discoveredDeviceService.latest());
        return ApiResponse.success(result);
    }
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mqttConnected", mqtt.isConnected());
        result.put("rawRetryOrInvalidCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM log_raw_message WHERE parse_status = 2", Long.class));
        result.put("pendingForwardCount", jdbc.queryForObject(
                "SELECT COUNT(*) FROM log_raw_message WHERE parse_status = 0 AND topic LIKE '%/data/upload'", Long.class));
        result.put("healthy", mqtt.isConnected());
        return ApiResponse.success(result);
    }
}
