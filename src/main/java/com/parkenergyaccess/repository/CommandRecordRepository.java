package com.parkenergyaccess.repository;

import com.parkenergyaccess.entity.CommandRecord;
import com.parkenergyaccess.enums.CommandStatus;

import java.util.List;
import java.util.Optional;

public interface CommandRecordRepository {

    CommandRecord save(CommandRecord record);

    Optional<CommandRecord> findByCommandId(String commandId);

    long maxCommandSequence(String commandPrefix);

    List<CommandRecord> findLatest();

    List<CommandRecord> findByStatus(CommandStatus status);
}
