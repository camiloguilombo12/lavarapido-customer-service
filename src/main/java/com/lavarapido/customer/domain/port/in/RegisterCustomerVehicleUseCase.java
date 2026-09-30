package com.lavarapido.customer.domain.port.in;

/**
 * Registra un vehiculo del cliente que viene en el token.
 *
 * No se recibe el id del cliente: sale del token, para que nadie registre un vehiculo
 * en nombre de otro.
 */
public interface RegisterCustomerVehicleUseCase {

    CustomerVehicleView execute(long userId, RegisterCustomerVehicleCommand command);
}