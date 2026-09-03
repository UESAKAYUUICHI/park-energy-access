package com.parkenergyaccess.mqtt;

import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.mqtt.handler.CommandResponseHandler;
import com.parkenergyaccess.mqtt.handler.GatewayDataUploadHandler;
import com.parkenergyaccess.mqtt.handler.GatewayHeartbeatHandler;
import com.parkenergyaccess.mqtt.handler.GatewayAlarmUploadHandler;
import org.springframework.stereotype.Component;

@Component
public class MqttMessageDispatcher {

    private final MqttTopicParser topicParser;
    private final GatewayHeartbeatHandler heartbeatHandler;
    private final GatewayDataUploadHandler dataUploadHandler;
    private final CommandResponseHandler commandResponseHandler;
    private final GatewayAlarmUploadHandler alarmUploadHandler;

    public MqttMessageDispatcher(MqttTopicParser topicParser, GatewayHeartbeatHandler heartbeatHandler,
                                 GatewayDataUploadHandler dataUploadHandler,
                                 CommandResponseHandler commandResponseHandler,
                                 GatewayAlarmUploadHandler alarmUploadHandler) {
        this.topicParser = topicParser;
        this.heartbeatHandler = heartbeatHandler;
        this.dataUploadHandler = dataUploadHandler;
        this.commandResponseHandler = commandResponseHandler;
        this.alarmUploadHandler = alarmUploadHandler;
    }

    public void dispatch(String topic, String payload) {
        MqttTopicParser.ParsedTopic parsed = topicParser.parse(topic);
        if (parsed.messageType() == MqttMessageType.HEARTBEAT) {
            heartbeatHandler.handle(parsed.topicGatewayId(), topic, payload);
        } else if (parsed.messageType() == MqttMessageType.DATA_UPLOAD) {
            dataUploadHandler.handle(parsed.topicGatewayId(), topic, payload);
        } else if (parsed.messageType() == MqttMessageType.COMMAND_RESPONSE) {
            commandResponseHandler.handle(parsed.topicGatewayId(), topic, payload);
        } else if (parsed.messageType() == MqttMessageType.ALARM_UPLOAD) {
            alarmUploadHandler.handle(parsed.topicGatewayId(), topic, payload);
        }
    }
}
