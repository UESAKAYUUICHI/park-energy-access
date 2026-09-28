package com.parkenergyaccess.controller;
import com.parkenergyaccess.service.gateway.GatewayStatusService;

import com.parkenergyaccess.common.ApiResponse;
import com.parkenergyaccess.vo.GatewayStatusVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/access/gateways")
public class GatewayStatusController {

    private final GatewayStatusService gatewayStatusService;

    public GatewayStatusController(GatewayStatusService gatewayStatusService) {
        this.gatewayStatusService = gatewayStatusService;
    }

    @GetMapping("/status")
    public ApiResponse<List<GatewayStatusVO>> listGatewayStatus() {
        return ApiResponse.success(gatewayStatusService.list());
    }
}
