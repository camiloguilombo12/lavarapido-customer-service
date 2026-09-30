package com.lavarapido.customer.domain.port.in;

/** Edita un vehiculo. Si es de otro cliente responde 404, no 403. */
public interface UpdateCustomerVehicleUseCase {

    CustomerVehicleView execute(long userId, long vehicleId, UpdateCustomerVehicleCommand command);
}