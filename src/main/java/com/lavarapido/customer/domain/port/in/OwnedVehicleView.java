package com.lavarapido.customer.domain.port.in;

/**
 * Un vehiculo con el user_id de su dueño. Solo para el administrador (reservas que crea a nombre
 * de un cliente) y para booking-service, que necesita saber a quien notificar.
 */
public record OwnedVehicleView(CustomerVehicleView vehicle, Long ownerUserId) {
}
