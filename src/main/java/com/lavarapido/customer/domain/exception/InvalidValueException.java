package com.lavarapido.customer.domain.exception;

/** Un valor no sirve (placa, tipo de vehiculo, marca...). */
public class InvalidValueException extends DomainException {

    public InvalidValueException(String code, String message) {
        super(code, message);
    }
}
