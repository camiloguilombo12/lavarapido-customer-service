package com.lavarapido.customer.application.usecase;

import com.lavarapido.customer.domain.model.VehicleType;
import com.lavarapido.customer.domain.port.in.ListVehicleTypesUseCase;
import com.lavarapido.customer.domain.port.out.VehicleTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Consulta del catalogo de tipos de vehiculo. Solo lectura. */
@Service
@Transactional(readOnly = true)
public class VehicleTypeQueryService implements ListVehicleTypesUseCase {

    private final VehicleTypeRepository vehicleTypes;

    public VehicleTypeQueryService(VehicleTypeRepository vehicleTypes) {
        this.vehicleTypes = vehicleTypes;
    }

    @Override
    public List<VehicleType> execute() {
        return vehicleTypes.findAllActive();
    }
}
