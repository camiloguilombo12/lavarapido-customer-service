package com.lavarapido.customer.domain.exception;

/** El vehiculo no existe, o no es del cliente que esta llamando. */
public class VehicleNotFoundException extends DomainException {

    public VehicleNotFoundException(long vehicleId) {
        super("VEHICLE_NOT_FOUND", "Vehicle " + vehicleId + " does not exist");
    }
}