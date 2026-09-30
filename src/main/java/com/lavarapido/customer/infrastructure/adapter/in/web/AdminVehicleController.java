package com.lavarapido.customer.infrastructure.adapter.in.web;

import com.lavarapido.customer.domain.exception.InvalidValueException;
import com.lavarapido.customer.domain.port.in.LookupVehiclesUseCase;
import com.lavarapido.customer.infrastructure.adapter.in.web.dto.OwnedVehicleResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Vehiculos de cualquier cliente, solo para ADMIN (SecurityConfig protege /api/v1/admin/**).
 *
 * Lo usan la pantalla de reservas del admin (placa, tipo y dueño de cada reserva) y
 * booking-service cuando el admin crea una reserva a nombre de un cliente.
 * Se busca por ids (?ids=1,2,3) o por placa (?plate=ABC123); uno de los dos es obligatorio.
 */
@RestController
@RequestMapping("/api/v1/admin/vehicles")
class AdminVehicleController {

    private final LookupVehiclesUseCase lookup;

    AdminVehicleController(LookupVehiclesUseCase lookup) {
        this.lookup = lookup;
    }

    @GetMapping
    List<OwnedVehicleResponse> find(@RequestParam(required = false) List<Long> ids,
                                    @RequestParam(required = false) String plate) {
        if (plate != null && !plate.isBlank()) {
            return lookup.byPlate(plate).map(OwnedVehicleResponse::from).stream().toList();
        }
        if (ids != null && !ids.isEmpty()) {
            return lookup.byIds(ids).stream().map(OwnedVehicleResponse::from).toList();
        }
        throw new InvalidValueException("MISSING_FILTER", "Send ids or plate");
    }
}
