package com.parkenergyaccess.controller;

import com.parkenergyaccess.common.ApiResponse;
import com.parkenergyaccess.service.RawMessageService;
import com.parkenergyaccess.vo.RawMessageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/access/raw-messages")
public class RawMessageController {

    private final RawMessageService rawMessageService;

    public RawMessageController(RawMessageService rawMessageService) {
        this.rawMessageService = rawMessageService;
    }

    @GetMapping
    public ApiResponse<List<RawMessageVO>> listRawMessages() {
        return ApiResponse.success(rawMessageService.latest());
    }
}
