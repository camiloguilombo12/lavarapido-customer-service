package com.lavarapido.customer.domain.model;

import com.lavarapido.customer.domain.exception.InvalidValueException;

import java.util.Locale;
import java.util.Objects;

/**
 * La placa de un vehículo, ya guardada como debe quedar: sin guiones y en mayúsculas.
 * Carro: ABC123. Moto: ABC12D.
 *
 * Esta clase solo limpia el texto. El formato segun el tipo de vehiculo lo revisa
 * LicensePlatePolicy, porque el tipo no hace parte de la placa.
 *
 * Se guarda limpia y no como la escribio el usuario, para que la restriccion de placa unica
 * no dependa de si el usuario le puso guion o no.
 */
public record LicensePlate(String value) {

    // carro ABC123 y moto ABC12D: 6 letras y numeros (E-16)
    private static final int MAX_LENGTH = 6;

    public LicensePlate {
        value = normalize(value);
        if (value.isEmpty()) {
            throw new InvalidValueException("INVALID_PLATE", "License plate is required");
        }
    }

    /** La placa que alguien escribe (crear, editar, buscar). */
    public static LicensePlate of(String raw) {
        LicensePlate plate = new LicensePlate(raw);
        // E-16: se rechaza en vez de cortarla; cortada podia quedar la placa de otro vehiculo
        if (plate.value().length() > MAX_LENGTH) {
            throw new InvalidValueException("INVALID_PLATE",
                    "A license plate has at most " + MAX_LENGTH + " letters and digits");
        }
        return plate;
    }

    /**
     * La placa ya guardada en la base. La columna admite 10 caracteres y antes de E-16 pudo quedar
     * alguna mas larga que 6: leerla no puede fallar, porque romperia la lista del cliente y a
     * booking-service, que lee el vehiculo. El limite se exige cuando alguien la vuelve a escribir.
     */
    public static LicensePlate stored(String raw) {
        return new LicensePlate(raw);
    }

    /** Mayusculas y solo letras y numeros: "abc-123" queda "ABC123". */
    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /** Como se ve en pantalla: "ABC123" queda "ABC-123". */
    public String formatted() {
        return value.length() > 3 ? value.substring(0, 3) + "-" + value.substring(3) : value;
    }

    public boolean matches(String pattern) {
        return value.matches(pattern);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LicensePlate plate && value.equals(plate.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
