package com.lavarapido.customer.domain.model;

import com.lavarapido.customer.domain.exception.InvalidValueException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Los 6 tipos de vehiculo. Son los mismos que tiene el modal de registro del frontend
 * y los que siembra el changelog 102.
 *
 * Aca no va el precio ni el nombre: eso esta en la tabla vehicle_type y se puede cambiar
 * sin recompilar. Solo vive lo que es una regla, no un dato.
 */
public enum VehicleTypeCode {

    CAR,
    SEDAN,
    SUV,
    PICKUP,
    TRUCK,
    MOTO;

    /** Las motos en Colombia llevan 3 letras, 2 numeros y 1 letra. */
    public boolean isMotorcycle() {
        return this == MOTO;
    }

    public static VehicleTypeCode of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidValueException("INVALID_VEHICLE_TYPE", "Vehicle type is required");
        }
        String candidate = raw.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(code -> code.name().equals(candidate))
                .findFirst()
                .orElseThrow(() -> new InvalidValueException("INVALID_VEHICLE_TYPE",
                        "Vehicle type must be one of " + List.of(values())));
    }
}
