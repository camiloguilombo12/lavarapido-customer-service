package com.lavarapido.customer.infrastructure.adapter.in.messaging;

import com.lavarapido.customer.domain.port.in.ConsumeUserRegisteredUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Adaptador de entrada: security.user_registered crea el perfil de cliente.
 *
 * Solo las cuentas con rol CLIENT tienen perfil; el caso de uso es idempotente, asi que un
 * mensaje repetido no duplica nada. Un mensaje que no es JSON valido va directo a la DLQ.
 */
@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true")
class UserRegisteredListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredListener.class);

    private final ConsumeUserRegisteredUseCase consumer;
    private final JsonMapper json = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    UserRegisteredListener(ConsumeUserRegisteredUseCase consumer) {
        this.consumer = consumer;
    }

    @RabbitListener(queues = RabbitMessagingConfig.QUEUE)
    void onEvent(Message message) {
        Envelope event;
        try {
            event = json.readValue(message.getBody(), Envelope.class);
        } catch (JacksonException e) {
            log.warn("Message on {} is not a valid event envelope, sent to the DLQ", RabbitMessagingConfig.QUEUE);
            throw new AmqpRejectAndDontRequeueException("Invalid event envelope", e);
        }
        UserRegisteredPayload payload = event.payload();
        if (payload == null || payload.userId() == null || payload.personId() == null) {
            throw new AmqpRejectAndDontRequeueException("UserRegistered without userId or personId");
        }
        if (payload.roles() != null && !payload.roles().contains("CLIENT")) {
            return;
        }
        consumer.handle(payload.userId(), payload.personId());
    }

    /** Sobre comun de los eventos (ADR-004); solo se leen los campos que se usan. */
    record Envelope(String eventId, String eventType, UserRegisteredPayload payload) {
    }

    record UserRegisteredPayload(Long userId, Long personId, List<String> roles) {
    }
}
