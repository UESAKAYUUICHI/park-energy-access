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

import java.nio.charset.StandardCharsets;

@Component
public class MqttConnectionManager implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttConnectionManager.class);

    private final MqttProperties properties;
    private final MqttMessageDispatcher dispatcher;
    private MqttClient client;

    public MqttConnectionManager(MqttProperties properties, MqttMessageDispatcher dispatcher) {
        this.properties = properties;
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 1000)
    public void connectIfNecessary() {
        if (!properties.inboundEnabled()) {
            return;
        }
        try {
            if (client != null && client.isConnected()) {
                return;
            }
            client = new MqttClient(properties.brokerUri(), properties.clientId(), new MemoryPersistence());
            client.setCallback(this);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setUserName(properties.username());
            options.setPassword(properties.password().toCharArray());
            client.connect(options);
        } catch (Exception ex) {
            log.warn("NanoMQ is not ready: {}", ex.getMessage());
        }
    }

    public void publish(String topic, byte[] payload) throws MqttException {
        if (client == null || !client.isConnected()) {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
        client.publish(topic, payload, properties.qos(), false);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            client.subscribe("gateway/+/status/heartbeat", properties.qos());
            client.subscribe("gateway/+/data/upload", properties.qos());
            client.subscribe("gateway/+/cmd/response", properties.qos());
            log.info("Subscribed NanoMQ topics from {}", serverURI);
        } catch (MqttException ex) {
            log.warn("Subscribe NanoMQ topics failed: {}", ex.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("NanoMQ connection lost: {}", cause == null ? "unknown" : cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        dispatcher.dispatch(topic, new String(message.getPayload(), StandardCharsets.UTF_8));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
