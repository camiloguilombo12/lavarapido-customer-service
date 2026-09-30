package com.lavarapido.customer.infrastructure.adapter.in.web;

import com.lavarapido.customer.domain.port.in.ListVehicleTypesUseCase;
import com.lavarapido.customer.infrastructure.adapter.in.web.dto.VehicleTypeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * El catalogo de tipos de vehiculo. Es publico a proposito: el formulario de registro necesita
 * la lista antes de que haya sesion, y no hay ningun dato de cliente detras.
 */
@RestController
@RequestMapping("/api/v1/vehicle-types")
class VehicleTypeController {

    private final ListVehicleTypesUseCase listVehicleTypes;

    VehicleTypeController(ListVehicleTypesUseCase listVehicleTypes) {
        this.listVehicleTypes = listVehicleTypes;
    }

    @GetMapping
    List<VehicleTypeResponse> list() {
        return listVehicleTypes.execute().stream()
                .map(VehicleTypeResponse::from)
                .toList();
    }
}