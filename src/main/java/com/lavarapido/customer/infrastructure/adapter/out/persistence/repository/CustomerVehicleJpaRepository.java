package com.lavarapido.customer.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.customer.infrastructure.adapter.out.persistence.entity.CustomerVehicleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Los vehiculos de un cliente, y la comprobacion de que la placa este libre. */
public interface CustomerVehicleJpaRepository extends JpaRepository<CustomerVehicleJpaEntity, Long> {

    List<CustomerVehicleJpaEntity> findByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(long customerId);

    /**
     * La placa se busca entre TODOS los clientes y no solo dentro de una cuenta, por eso esta
     * consulta no lleva customer_id. Devuelve el vehiculo y no la cuenta, para que al editar se
     * pueda diferenciar "es el mismo" de "es de otro".
     */
    @Query("""
            SELECT v FROM CustomerVehicleJpaEntity v
            WHERE v.licensePlate = :plate AND v.deletedAt IS NULL
            """)
    Optional<CustomerVehicleJpaEntity> findActiveByLicensePlate(@Param("plate") String plate);
}