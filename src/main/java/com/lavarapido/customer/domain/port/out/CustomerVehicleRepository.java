package com.lavarapido.customer.domain.port.out;

import com.lavarapido.customer.domain.model.LicensePlate;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Consultas sobre vehiculos que no pasan por la cuenta. La placa es unica entre todos los
 * clientes, no dentro de una cuenta, y por eso se pregunta una vez por operacion en vez de
 * recorrer los vehiculos que ya tiene el cliente.
 */
public interface CustomerVehicleRepository {

    /**
     * Id del vehiculo activo que tiene esa placa, si hay alguno.
     *
     * Devuelve el id del vehiculo y no el de la cuenta porque al editar hay que diferenciar
     * "la placa es de este mismo vehiculo" (se deja pasar) de "es de otro" (choca).
     */
    Optional<Long> findActiveVehicleIdByPlate(LicensePlate plate);

    /** customer_id del dueño de un vehiculo activo; vacio si no existe o esta borrado. */
    Optional<Long> findCustomerIdOfActiveVehicle(long vehicleId);

    /**
     * customer_id del dueño de cada vehiculo activo de la lista, en una sola consulta (la lista del
     * administrador pide hasta 100). Los que no existen o estan borrados no aparecen.
     */
    Map<Long, Long> findCustomerIdsOfActiveVehicles(Collection<Long> vehicleIds);
}
