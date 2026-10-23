package com.lavarapido.customer.domain.port.out;

import java.util.Optional;

/**
 * Pregunta al security-service el person_id de una cuenta (REST, ADR-004).
 *
 * Se usa cuando el cliente entra y su perfil todavia no existe porque el evento
 * security.user_registered no llego (RabbitMQ apagado, o cuentas creadas antes de conectar los
 * eventos). El token no trae el person_id a proposito, y la fila de customer lo necesita.
 */
public interface IdentityDirectory {

    /** person_id de la cuenta que esta llamando; vacio si el security-service no la reconoce. */
    Optional<Long> personIdOf(long userId);
}
