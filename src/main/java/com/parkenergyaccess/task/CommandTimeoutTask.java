package com.parkenergyaccess.task;

import com.parkenergyaccess.config.MqttProperties;
import com.parkenergyaccess.service.CommandService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CommandTimeoutTask {

    private final CommandService commandService;
    private final MqttProperties mqttProperties;

    public CommandTimeoutTask(CommandService commandService, MqttProperties mqttProperties) {
        this.commandService = commandService;
        this.mqttProperties = mqttProperties;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void markTimeoutCommands() {
        commandService.markTimeoutCommands(mqttProperties.commandTimeoutSeconds());
    }
}
