package com.lavarapido.customer.infrastructure.adapter.out.persistence;

import com.lavarapido.customer.domain.model.CustomerVehicle;
import com.lavarapido.customer.domain.model.LicensePlate;
import com.lavarapido.customer.domain.model.VehicleText;
import com.lavarapido.customer.domain.model.VehicleType;
import com.lavarapido.customer.infrastructure.adapter.out.persistence.entity.CustomerVehicleJpaEntity;
import com.lavarapido.customer.infrastructure.adapter.out.persistence.repository.CustomerVehicleJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Traduce filas de customer_vehicle a objetos del dominio y al reves.
 *
 * La fila guarda un vehicle_type_id y el dominio un VehicleType con codigo y nombre, asi que
 * ese va y viene pasa por el catalogo. Son seis filas fijas, entonces el costo es nada y evita
 * duplicar el mapeo en los dos lados.
 */
@Repository
class CustomerVehiclePersistenceAdapter {

    private final CustomerVehicleJpaRepository repository;
    private final VehicleTypePersistenceAdapter catalog;

    CustomerVehiclePersistenceAdapter(CustomerVehicleJpaRepository repository,
                                       VehicleTypePersistenceAdapter catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    List<CustomerVehicle> loadByCustomerId(long customerId) {
        return repository.findByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(customerId).stream()
                .map(this::toDomain)
                .toList();
    }

    Optional<Long> findActiveVehicleIdByPlate(LicensePlate plate) {
        return repository.findActiveByLicensePlate(plate.value())
                .map(CustomerVehicleJpaEntity::getId);
    }

    /**
     * Inserta un vehiculo nuevo. Como la columna es IDENTITY el id solo existe despues del INSERT,
     * asi que se le asigna al objeto del dominio para que el caso de uso pueda devolverlo.
     */
    void insert(CustomerVehicle vehicle, long actorUserId) {
        CustomerVehicleJpaEntity entity = new CustomerVehicleJpaEntity(
                vehicle.customerId(),
                vehicle.licensePlate().value(),
                vehicle.vehicleType().vehicleTypeId(),
                vehicle.brand().orNull(),
                vehicle.model().orNull(),
                vehicle.color().orNull(),
                vehicle.createdAt());
        entity.setUpdatedBy(actorUserId);
        vehicle.assignId(repository.save(entity).getId());
    }

    void applyUpdate(CustomerVehicle vehicle, long actorUserId) {
        CustomerVehicleJpaEntity entity = repository.findById(vehicle.customerVehicleId()).orElseThrow();
        entity.setLicensePlate(vehicle.licensePlate().value());
        entity.setVehicleTypeId(vehicle.vehicleType().vehicleTypeId());
        entity.setBrand(vehicle.brand().orNull());
        entity.setModel(vehicle.model().orNull());
        entity.setColor(vehicle.color().orNull());
        entity.setUpdatedBy(actorUserId);
        repository.save(entity);
    }

    /**
     * Borrado logico: la fila se queda y se le pone deleted_at. Como es un UPDATE, el trigger
     * tambien actualiza updated_at y row_version.
     */
    void applyRemoval(long vehicleId, Instant deletedAt) {
        CustomerVehicleJpaEntity entity = repository.findById(vehicleId).orElseThrow();
        entity.setDeletedAt(deletedAt);
        repository.save(entity);
    }

    private CustomerVehicle toDomain(CustomerVehicleJpaEntity entity) {
        Short typeId = entity.getVehicleTypeId();
        return CustomerVehicle.restore(
                entity.getId(),
                entity.getCustomerId(),
                LicensePlate.of(entity.getLicensePlate()),
                typeId == null ? null : catalog.requireById(typeId),
                VehicleText.brand(entity.getBrand()),
                VehicleText.model(entity.getModel()),
                VehicleText.color(entity.getColor()),
                entity.getCreatedAt(),
                entity.getDeletedAt());
    }
}
