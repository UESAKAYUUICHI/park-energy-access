package com.parkenergyaccess.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.config.RabbitMqProperties;
import com.parkenergyaccess.mq.message.AccessForwardMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RawDataProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties properties;
    private final ObjectMapper objectMapper;

    public RawDataProducer(RabbitTemplate rabbitTemplate, RabbitMqProperties properties, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void publish(AccessForwardMessage message) throws JsonProcessingException {
        rabbitTemplate.convertAndSend(properties.exchange(), properties.rawDataRoutingKey(),
                objectMapper.writeValueAsString(message));
    }
}
