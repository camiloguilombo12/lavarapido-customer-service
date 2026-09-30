package com.lavarapido.customer.domain.port.in;

import java.time.Instant;

/**
 * El vehiculo como lo ve el controlador. El CustomerAccount no sale del dominio: los
 * adaptadores traduzcan esto, no al revés.
 *
 * El tipo viene plano (codigo, id y nombre) y no como objeto, porque quien lo lee es la
 * pantalla y no necesita el factor de tamaño.
 */
public record CustomerVehicleView(
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
}