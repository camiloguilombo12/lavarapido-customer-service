package com.lavarapido.customer.infrastructure.adapter.in.web.dto;

import com.lavarapido.customer.domain.port.in.CustomerVehicleView;

import java.time.Instant;

/**
 * Respuesta de un vehiculo.
 *
 * Van las dos formas de la placa: licensePlate como se guarda (ABC123) y
 * licensePlateFormatted como se ve (ABC-123). El frontend arma el input por su cuenta, pero
 * mandarle las dos evita que cada pantalla invente su propio formato.
 */
public record VehicleResponse(
        long id,
        String licensePlate,
        String licensePlateFormatted,
        String vehicleType,
        short vehicleTypeId,
        String vehicleTypeName,
        String brand,
        String model,
        String color,
        Instant createdAt
) {

    public static VehicleResponse from(CustomerVehicleView view) {
        return new VehicleResponse(
                view.id(),
                view.licensePlate(),
                view.licensePlateFormatted(),
                view.vehicleType(),
                view.vehicleTypeId(),
                view.vehicleTypeName(),
                view.brand(),
                view.model(),
                view.color(),
                view.createdAt());
    }
}