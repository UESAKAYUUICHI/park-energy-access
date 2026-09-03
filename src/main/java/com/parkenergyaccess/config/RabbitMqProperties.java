package com.parkenergyaccess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "park.rabbitmq")
public record RabbitMqProperties(String exchange, String rawDataQueue, String rawDataRoutingKey,
                                 String alarmQueue, String alarmRoutingKey) {
    public String retryQueue() { return rawDataQueue + ".retry"; }
    public String retryRoutingKey() { return rawDataRoutingKey + ".retry"; }
    public String deadLetterQueue() { return rawDataQueue + ".dlq"; }
    public String deadLetterRoutingKey() { return rawDataRoutingKey + ".dlq"; }
    public int retryDelayMilliseconds() { return 30_000; }
    public String resultQueue() { return rawDataQueue + ".result"; }
    public String resultRoutingKey() { return rawDataRoutingKey + ".result"; }
}
