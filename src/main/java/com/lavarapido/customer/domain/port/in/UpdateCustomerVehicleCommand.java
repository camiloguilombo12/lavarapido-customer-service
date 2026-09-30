package com.lavarapido.customer.domain.port.in;

/** Lo que llega del formulario de edicion, todavia sin validar. */
public record UpdateCustomerVehicleCommand(
        String licensePlate,
        String vehicleType,
        String brand,
        String model,
        String color
) {
}