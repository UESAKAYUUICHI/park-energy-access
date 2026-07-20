package com.parkenergyaccess.repository;

import com.parkenergyaccess.entity.RawMessage;
import com.parkenergyaccess.enums.MqttMessageType;
import com.parkenergyaccess.enums.RawMessageStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcRawMessageRepository implements RawMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RawMessage> rowMapper = (rs, rowNum) -> new RawMessage(
            rs.getLong("id"),
            rs.getLong("gateway_id"),
            rs.getString("gateway_sn"),
            rs.getString("message_id"),
            rs.getString("topic"),
            MqttMessageType.UNKNOWN,
            rs.getString("payload"),
            rs.getTimestamp("receive_time").toInstant(),
            fromParseStatus(rs.getInt("parse_status"), rs.getString("fail_reason")),
            rs.getString("fail_reason")
    );

    public JdbcRawMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RawMessage save(RawMessage message) {
        if (message.gatewayId() == null) {
            return message;
        }
        if (message.id() > 0 && message.status() != RawMessageStatus.RECEIVED) {
            jdbcTemplate.update("""
                            update log_raw_message
                            set parse_status = ?, fail_reason = ?
                            where id = ?
                            """,
                    toParseStatus(message.status()), message.failReason(), message.id());
            return message;
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into log_raw_message
                      (gateway_id, message_id, topic, payload, receive_time, parse_status, fail_reason)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, message.gatewayId());
            ps.setString(2, message.messageId());
            ps.setString(3, message.topic());
            ps.setString(4, message.payload());
            ps.setTimestamp(5, Timestamp.from(message.receiveTime()));
            ps.setInt(6, toParseStatus(message.status()));
            ps.setString(7, message.failReason());
            return ps;
        }, keyHolder);
        Number id = keyHolder.getKey();
        return new RawMessage(id == null ? message.id() : id.longValue(), message.gatewayId(), message.gatewaySn(),
                message.messageId(), message.topic(), message.messageType(), message.payload(), message.receiveTime(),
                message.status(), message.failReason());
    }

    @Override
    public Optional<RawMessage> findByGatewayIdAndMessageId(Long gatewayId, String messageId) {
        List<RawMessage> results = jdbcTemplate.query("""
                        select r.id, r.gateway_id, g.gateway_sn, r.message_id, r.topic, r.payload,
                               r.receive_time, r.parse_status, r.fail_reason
                        from log_raw_message r
                        left join dev_gateway g on g.id = r.gateway_id
                        where r.gateway_id = ? and r.message_id = ?
                        limit 1
                        """,
                rowMapper, gatewayId, messageId);
        return results.stream().findFirst();
    }

    @Override
    public Optional<RawMessage> findById(long id) {
        List<RawMessage> results = jdbcTemplate.query("""
                        select r.id, r.gateway_id, g.gateway_sn, r.message_id, r.topic, r.payload,
                               r.receive_time, r.parse_status, r.fail_reason
                        from log_raw_message r
                        left join dev_gateway g on g.id = r.gateway_id
                        where r.id = ?
                        limit 1
                        """,
                rowMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public List<RawMessage> findLatest() {
        return jdbcTemplate.query("""
                        select r.id, r.gateway_id, g.gateway_sn, r.message_id, r.topic, r.payload,
                               r.receive_time, r.parse_status, r.fail_reason
                        from log_raw_message r
                        left join dev_gateway g on g.id = r.gateway_id
                        order by r.receive_time desc, r.id desc
                        limit 100
                        """,
                rowMapper);
    }

    @Override
    public List<RawMessage> findMqFailed(int limit) {
        return jdbcTemplate.query("""
                        select r.id, r.gateway_id, g.gateway_sn, r.message_id, r.topic, r.payload,
                               r.receive_time, r.parse_status, r.fail_reason
                        from log_raw_message r
                        left join dev_gateway g on g.id = r.gateway_id
                        where r.parse_status = 2
                          and r.gateway_id is not null
                          and r.fail_reason like 'MQ_FAILED:%'
                        order by r.receive_time asc, r.id asc
                        limit ?
                        """, rowMapper, limit);
    }

    private int toParseStatus(RawMessageStatus status) {
        return switch (status) {
            case RECEIVED -> 0;
            case FORWARDED, DUPLICATE -> 1;
            case MQ_FAILED, INVALID -> 2;
        };
    }

    private RawMessageStatus fromParseStatus(int parseStatus, String failReason) {
        if (parseStatus == 1) {
            return RawMessageStatus.FORWARDED;
        }
        if (parseStatus == 2) {
            return failReason != null && failReason.startsWith("MQ_FAILED:")
                    ? RawMessageStatus.MQ_FAILED
                    : RawMessageStatus.INVALID;
        }
        return RawMessageStatus.RECEIVED;
    }
}
