package com.lavarapido.customer.domain.port.in;

/** Borra un vehiculo de forma logica. */
public interface RemoveCustomerVehicleUseCase {

    void execute(long userId, long vehicleId);
}