package com.parkenergyaccess.mqtt;

import com.parkenergyaccess.enums.MqttMessageType;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MqttTopicParser {

    private static final Pattern TOPIC_PATTERN = Pattern.compile("^gateway/([^/]+)/(data/upload|status/heartbeat|cmd/response)$");

    public ParsedTopic parse(String topic) {
        Matcher matcher = TOPIC_PATTERN.matcher(topic);
        if (!matcher.matches()) {
            return new ParsedTopic(topic, null, MqttMessageType.UNKNOWN);
        }
        String gatewayId = matcher.group(1);
        String suffix = matcher.group(2);
        MqttMessageType type = switch (suffix) {
            case "data/upload" -> MqttMessageType.DATA_UPLOAD;
            case "status/heartbeat" -> MqttMessageType.HEARTBEAT;
            case "cmd/response" -> MqttMessageType.COMMAND_RESPONSE;
            default -> MqttMessageType.UNKNOWN;
        };
        return new ParsedTopic(topic, gatewayId, type);
    }

    public record ParsedTopic(String topic, String topicGatewayId, MqttMessageType messageType) {
    }
}
