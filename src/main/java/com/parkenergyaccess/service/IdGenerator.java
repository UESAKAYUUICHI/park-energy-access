package com.parkenergyaccess.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class IdGenerator {

    private final AtomicLong rawMessageId = new AtomicLong(1);
    private final AtomicLong commandSequence = new AtomicLong(1);

    public long nextRawMessageId() {
        return rawMessageId.getAndIncrement();
    }

    public long nextCommandRecordId() {
        return commandSequence.getAndIncrement();
    }

    public String nextCommandId() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "CMD-" + date + "-" + String.format("%04d", commandSequence.getAndIncrement());
    }
}
