package com.lavarapido.customer.domain.service;

import com.lavarapido.customer.domain.exception.InvalidValueException;
import com.lavarapido.customer.domain.model.LicensePlate;
import com.lavarapido.customer.domain.model.VehicleTypeCode;

/**
 * Formato de la placa colombiana. Ojo que el formato cambia segun el vehiculo:
 * carro y camionetas son ABC123 (3 letras, 3 numeros).
 * las motos son ABC12D (3 letras, 2 numeros, 1 letra).
 *
 * La regla va acá y no en la base ni en el formulario, porque es del negocio.
 * El frontend valida lo mismo para responder rapido, pero el que manda es esto.
 */
public class LicensePlatePolicy {

    private static final String CAR_LIKE = "^[A-Z]{3}[0-9]{3}$";
    private static final String MOTORCYCLE = "^[A-Z]{3}[0-9]{2}[A-Z]$";

    /**
     * Revisa la placa segun el tipo y la devuelve igual, o lanza si no corresponde.
     *
     * @throws InvalidValueException con el codigo INVALID_PLATE si el formato no es el del tipo
     */
    public LicensePlate validate(LicensePlate plate, VehicleTypeCode type) {
        String expected = type.isMotorcycle() ? MOTORCYCLE : CAR_LIKE;

        if (!plate.matches(expected)) {
            throw new InvalidValueException("INVALID_PLATE", type.isMotorcycle()
                    ? "A motorcycle plate must be 3 letters, 2 digits and 1 letter (ABC12D)"
                    : "A plate must be 3 letters followed by 3 digits (ABC123)");
        }
        return plate;
    }

    /** El formato que espera este tipo, para la ayuda del formulario. */
    public String patternFor(VehicleTypeCode type) {
        return type.isMotorcycle() ? MOTORCYCLE : CAR_LIKE;
    }

    /** Un ejemplo valido del tipo, para el placeholder del formulario. */
    public String exampleFor(VehicleTypeCode type) {
        return type.isMotorcycle() ? "ABC12D" : "ABC123";
    }
}
