package com.parkenergyaccess.service;

import com.parkenergyaccess.common.BusinessException;
import com.parkenergyaccess.common.ErrorCode;
import com.parkenergyaccess.dto.CommandCreateRequest;
import com.parkenergyaccess.dto.CommandResponsePayload;
import com.parkenergyaccess.entity.CommandRecord;
import com.parkenergyaccess.enums.CommandStatus;
import com.parkenergyaccess.mqtt.publisher.CommandMqttPublisher;
import com.parkenergyaccess.repository.CommandRecordRepository;
import com.parkenergyaccess.vo.CommandRecordVO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommandService {

    private final CommandRecordRepository repository;
    private final IdGenerator idGenerator;
    private final CommandMqttPublisher publisher;
    private final GatewayArchiveService gatewayArchiveService;

    public CommandService(CommandRecordRepository repository, IdGenerator idGenerator, CommandMqttPublisher publisher,
                          GatewayArchiveService gatewayArchiveService) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.publisher = publisher;
        this.gatewayArchiveService = gatewayArchiveService;
    }

    public CommandRecordVO sendCommand(CommandCreateRequest request) {
        gatewayArchiveService.validateGatewayId(request.gatewayId());
        String commandId = nextUniqueCommandId();
        String topicGatewayId = request.topicGatewayId() == null || request.topicGatewayId().isBlank()
                ? String.valueOf(request.gatewayId())
                : request.topicGatewayId();
        String topic = "gateway/" + topicGatewayId + "/cmd/down";
        Instant now = Instant.now();
        CommandRecord pending = new CommandRecord(idGenerator.nextCommandRecordId(), commandId, request.gatewayId(),
                topicGatewayId, request.targetType(), request.targetId(), request.targetSn(), request.commandType(),
                request.commandPayload(), topic, CommandStatus.PENDING, request.requestUserId(),
                request.requestUsername(), now, null, null, null, null);
        pending = repository.save(pending);

        try {
            publisher.publish(topic, buildCommandBody(pending));
            CommandRecord sent = pending.withStatus(CommandStatus.SENT, Instant.now(), null, null, null);
            sent = repository.save(sent);
            return toVO(sent);
        } catch (Exception ex) {
            CommandRecord failed = pending.withStatus(CommandStatus.FAILED, null, null, null, ex.getMessage());
            failed = repository.save(failed);
            return toVO(failed);
        }
    }

    private String nextUniqueCommandId() {
        long latest = repository.maxCommandSequence(idGenerator.commandPrefix());
        for (int attempt = 0; attempt < 10; attempt++) {
            String commandId = idGenerator.nextCommandId(latest);
            if (repository.findByCommandId(commandId).isEmpty()) {
                return commandId;
            }
            latest++;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "command id generation conflict");
    }

    public void handleResponse(String topicGatewayId, CommandResponsePayload response, Map<String, Object> rawResponse) {
        if (response.commandId() == null || response.commandId().isBlank()) {
            return;
        }
        repository.findByCommandId(response.commandId()).ifPresent(command -> {
            CommandStatus nextStatus = "FAILED".equalsIgnoreCase(response.status())
                    ? CommandStatus.FAILED
                    : CommandStatus.SUCCESS;
            repository.save(command.withStatus(nextStatus, command.sendTime(), Instant.now(), rawResponse,
                    nextStatus == CommandStatus.FAILED ? response.message() : null));
        });
    }

    public int markTimeoutCommands(int timeoutSeconds) {
        Instant now = Instant.now();
        List<CommandRecord> sentCommands = repository.findByStatus(CommandStatus.SENT);
        int updated = 0;
        for (CommandRecord command : sentCommands) {
            if (command.sendTime() != null && Duration.between(command.sendTime(), now).toSeconds() >= timeoutSeconds) {
                repository.save(command.withStatus(CommandStatus.TIMEOUT, command.sendTime(), null, null,
                        "command response timeout"));
                updated++;
            }
        }
        return updated;
    }

    public CommandRecordVO get(String commandId) {
        return repository.findByCommandId(commandId)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMAND_NOT_FOUND, "command not found"));
    }

    public List<CommandRecordVO> latest() {
        return repository.findLatest().stream().map(this::toVO).toList();
    }

    private Map<String, Object> buildCommandBody(CommandRecord command) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("commandId", command.commandId());
        body.put("targetType", command.targetType());
        body.put("targetId", command.targetId());
        body.put("targetSn", command.targetSn());
        body.put("commandType", command.commandType());
        body.put("payload", command.commandPayload() == null ? Map.of() : command.commandPayload());
        return body;
    }

    private CommandRecordVO toVO(CommandRecord command) {
        return new CommandRecordVO(command.id(), command.commandId(), command.gatewayId(), command.targetType(),
                command.targetId(), command.targetSn(), command.commandType(), command.mqttTopic(), command.status(),
                command.requestTime(), command.sendTime(), command.responseTime(), command.responsePayload(),
                command.failReason());
    }
}
