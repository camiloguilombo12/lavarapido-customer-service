package com.lavarapido.customer.infrastructure.adapter.in.web.dto;

import com.lavarapido.customer.domain.port.in.OwnedVehicleView;

/** Un vehiculo con el user_id de su dueño (solo administrador). */
public record OwnedVehicleResponse(VehicleResponse vehicle, Long ownerUserId) {

    public static OwnedVehicleResponse from(OwnedVehicleView view) {
        return new OwnedVehicleResponse(VehicleResponse.from(view.vehicle()), view.ownerUserId());
    }
}
