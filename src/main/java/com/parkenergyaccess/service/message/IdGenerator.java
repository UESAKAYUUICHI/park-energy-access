package com.parkenergyaccess.service.message;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class IdGenerator {

    private static final DateTimeFormatter COMMAND_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private final AtomicLong rawMessageId = new AtomicLong(1);
    private final AtomicLong commandSequence = new AtomicLong(1);

    public long nextRawMessageId() {
        return rawMessageId.getAndIncrement();
    }

    public long nextCommandRecordId() {
        return commandSequence.getAndIncrement();
    }

    public String commandPrefix() {
        return "CMD-" + LocalDate.now().format(COMMAND_DATE_FORMATTER) + "-";
    }

    public synchronized String nextCommandId(long latestPersistedSequence) {
        commandSequence.updateAndGet(current -> Math.max(current, latestPersistedSequence + 1));
        return commandPrefix() + String.format("%04d", commandSequence.getAndIncrement());
    }
}
