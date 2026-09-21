package com.parkenergyaccess.repository;

import com.parkenergyaccess.entity.CommandRecord;
import com.parkenergyaccess.enums.CommandStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
@ConditionalOnMissingBean(CommandRecordRepository.class)
public class InMemoryCommandRecordRepository implements CommandRecordRepository {

    private final ConcurrentMap<String, CommandRecord> commands = new ConcurrentHashMap<>();

    @Override
    public CommandRecord save(CommandRecord record) {
        commands.put(record.commandId(), record);
        return record;
    }

    @Override
    public Optional<CommandRecord> findByCommandId(String commandId) {
        return Optional.ofNullable(commands.get(commandId));
    }

    @Override
    public long maxCommandSequence(String commandPrefix) {
        return commands.keySet().stream()
                .filter(commandId -> commandId.startsWith(commandPrefix))
                .map(commandId -> commandId.substring(commandPrefix.length()))
                .filter(suffix -> suffix.chars().allMatch(Character::isDigit))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0L);
    }

    @Override
    public List<CommandRecord> findLatest() {
        return commands.values().stream()
                .sorted(Comparator.comparing(CommandRecord::requestTime).reversed())
                .limit(100)
                .toList();
    }

    @Override
    public List<CommandRecord> findByStatus(CommandStatus status) {
        return commands.values().stream()
                .filter(command -> command.status() == status)
                .toList();
    }
}
