package com.parkenergyaccess.mq.message;

public record AlarmProcessingReceipt(
        Long gatewayId,
        String messageId,
        String eventId,
        String status,
        String error,
        long processedAt
) {}
