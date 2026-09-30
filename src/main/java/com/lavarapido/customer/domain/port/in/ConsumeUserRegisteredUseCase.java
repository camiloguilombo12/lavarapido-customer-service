package com.lavarapido.customer.domain.port.in;

/**
 * Reaccion al evento security.user.registered: crea la fila de customer.
 *
 * A proposito es idempotente. Los mensajeros entregan el mismo evento mas de una vez, asi que
 * si la cuenta ya existe no se toca.
 */
public interface ConsumeUserRegisteredUseCase {

    void handle(long userId, long personId);
}