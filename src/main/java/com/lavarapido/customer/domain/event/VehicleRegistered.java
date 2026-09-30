package com.lavarapido.customer.domain.event;

import java.time.Instant;

/** Un cliente registro un vehiculo. */
public record VehicleRegistered(long customerId, long vehicleId, String licensePlate, String vehicleType,
                                Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "customer.vehicle.registered";
    }
}