package com.lavarapido.customer.domain.port.in;

import java.util.List;
import java.util.Optional;

/** Consultas del administrador sobre vehiculos de cualquier cliente. */
public interface LookupVehiclesUseCase {

    /** Vehiculos activos con esos ids (los que no existen o estan borrados no se devuelven). */
    List<OwnedVehicleView> byIds(List<Long> vehicleIds);

    /** El vehiculo activo con esa placa, si hay alguno. */
    Optional<OwnedVehicleView> byPlate(String licensePlate);
}
