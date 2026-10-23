package com.lavarapido.customer.domain.port.in;

/**
 * Un vehiculo del cliente que llama. Lo usa booking-service para validar que el vehiculo de la
 * reserva es de quien reserva (INV-BOOK-002): si es de otro, responde 404.
 */
public interface GetCustomerVehicleUseCase {

    CustomerVehicleView get(long userId, long vehicleId);
}
