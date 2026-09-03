package com.parkenergyaccess.mqtt;

import com.parkenergyaccess.config.MqttProperties;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

import java.nio.charset.StandardCharsets;

@Component
public class MqttConnectionManager implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttConnectionManager.class);

    private final MqttProperties properties;
    private final MqttMessageDispatcher dispatcher;
    private final Object connectionLock = new Object();
    private volatile MqttClient client;
    private long nextConnectAt;
    private long reconnectDelayMs = 5_000L;
    private static final long MAX_RECONNECT_DELAY_MS = 300_000L;

    public MqttConnectionManager(MqttProperties properties, MqttMessageDispatcher dispatcher) {
        this.properties = properties;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    public void connectIfNecessary() {
        if (!properties.inboundEnabled()) {
            return;
        }
        synchronized (connectionLock) {
            if (client != null && client.isConnected()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now < nextConnectAt) {
                return;
            }
            try {
                if (client == null) {
                    client = new MqttClient(properties.brokerUri(), properties.clientId(), new MemoryPersistence());
                    client.setCallback(this);
                }
                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(false);
                options.setCleanSession(true);
                options.setUserName(properties.username());
                options.setPassword(properties.password().toCharArray());
                client.connect(options);
                reconnectDelayMs = 5_000L;
                nextConnectAt = 0L;
                log.info("Connected to NanoMQ with clientId={}", properties.clientId());
            } catch (Exception ex) {
                nextConnectAt = now + reconnectDelayMs;
                log.warn("NanoMQ connection unavailable; retrying in {}s: {}", reconnectDelayMs / 1000, ex.getMessage());
                reconnectDelayMs = Math.min(reconnectDelayMs * 2, MAX_RECONNECT_DELAY_MS);
                closeClientAfterFailure();
            }
        }
    }

    private void closeClientAfterFailure() {
        if (client == null) {
            return;
        }
        try {
            client.close(true);
        } catch (MqttException ignored) {
            // The next attempt will create a fresh client if the old one cannot close cleanly.
        } finally {
            client = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (connectionLock) {
            closeClientAfterFailure();
        }
    }

    public void publish(String topic, byte[] payload) throws MqttException {
        if (client == null || !client.isConnected()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
        client.publish(topic, payload, properties.qos(), false);
    }

    /** Read-only status used by the operations dashboard. */
    public boolean isConnected() {
        MqttClient current = client;
        return current != null && current.isConnected();
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        MqttClient connectedClient = client;
        if (connectedClient == null || !connectedClient.isConnected()) {
            return;
        }
        try {
            connectedClient.subscribe("gateway/+/status/heartbeat", properties.qos());
            connectedClient.subscribe("gateway/+/data/upload", properties.qos());
            connectedClient.subscribe("gateway/+/cmd/response", properties.qos());
            connectedClient.subscribe("gateway/+/alarm/up", properties.qos());
            log.info("Subscribed NanoMQ topics from {}", serverURI);
        } catch (MqttException ex) {
            log.warn("Subscribe NanoMQ topics failed while connected: {}", ex.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        synchronized (connectionLock) {
            nextConnectAt = Math.max(nextConnectAt, System.currentTimeMillis() + reconnectDelayMs);
        }
        log.warn("NanoMQ connection lost; reconnect is backoff controlled: {}", cause == null ? "unknown" : cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        dispatcher.dispatch(topic, new String(message.getPayload(), StandardCharsets.UTF_8));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
