package com.lavarapido.customer.domain.port.in;

/** Lo que llega del formulario de registro, todavia sin validar. */
public record RegisterCustomerVehicleCommand(
        String licensePlate,
        String vehicleType,
        String brand,
        String model,
        String color
) {
}