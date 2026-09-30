package com.lavarapido.customer.domain.port.out;

import com.lavarapido.customer.domain.event.DomainEvent;

import java.util.List;

/**
 * Publica los eventos. Hoy el adaptador solo los escribe en el log; cuando llegue el broker
 * se cambia ese adaptador y ni el dominio ni los casos de uso se enteran.
 */
public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}