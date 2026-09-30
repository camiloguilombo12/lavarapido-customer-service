package com.lavarapido.customer.domain.event;

import java.time.Instant;

/**
 * Se creo la fila de customer a partir del evento security.user.registered.
 * booking-service lo escucha para saber a quien pertenece un vehiculo sin llamar a este
 * servicio en cada lectura.
 */
public record CustomerProvisioned(long customerId, long personId, long userId, Instant occurredAt)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "customer.provisioned";
    }
}