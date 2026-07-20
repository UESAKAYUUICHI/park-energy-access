package com.parkenergyaccess.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "park.mqtt")
public record MqttProperties(
        boolean inboundEnabled,
        String host,
        int port,
        String username,
        String password,
        String clientId,
        int qos,
        int commandTimeoutSeconds
) {

    public String brokerUri() {
        return "tcp://" + host + ":" + port;
    }
}
