package com.lavarapido.customer.application.usecase;

import com.lavarapido.customer.domain.model.CustomerVehicle;
import com.lavarapido.customer.domain.model.VehicleType;
import com.lavarapido.customer.domain.port.in.CustomerVehicleView;

/** Arma la vista del vehiculo en un solo lugar, para que el dueño y el admin vean lo mismo. */
final class VehicleViews {

    private VehicleViews() {
    }

    static CustomerVehicleView of(CustomerVehicle vehicle) {
        VehicleType type = vehicle.vehicleType();
        return new CustomerVehicleView(
                vehicle.customerVehicleId(),
                vehicle.licensePlate().value(),
                vehicle.licensePlate().formatted(),
                type.code().name(),
                type.vehicleTypeId(),
                type.name(),
                vehicle.brand().orNull(),
                vehicle.model().orNull(),
                vehicle.color().orNull(),
                vehicle.createdAt());
    }
}
