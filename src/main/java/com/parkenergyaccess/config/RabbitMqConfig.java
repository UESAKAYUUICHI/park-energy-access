package com.parkenergyaccess.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange parkEnergyExchange(RabbitMqProperties properties) {
        return new DirectExchange(properties.exchange(), true, false);
    }

    @Bean
    public Queue rawElectricDataQueue(RabbitMqProperties properties) {
        return new Queue(properties.rawDataQueue(), true);
    }

    @Bean
    public Binding rawElectricDataBinding(Queue rawElectricDataQueue, DirectExchange parkEnergyExchange,
                                          RabbitMqProperties properties) {
        return BindingBuilder.bind(rawElectricDataQueue)
                .to(parkEnergyExchange)
                .with(properties.rawDataRoutingKey());
    }
}
