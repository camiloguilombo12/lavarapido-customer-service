package com.lavarapido.customer.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.customer.infrastructure.adapter.out.persistence.entity.VehicleTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** El catalogo de tipos de vehiculo: seis filas que siembra el changeset 102. */
public interface VehicleTypeJpaRepository extends JpaRepository<VehicleTypeJpaEntity, Short> {

    List<VehicleTypeJpaEntity> findByActiveTrueAndDeletedAtIsNullOrderByDisplayOrderAsc();

    Optional<VehicleTypeJpaEntity> findByCodeAndDeletedAtIsNull(String code);
}