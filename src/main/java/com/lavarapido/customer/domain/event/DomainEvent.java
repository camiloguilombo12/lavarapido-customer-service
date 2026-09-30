package com.lavarapido.customer.domain.event;

import java.time.Instant;

/**
 * Algo que ya paso. Solo lleva ids, porque el que lo recibe pide lo demas que necesite
 * y asi el que publica no se amarra a lo que el otro servicio guarde.
 */
public interface DomainEvent {

    String eventType();

    Instant occurredAt();
}