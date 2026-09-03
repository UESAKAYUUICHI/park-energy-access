package com.parkenergyaccess.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.mq.message.AlarmProcessingReceipt;
import com.parkenergyaccess.mqtt.publisher.CommandMqttPublisher;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AlarmResultConsumer {
    private final ObjectMapper objectMapper;
    private final CommandMqttPublisher mqttPublisher;

    public AlarmResultConsumer(ObjectMapper objectMapper, CommandMqttPublisher mqttPublisher) {
        this.objectMapper = objectMapper;
        this.mqttPublisher = mqttPublisher;
    }

    @RabbitListener(queues = "#{@alarmResultQueue.name}")
    public void consume(String body) throws Exception {
        AlarmProcessingReceipt receipt = objectMapper.readValue(body, AlarmProcessingReceipt.class);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", receipt.messageId());
        payload.put("eventId", receipt.eventId());
        payload.put("status", receipt.status());
        payload.put("error", receipt.error());
        payload.put("processedAt", receipt.processedAt());
        mqttPublisher.publish("gateway/" + receipt.gatewayId() + "/alarm/ack", payload);
    }
}
