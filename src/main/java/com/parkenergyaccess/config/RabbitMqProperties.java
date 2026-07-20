package com.parkenergyaccess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "park.rabbitmq")
public record RabbitMqProperties(String exchange, String rawDataQueue, String rawDataRoutingKey) {
}
