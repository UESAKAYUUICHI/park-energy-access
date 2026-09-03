package com.parkenergyaccess.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RabbitMqStartupInitializer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMqStartupInitializer.class);

    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange exchange;
    private final List<Queue> queues;
    private final List<Binding> bindings;

    public RabbitMqStartupInitializer(RabbitAdmin rabbitAdmin, RabbitTemplate rabbitTemplate,
                                      DirectExchange exchange, List<Queue> queues, List<Binding> bindings) {
        this.rabbitAdmin = rabbitAdmin;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.queues = queues;
        this.bindings = bindings;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeRabbitMq() {
        rabbitAdmin.declareExchange(exchange);
        queues.forEach(rabbitAdmin::declareQueue);
        bindings.forEach(rabbitAdmin::declareBinding);
        rabbitTemplate.execute(channel -> null);
        log.info("RabbitMQ topology declared and publisher connection warmed up");
    }
}
