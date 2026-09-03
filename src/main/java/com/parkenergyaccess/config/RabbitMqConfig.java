package com.parkenergyaccess.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitMqConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public DirectExchange parkEnergyExchange(RabbitMqProperties properties) {
        return new DirectExchange(properties.exchange(), true, false);
    }

    @Bean
    public Queue rawElectricDataQueue(RabbitMqProperties properties) {
        return new Queue(properties.rawDataQueue(), true);
    }

    @Bean
    public Queue rawElectricDataRetryQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.retryQueue())
                .withArgument("x-message-ttl", properties.retryDelayMilliseconds())
                .withArgument("x-dead-letter-exchange", properties.exchange())
                .withArgument("x-dead-letter-routing-key", properties.rawDataRoutingKey())
                .build();
    }

    @Bean
    public Queue rawElectricDataDeadLetterQueue(RabbitMqProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    public Queue alarmResultQueue(RabbitMqProperties properties) {
        return new Queue(properties.resultQueue(), true);
    }

    @Bean
    public Queue gatewayAlarmQueue(RabbitMqProperties properties) {
        return new Queue(properties.alarmQueue(), true);
    }

    @Bean
    public Binding rawElectricDataBinding(@Qualifier("rawElectricDataQueue") Queue rawElectricDataQueue, DirectExchange parkEnergyExchange,
                                          RabbitMqProperties properties) {
        return BindingBuilder.bind(rawElectricDataQueue)
                .to(parkEnergyExchange)
                .with(properties.rawDataRoutingKey());
    }

    @Bean
    public Binding rawElectricDataRetryBinding(@Qualifier("rawElectricDataRetryQueue") Queue rawElectricDataRetryQueue, DirectExchange parkEnergyExchange,
                                               RabbitMqProperties properties) {
        return BindingBuilder.bind(rawElectricDataRetryQueue).to(parkEnergyExchange).with(properties.retryRoutingKey());
    }

    @Bean
    public Binding rawElectricDataDeadLetterBinding(@Qualifier("rawElectricDataDeadLetterQueue") Queue rawElectricDataDeadLetterQueue, DirectExchange parkEnergyExchange,
                                                    RabbitMqProperties properties) {
        return BindingBuilder.bind(rawElectricDataDeadLetterQueue).to(parkEnergyExchange).with(properties.deadLetterRoutingKey());
    }

    @Bean
    public Binding alarmResultBinding(@Qualifier("alarmResultQueue") Queue alarmResultQueue,
                                      DirectExchange parkEnergyExchange, RabbitMqProperties properties) {
        return BindingBuilder.bind(alarmResultQueue).to(parkEnergyExchange).with(properties.resultRoutingKey());
    }

    @Bean
    public Binding gatewayAlarmBinding(@Qualifier("gatewayAlarmQueue") Queue gatewayAlarmQueue,
                                       DirectExchange parkEnergyExchange, RabbitMqProperties properties) {
        return BindingBuilder.bind(gatewayAlarmQueue).to(parkEnergyExchange).with(properties.alarmRoutingKey());
    }
}
