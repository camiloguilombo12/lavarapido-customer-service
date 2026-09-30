package com.lavarapido.customer.domain.event;

import java.time.Instant;

/** Un cliente borro un vehiculo. Es borrado logico, asi que el evento dice que ya no esta activo. */
public record VehicleRemoved(long customerId, long vehicleId, String licensePlate, Instant occurredAt)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "customer.vehicle.removed";
    }
}