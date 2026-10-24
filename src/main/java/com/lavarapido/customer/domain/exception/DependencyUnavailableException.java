package com.lavarapido.customer.domain.exception;

/** Otro servicio del que depende la operacion (booking-service) no respondio (503). */
public class DependencyUnavailableException extends DomainException {

    public DependencyUnavailableException(String code, String message) {
        super(code, message);
    }
}
