package com.parkenergyaccess.task;
import com.parkenergyaccess.service.message.RawMessageService;

import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.enums.RawMessageStatus;
import com.parkenergyaccess.mq.RawDataProducer;
import com.parkenergyaccess.mq.message.AccessForwardMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RawMessageRetryTask {
    private static final Logger log = LoggerFactory.getLogger(RawMessageRetryTask.class);

    private final RawMessageService rawMessageService;
    private final RawDataProducer rawDataProducer;

    public RawMessageRetryTask(RawMessageService rawMessageService, RawDataProducer rawDataProducer) {
        this.rawMessageService = rawMessageService;
        this.rawDataProducer = rawDataProducer;
    }

    @Scheduled(fixedDelayString = "${park.rabbitmq.retry-delay-ms:30000}")
    public void retryMqFailedMessages() {
        for (RawMessage raw : rawMessageService.pendingForward(20)) {
            try {
                rawDataProducer.publish(new AccessForwardMessage(raw.id(), raw.messageId(), raw.gatewayId(),
                        raw.gatewaySn(), raw.topic().endsWith("/alarm/up") ? "ALARM_UPLOAD" : "DATA_UPLOAD",
                        raw.payload(), raw.receiveTime()));
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.FORWARDED, null);
                log.info("Forwarded pending raw message successfully, rawLogId={}", raw.id());
            } catch (Exception ex) {
                rawMessageService.updateStatus(raw.id(), RawMessageStatus.MQ_FAILED, ex.getMessage());
                log.warn("Forward pending raw message failed, rawLogId={}", raw.id(), ex);
            }
        }
    }
}
