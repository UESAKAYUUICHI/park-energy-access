package com.parkenergyaccess.controller;

import com.parkenergyaccess.common.ApiResponse;
import com.parkenergyaccess.service.RawMessageService;
import com.parkenergyaccess.vo.RawMessageVO;
import com.parkenergyaccess.vo.RawMessageDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/{rawLogId}")
    public ApiResponse<RawMessageDetailVO> detail(@PathVariable long rawLogId) {
        return ApiResponse.success(rawMessageService.detail(rawLogId));
    }

    @PostMapping("/{rawLogId}/replay")
    public ApiResponse<Void> replay(@PathVariable long rawLogId) {
        rawMessageService.replay(rawLogId);
        return ApiResponse.success(null);
    }
}
