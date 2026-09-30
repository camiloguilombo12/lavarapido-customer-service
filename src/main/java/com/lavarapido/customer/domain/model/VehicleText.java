package com.lavarapido.customer.domain.model;

import com.lavarapido.customer.domain.exception.InvalidValueException;

/**
 * Marca, modelo o color del vehiculo. Los tres son opcionales porque de verdad lo son:
 * un carro de segunda mano muchas veces no tiene el modelo a la vista, y el color casi
 * nunca se escribe.
 *
 * Quita espacios de mas y deja el texto como lo escribio el dueño (no lo pasa a minuscula),
 * igual que hace PersonName en el security-service.
 *
 * No es un record porque cada campo tiene un limite de longitud distinto y el constructor
 * de un record no puede recibir nada mas que sus propios componentes.
 */
public final class VehicleText {

    private final String value;

    private VehicleText(String raw, int maxLength, String label) {
        String cleaned = clean(raw);
        if (cleaned.length() > maxLength) {
            throw new InvalidValueException("INVALID_VEHICLE_TEXT", label + " is too long");
        }
        this.value = cleaned;
    }

    /** Marca: columna de 50. */
    public static VehicleText brand(String raw) {
        return new VehicleText(raw, 50, "Brand");
    }

    /** Modelo: columna de 50, la agrego el changeset 015. */
    public static VehicleText model(String raw) {
        return new VehicleText(raw, 50, "Model");
    }

    /** Color: columna de 30. */
    public static VehicleText color(String raw) {
        return new VehicleText(raw, 30, "Color");
    }

    /** Sin dato, para cuando el campo opcional no se lleno. */
    public static VehicleText empty() {
        return new VehicleText("", 50, "Vehicle text");
    }

    /** Vacio si el usuario no lo lleno, para no devolver strings en blanco por la API. */
    public String orNull() {
        return value.isEmpty() ? null : value;
    }

    public String value() {
        return value;
    }

    private static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.strip().replaceAll("\\s+", " ");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VehicleText text && value.equals(text.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
