package com.lavarapido.customer.domain.port.out;

import com.lavarapido.customer.domain.model.VehicleType;
import com.lavarapido.customer.domain.model.VehicleTypeCode;

import java.util.List;
import java.util.Optional;

/** El catalogo de tipos de vehiculo, como esta en la base. */
public interface VehicleTypeRepository {

    List<VehicleType> findAllActive();

    Optional<VehicleType> findByCode(VehicleTypeCode code);

    Optional<VehicleType> findById(short vehicleTypeId);
}