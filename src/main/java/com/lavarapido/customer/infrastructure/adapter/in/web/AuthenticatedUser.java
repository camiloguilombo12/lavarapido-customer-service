package com.lavarapido.customer.infrastructure.adapter.in.web;

import com.lavarapido.customer.domain.exception.InvalidValueException;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Quien esta llamando, leido de un token que ya se verifico.
 *
 * El claim sub es el user_id de app_user, NO el customer_id. Son columnas distintas y
 * confundirlas haria que un cliente viera los vehiculos de otro. La fila de cliente se busca
 * por user_id, que es la columna que agrego el changeset 014.
 */
record AuthenticatedUser(long userId) {

    static AuthenticatedUser from(Jwt jwt) {
        String subject = jwt == null ? null : jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidValueException("INVALID_TOKEN", "The token has no subject claim");
        }
        try {
            return new AuthenticatedUser(Long.parseLong(subject));
        } catch (NumberFormatException e) {
            throw new InvalidValueException("INVALID_TOKEN", "The token subject is not a valid user id");
        }
    }
}