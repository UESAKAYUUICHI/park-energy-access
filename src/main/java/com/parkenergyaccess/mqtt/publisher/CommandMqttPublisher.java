package com.parkenergyaccess.mqtt.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.mqtt.MqttConnectionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommandMqttPublisher {

    private final ObjectMapper objectMapper;
    private final ObjectProvider<MqttConnectionManager> connectionManagerProvider;

    public CommandMqttPublisher(ObjectMapper objectMapper, ObjectProvider<MqttConnectionManager> connectionManagerProvider) {
        this.objectMapper = objectMapper;
        this.connectionManagerProvider = connectionManagerProvider;
    }

    public void publish(String topic, Map<String, Object> commandBody) throws Exception {
        connectionManagerProvider.getObject().publish(topic, objectMapper.writeValueAsBytes(commandBody));
    }
}
