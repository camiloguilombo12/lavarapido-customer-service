package com.lavarapido.customer.infrastructure.adapter.in.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia de RabbitMQ del servicio (ADR-004): la cola propia customer-service.events enlazada
 * al exchange compartido carwash.events, solo para el registro de usuarios.
 *
 * Los mensajes que fallan varias veces van a la DLQ para revisarlos en vez de perderse.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true")
class RabbitMessagingConfig {

    static final String EXCHANGE = "carwash.events";
    static final String QUEUE = "customer-service.events";
    static final String DEAD_LETTER_EXCHANGE = "carwash.events.dlx";
    static final String DEAD_LETTER_QUEUE = "customer-service.events.dlq";
    static final String USER_REGISTERED = "security.user_registered";

    @Bean
    Declarables customerTopology() {
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
        Queue deadLetterQueue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
        Queue queue = QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
        return new Declarables(exchange, deadLetterExchange, deadLetterQueue, queue,
                BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_QUEUE),
                BindingBuilder.bind(queue).to(exchange).with(USER_REGISTERED));
    }
}
