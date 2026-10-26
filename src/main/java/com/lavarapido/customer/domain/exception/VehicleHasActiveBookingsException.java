package com.lavarapido.customer.domain.exception;

/**
 * BIZ-08: el vehiculo tiene reservas activas (en curso, o programadas/confirmadas que no han
 * terminado) y retirarlo las dejaria huerfanas. Primero hay que cancelarlas o esperar a que pasen.
 */
public class VehicleHasActiveBookingsException extends DomainException {

    private final long activeBookings;

    public VehicleHasActiveBookingsException(long vehicleId, long activeBookings) {
        super("VEHICLE_HAS_ACTIVE_BOOKINGS",
                "Vehicle " + vehicleId + " has " + activeBookings + " active booking(s)");
        this.activeBookings = activeBookings;
    }

    public long activeBookings() {
        return activeBookings;
    }
}
