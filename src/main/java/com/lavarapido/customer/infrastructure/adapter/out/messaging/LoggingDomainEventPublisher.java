package com.lavarapido.customer.infrastructure.adapter.out.messaging;

import com.lavarapido.customer.domain.event.DomainEvent;
import com.lavarapido.customer.domain.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publica los eventos, pero por ahora solo los escribe en el log.
 *
 * Es el mismo punto de entrada que usara el broker. Cuando se conecte, se cambia este adaptador
 * por uno que publique al bus y ni el dominio ni los casos de uso se enteran. Por eso los
 * eventos solo llevan ids, que es lo que viaja bien entre servicios, y no objetos del dominio.
 */
@Component
class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(event -> log.info("Domain event {} at {}: {}",
                event.eventType(), event.occurredAt(), event));
    }
}
