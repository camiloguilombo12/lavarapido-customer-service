package com.lavarapido.customer.domain.port.out;

/**
 * Pregunta a booking-service por las reservas de un vehiculo (REST, ADR-004; BIZ-08).
 *
 * Se usa antes de retirar un vehiculo: con reservas activas no se retira. Si booking no responde,
 * lanza DependencyUnavailableException (503): no se retira a ciegas.
 */
public interface BookingDirectory {

    /** Reservas activas del vehiculo: en curso, o programadas/confirmadas que no han terminado. */
    long activeBookingsOf(long customerVehicleId);
}
