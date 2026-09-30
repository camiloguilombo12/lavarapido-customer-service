package com.lavarapido.customer.domain.model;

import java.math.BigDecimal;

/**
 * Una fila del catalogo de tipos de vehiculo: el codigo, el nombre para mostrar y el factor
 * de tamaño.
 *
 * El sizeFactor no sirve para calcular cobros. El precio sale siempre de catalog.service_price;
 * este numero es solo la sugerencia que ve el administrador cuando crea una tarifa.
 */
public record VehicleType(
        short vehicleTypeId,
        VehicleTypeCode code,
        String name,
        BigDecimal sizeFactor,
        short displayOrder,
        boolean active
) {

    /** Un tipo que ya no se ofrece: se puede leer, pero no sale en los formularios nuevos. */
    public boolean isSelectable() {
        return active;
    }
}
