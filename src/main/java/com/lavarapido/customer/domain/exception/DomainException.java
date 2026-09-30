package com.lavarapido.customer.domain.exception;

/**
 * Base de todo error que lanza el dominio por una regla de negocio.
 * El code() es un texto fijo que el frontend traduce, entonces no se cambia sin avisarle.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}