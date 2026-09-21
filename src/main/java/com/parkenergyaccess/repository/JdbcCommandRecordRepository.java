package com.parkenergyaccess.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkenergyaccess.entity.CommandRecord;
import com.parkenergyaccess.enums.CommandStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcCommandRecordRepository implements CommandRecordRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcCommandRecordRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    private final RowMapper<CommandRecord> rowMapper = (rs, rowNum) -> {
        Long targetId = rs.getObject("target_id") == null ? null : rs.getLong("target_id");
        Long requestUserId = rs.getObject("request_user_id") == null ? null : rs.getLong("request_user_id");
        Timestamp sendTime = rs.getTimestamp("send_time");
        Timestamp responseTime = rs.getTimestamp("response_time");
        return new CommandRecord(
                rs.getLong("id"),
                rs.getString("command_id"),
                rs.getLong("gateway_id"),
                String.valueOf(rs.getLong("gateway_id")),
                rs.getString("target_type"),
                targetId,
                rs.getString("target_sn"),
                rs.getString("command_type"),
                readMap(rs.getString("command_payload")),
                "gateway/" + rs.getLong("gateway_id") + "/cmd/down",
                fromStatus(rs.getInt("status")),
                requestUserId,
                rs.getString("request_username"),
                rs.getTimestamp("request_time").toInstant(),
                sendTime == null ? null : sendTime.toInstant(),
                responseTime == null ? null : responseTime.toInstant(),
                readMap(rs.getString("response_payload")),
                rs.getString("fail_reason")
        );
    };

    @Override
    public CommandRecord save(CommandRecord record) {
        Optional<CommandRecord> existing = findByCommandId(record.commandId());
        if (existing.isPresent()) {
            jdbcTemplate.update("""
                            update command_record
                            set status = ?, send_time = ?, response_time = ?, response_payload = ?, fail_reason = ?
                            where command_id = ?
                            """,
                    toStatus(record.status()),
                    record.sendTime() == null ? null : Timestamp.from(record.sendTime()),
                    record.responseTime() == null ? null : Timestamp.from(record.responseTime()),
                    writeMap(record.responsePayload()),
                    record.failReason(),
                    record.commandId());
            return record;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into command_record
                      (command_id, gateway_id, target_type, target_id, target_sn, command_type, command_payload,
                       status, request_user_id, request_username, request_time, send_time, response_time,
                       response_payload, fail_reason)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, record.commandId());
            ps.setLong(2, record.gatewayId());
            ps.setString(3, record.targetType());
            if (record.targetId() == null) {
                ps.setObject(4, null);
            } else {
                ps.setLong(4, record.targetId());
            }
            ps.setString(5, record.targetSn());
            ps.setString(6, record.commandType());
            ps.setString(7, writeMap(record.commandPayload()));
            ps.setInt(8, toStatus(record.status()));
            if (record.requestUserId() == null) {
                ps.setObject(9, null);
            } else {
                ps.setLong(9, record.requestUserId());
            }
            ps.setString(10, record.requestUsername());
            ps.setTimestamp(11, Timestamp.from(record.requestTime()));
            ps.setTimestamp(12, record.sendTime() == null ? null : Timestamp.from(record.sendTime()));
            ps.setTimestamp(13, record.responseTime() == null ? null : Timestamp.from(record.responseTime()));
            ps.setString(14, writeMap(record.responsePayload()));
            ps.setString(15, record.failReason());
            return ps;
        }, keyHolder);
        Number id = keyHolder.getKey();
        return new CommandRecord(id == null ? record.id() : id.longValue(), record.commandId(), record.gatewayId(),
                record.topicGatewayId(), record.targetType(), record.targetId(), record.targetSn(),
                record.commandType(), record.commandPayload(), record.mqttTopic(), record.status(),
                record.requestUserId(), record.requestUsername(), record.requestTime(), record.sendTime(),
                record.responseTime(), record.responsePayload(), record.failReason());
    }

    @Override
    public Optional<CommandRecord> findByCommandId(String commandId) {
        List<CommandRecord> results = jdbcTemplate.query("""
                        select id, command_id, gateway_id, target_type, target_id, target_sn, command_type,
                               command_payload, status, request_user_id, request_username, request_time, send_time,
                               response_time, response_payload, fail_reason
                        from command_record
                        where command_id = ?
                        limit 1
                        """,
                rowMapper, commandId);
        return results.stream().findFirst();
    }

    @Override
    public long maxCommandSequence(String commandPrefix) {
        Integer suffixStart = commandPrefix.length() + 1;
        String pattern = "^" + commandPrefix + "[0-9]+$";
        Long value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(command_id, ?) AS UNSIGNED)), 0)
                FROM command_record
                WHERE command_id LIKE ? AND command_id REGEXP ?
                """, Long.class, suffixStart, commandPrefix + "%", pattern);
        return value == null ? 0L : value;
    }

    @Override
    public List<CommandRecord> findLatest() {
        return jdbcTemplate.query("""
                        select id, command_id, gateway_id, target_type, target_id, target_sn, command_type,
                               command_payload, status, request_user_id, request_username, request_time, send_time,
                               response_time, response_payload, fail_reason
                        from command_record
                        order by request_time desc, id desc
                        limit 100
                        """,
                rowMapper);
    }

    @Override
    public List<CommandRecord> findByStatus(CommandStatus status) {
        return jdbcTemplate.query("""
                        select id, command_id, gateway_id, target_type, target_id, target_sn, command_type,
                               command_payload, status, request_user_id, request_username, request_time, send_time,
                               response_time, response_payload, fail_reason
                        from command_record
                        where status = ?
                        """,
                rowMapper, toStatus(status));
    }

    private int toStatus(CommandStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case SENT -> 1;
            case SUCCESS -> 2;
            case FAILED -> 3;
            case TIMEOUT -> 4;
        };
    }

    private CommandStatus fromStatus(int status) {
        return switch (status) {
            case 1 -> CommandStatus.SENT;
            case 2 -> CommandStatus.SUCCESS;
            case 3 -> CommandStatus.FAILED;
            case 4 -> CommandStatus.TIMEOUT;
            default -> CommandStatus.PENDING;
        };
    }

    private Map<String, Object> readMap(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(text, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            return Map.of("_raw", text);
        }
    }

    private String writeMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
