package com.lavarapido.customer.domain.event;

import java.time.Instant;

/** Un cliente edito los datos de su vehiculo. */
public record VehicleUpdated(long customerId, long vehicleId, String licensePlate, String vehicleType,
                             Instant occurredAt) implements DomainEvent {

    @Override
    public String eventType() {
        return "customer.vehicle.updated";
    }
}