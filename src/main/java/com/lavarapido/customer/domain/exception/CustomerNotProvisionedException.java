package com.lavarapido.customer.domain.exception;

/**
 * La cuenta todavia no tiene fila en la tabla customer. Esa fila la crea este servicio al
 * recibir el evento security.user.registered, que es el que trae el person_id. Si el evento
 * aun no ha llegado, la cuenta existe pero su perfil de cliente todavia no.
 */
public class CustomerNotProvisionedException extends DomainException {

    public CustomerNotProvisionedException(long userId) {
        super("CUSTOMER_NOT_PROVISIONED",
                "The customer profile for account " + userId + " is not ready yet");
    }
}