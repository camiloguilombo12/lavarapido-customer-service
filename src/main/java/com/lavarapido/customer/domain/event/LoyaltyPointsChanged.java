package com.lavarapido.customer.domain.event;

import java.time.Instant;

/**
 * Cambio el saldo de puntos. El saldo real esta en payment.loyalty_transaction, aca solo se
 * actualiza la copia que se muestra. Por eso el evento dice que la copia quedo en N y no
 * que se ganaron N puntos.
 */
public record LoyaltyPointsChanged(long customerId, int newBalance, Instant occurredAt)
        implements DomainEvent {

    @Override
    public String eventType() {
        return "customer.loyalty-points.changed";
    }
}