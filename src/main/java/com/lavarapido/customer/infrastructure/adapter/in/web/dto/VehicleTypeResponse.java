package com.lavarapido.customer.infrastructure.adapter.in.web.dto;

import com.lavarapido.customer.domain.model.VehicleType;

import java.math.BigDecimal;

/**
 * Respuesta de un tipo de vehiculo. Se manda sizeFactor porque el formulario de administracion
 * lo necesita para sugerir el precio de una tarifa nueva, no para calcular cobros.
 */
public record VehicleTypeResponse(
        short id,
        String code,
        String name,
        BigDecimal sizeFactor,
        short displayOrder
) {

    public static VehicleTypeResponse from(VehicleType type) {
        return new VehicleTypeResponse(
                type.vehicleTypeId(),
                type.code().name(),
                type.name(),
                type.sizeFactor(),
                type.displayOrder());
    }
}