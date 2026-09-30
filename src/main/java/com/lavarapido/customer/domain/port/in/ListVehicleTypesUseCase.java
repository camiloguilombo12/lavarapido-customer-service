package com.lavarapido.customer.domain.port.in;

import com.lavarapido.customer.domain.model.VehicleType;

import java.util.List;

/** El catalogo de tipos de vehiculo. Es publico porque lo necesita el formulario de registro. */
public interface ListVehicleTypesUseCase {

    List<VehicleType> execute();
}