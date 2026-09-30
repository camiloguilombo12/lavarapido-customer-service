package com.lavarapido.customer.domain.port.in;

import java.util.List;

/** Los vehiculos del cliente que viene en el token, sin los borrados. */
public interface ListCustomerVehiclesUseCase {

    List<CustomerVehicleView> execute(long userId);
}