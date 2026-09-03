package com.parkenergyaccess.service;

import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;
import com.parkenergyaccess.mq.RawDataProducer;
import com.parkenergyaccess.mq.message.AccessForwardMessage;
import com.parkenergyaccess.repository.RawMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class RawMessageServiceTests {
    @Mock private RawMessageRepository repository;
    @Mock private RawDataProducer rawDataProducer;

    @Test
    void replayUsesANewForwardMessageIdWhileKeepingTheOriginalRawLogReference() throws Exception {
        RawMessage raw = new RawMessage(901L, 3L, "GW-DEMO-003", "MSG-3-1000",
                "gateway/3/data/upload", MqttMessageType.DATA_UPLOAD, "{\"meters\":[]}", Instant.now(),
                RawMessageStatus.INVALID, "DEVICE_SAMPLE_REJECTED", "unknown device");
        when(repository.findById(901L)).thenReturn(Optional.of(raw));
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        RawMessageService service = new RawMessageService(repository, new IdGenerator(), rawDataProducer);

        service.replay(901L);

        ArgumentCaptor<AccessForwardMessage> forward = ArgumentCaptor.forClass(AccessForwardMessage.class);
        verify(rawDataProducer).publish(forward.capture());
        assertThat(forward.getValue().rawLogId()).isEqualTo(901L);
        assertThat(forward.getValue().messageId()).startsWith("REPLAY-901-");
        assertThat(forward.getValue().rawPayload()).isEqualTo("{\"meters\":[]}");
    }

    @Test
    void forwardedMessageClearsFailureMetadata() {
        RawMessage raw = new RawMessage(902L, 3L, "GW-DEMO-003", "MSG-3-1001",
                "gateway/3/data/upload", MqttMessageType.DATA_UPLOAD, "{}", Instant.now(),
                RawMessageStatus.RECEIVED, null, null);
        when(repository.findById(902L)).thenReturn(Optional.of(raw));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RawMessageService service = new RawMessageService(repository, new IdGenerator(), rawDataProducer);

        RawMessage saved = service.updateStatus(902L, RawMessageStatus.FORWARDED, null);

        assertThat(saved.status()).isEqualTo(RawMessageStatus.FORWARDED);
        assertThat(saved.failCode()).isNull();
        assertThat(saved.failReason()).isNull();
    }
}
