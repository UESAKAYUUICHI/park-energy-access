package com.parkenergyaccess.controller;
import com.parkenergyaccess.service.message.CommandService;

import com.parkenergyaccess.common.ApiResponse;
import com.parkenergyaccess.dto.CommandCreateRequest;
import com.parkenergyaccess.vo.CommandRecordVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/access/commands")
public class AccessCommandController {

    private final CommandService commandService;

    public AccessCommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    public ApiResponse<CommandRecordVO> sendCommand(@Valid @RequestBody CommandCreateRequest request) {
        return ApiResponse.success(commandService.sendCommand(request));
    }

    @GetMapping
    public ApiResponse<List<CommandRecordVO>> listCommands() {
        return ApiResponse.success(commandService.latest());
    }

    @GetMapping("/{commandId}")
    public ApiResponse<CommandRecordVO> getCommand(@PathVariable String commandId) {
        return ApiResponse.success(commandService.get(commandId));
    }
}
