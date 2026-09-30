package com.lavarapido.customer.domain.exception;

/** Esa placa ya la tiene otro vehiculo que sigue activo. */
public class PlateAlreadyRegisteredException extends DomainException {

    private final String plate;

    public PlateAlreadyRegisteredException(String plate) {
        super("PLATE_ALREADY_REGISTERED", "The plate " + plate + " is already registered");
        this.plate = plate;
    }

    public String plate() {
        return plate;
    }
}