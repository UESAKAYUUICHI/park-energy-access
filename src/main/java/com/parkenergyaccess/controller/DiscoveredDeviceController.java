package com.parkenergyaccess.controller;

import com.parkenergyaccess.common.ApiResponse;
import com.parkenergyaccess.entity.DiscoveredDevice;
import com.parkenergyaccess.service.DiscoveredDeviceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access/discovered-devices")
public class DiscoveredDeviceController {
    private final DiscoveredDeviceService service;

    public DiscoveredDeviceController(DiscoveredDeviceService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<DiscoveredDevice>> latest() {
        return ApiResponse.success(service.latest());
    }

    @GetMapping("/{id}")
    public ApiResponse<DiscoveredDevice> detail(@PathVariable long id) {
        return ApiResponse.success(service.detail(id));
    }

    @PostMapping("/{id}/bind")
    public ApiResponse<DiscoveredDevice> bind(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Object deviceId = body.get("deviceId");
        if (deviceId == null) throw new IllegalArgumentException("deviceId is required");
        return ApiResponse.success(service.bind(id, Long.parseLong(String.valueOf(deviceId)),
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }
}
