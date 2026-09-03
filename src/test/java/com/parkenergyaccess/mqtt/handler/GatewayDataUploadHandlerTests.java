package com.parkenergyaccess.mqtt.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.dto.GatewayUploadPayload;
import com.parkenergyaccess.dto.MeterPayload;
import com.parkenergyaccess.entity.GatewayArchive;
import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;
import com.parkenergyaccess.mq.RawDataProducer;
import com.parkenergyaccess.mq.message.AccessForwardMessage;
import com.parkenergyaccess.service.DeviceArchiveService;
import com.parkenergyaccess.service.DeviceStatusService;
import com.parkenergyaccess.service.DiscoveredDeviceService;
import com.parkenergyaccess.service.GatewayArchiveService;
import com.parkenergyaccess.service.GatewayStatusService;
import com.parkenergyaccess.service.RawMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Uses the same points-style payload produced by SimulatedGateway. */
@ExtendWith(MockitoExtension.class)
class GatewayDataUploadHandlerTests {
    @Mock private GatewayArchiveService gatewayArchiveService;
    @Mock private DeviceArchiveService deviceArchiveService;
    @Mock private DiscoveredDeviceService discoveredDeviceService;
    @Mock private GatewayStatusService gatewayStatusService;
    @Mock private DeviceStatusService deviceStatusService;
    @Mock private RawMessageService rawMessageService;
    @Mock private RawDataProducer rawDataProducer;

    @InjectMocks private GatewayDataUploadHandler handler;

    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void forwardsKnownMeterAndDiscoversUnknownMeterFromTheSameGatewayPayload() throws Exception {
        GatewayArchive gateway = new GatewayArchive(3L, "3", "GW-DEMO-003", true);
        RawMessage raw = new RawMessage(901L, 3L, "GW-DEMO-003", "MSG-3-1000",
                "gateway/3/data/upload", MqttMessageType.DATA_UPLOAD, "{}", Instant.now(), RawMessageStatus.RECEIVED, null, null);
        when(gatewayArchiveService.validateGateway("3", "GW-DEMO-003")).thenReturn(gateway);
        when(rawMessageService.isDuplicate(3L, "MSG-3-1000")).thenReturn(false);
        when(rawMessageService.saveInbound(eq(gateway), eq("MSG-3-1000"), anyString(), eq(MqttMessageType.DATA_UPLOAD), anyString()))
                .thenReturn(raw);
        when(deviceArchiveService.inspectMeter(eq(3L), any(MeterPayload.class)))
                .thenAnswer(invocation -> "METER-KNOWN".equals(invocation.getArgument(1, MeterPayload.class).deviceSn())
                        ? DeviceArchiveService.MeterValidation.valid()
                        : DeviceArchiveService.MeterValidation.rejected("Device not found or not bound to gateway"));

        handler.handle("3", "gateway/3/data/upload", simulatedMixedPayload());

        ArgumentCaptor<AccessForwardMessage> forwardCaptor = ArgumentCaptor.forClass(AccessForwardMessage.class);
        verify(rawDataProducer).publish(forwardCaptor.capture());
        GatewayUploadPayload forwarded = objectMapper.readValue(forwardCaptor.getValue().rawPayload(), GatewayUploadPayload.class);
        assertThat(forwarded.schemaVersion()).isEqualTo("1.0");
        assertThat(forwarded.meters()).extracting(MeterPayload::deviceSn).containsExactly("METER-KNOWN");
        verify(discoveredDeviceService).record(eq(3L), any(MeterPayload.class), eq(raw),
                eq("Device not found or not bound to gateway"));
        verify(rawMessageService).updateStatus(901L, RawMessageStatus.FORWARDED, null);
    }

    @Test
    void keepsUnknownOnlyPayloadAsRawEvidenceWithoutForwardingIt() throws Exception {
        GatewayArchive gateway = new GatewayArchive(3L, "3", "GW-DEMO-003", true);
        RawMessage raw = new RawMessage(902L, 3L, "GW-DEMO-003", "MSG-3-1001",
                "gateway/3/data/upload", MqttMessageType.DATA_UPLOAD, "{}", Instant.now(), RawMessageStatus.RECEIVED, null, null);
        when(gatewayArchiveService.validateGateway("3", "GW-DEMO-003")).thenReturn(gateway);
        when(rawMessageService.isDuplicate(3L, "MSG-3-1001")).thenReturn(false);
        when(rawMessageService.saveInbound(eq(gateway), eq("MSG-3-1001"), anyString(), eq(MqttMessageType.DATA_UPLOAD), anyString()))
                .thenReturn(raw);
        when(deviceArchiveService.inspectMeter(eq(3L), any(MeterPayload.class)))
                .thenReturn(DeviceArchiveService.MeterValidation.rejected("Device not found or not bound to gateway"));

        handler.handle("3", "gateway/3/data/upload", simulatedUnknownOnlyPayload());

        verify(discoveredDeviceService).record(eq(3L), any(MeterPayload.class), eq(raw), anyString());
        verify(rawDataProducer, never()).publish(any());
        verify(rawMessageService).updateStatus(eq(902L), eq(RawMessageStatus.INVALID), anyString());
        verify(gatewayStatusService, never()).markOnline(any());
        verify(deviceStatusService, never()).markMetersOnline(any());
    }

    private String simulatedMixedPayload() {
        return """
                {"schemaVersion":"1.0","messageId":"MSG-3-1000","gatewaySn":"GW-DEMO-003","timestamp":1786099356805,
                 "type":"DATA_UPLOAD","meters":[
                   {"deviceSn":"METER-KNOWN","modbusAddr":1,"collectTime":1786099356805,"points":{"voltage_a":221.4,"forward_active_energy":12843.27},"quality":0},
                   {"deviceSn":"METER-UNKNOWN","modbusAddr":2,"collectTime":1786099356805,"points":{"voltage_a":219.8,"forward_active_energy":456.78},"quality":0}
                 ]}
                """;
    }

    private String simulatedUnknownOnlyPayload() {
        return simulatedMixedPayload().replace("\"MSG-3-1000\"", "\"MSG-3-1001\"")
                .replace("{\"deviceSn\":\"METER-KNOWN\",\"modbusAddr\":1,\"collectTime\":1786099356805,\"points\":{\"voltage_a\":221.4,\"forward_active_energy\":12843.27},\"quality\":0},", "");
    }
}
