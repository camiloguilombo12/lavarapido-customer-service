package com.lavarapido.customer.infrastructure.adapter.out.persistence;

import com.lavarapido.customer.domain.exception.InvalidValueException;
import com.lavarapido.customer.domain.model.VehicleType;
import com.lavarapido.customer.domain.model.VehicleTypeCode;
import com.lavarapido.customer.domain.port.out.VehicleTypeRepository;
import com.lavarapido.customer.infrastructure.adapter.out.persistence.entity.VehicleTypeJpaEntity;
import com.lavarapido.customer.infrastructure.adapter.out.persistence.repository.VehicleTypeJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Traduce el catalogo de tipos de vehiculo. Esta es la pieza que resuelve la clave foranea
 * vehicle_type_id en las dos direcciones: cuando el cliente manda el codigo "SUV" hay que
 * encontrar la id 3, y cuando se lee la fila hay que volver de la id al codigo y al nombre.
 */
@Repository
class VehicleTypePersistenceAdapter implements VehicleTypeRepository {

    private final VehicleTypeJpaRepository repository;

    VehicleTypePersistenceAdapter(VehicleTypeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<VehicleType> findAllActive() {
        return repository.findByActiveTrueAndDeletedAtIsNullOrderByDisplayOrderAsc().stream()
                .map(VehicleTypePersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<VehicleType> findByCode(VehicleTypeCode code) {
        return repository.findByCodeAndDeletedAtIsNull(code.name()).map(VehicleTypePersistenceAdapter::toDomain);
    }

    @Override
    public Optional<VehicleType> findById(short vehicleTypeId) {
        return repository.findById(vehicleTypeId).map(VehicleTypePersistenceAdapter::toDomain);
    }

    /**
     * La version que lanza, para cuando se lee. Si un vehiculo apunta a un tipo que ya no esta
     * en el catalogo, es un dato dañado y mejor que se note a devolver un vehiculo sin tipo.
     */
    VehicleType requireById(short vehicleTypeId) {
        return findById(vehicleTypeId)
                .orElseThrow(() -> new InvalidValueException("INVALID_VEHICLE_TYPE",
                        "The catalog has no vehicle type with id " + vehicleTypeId));
    }

    private static VehicleType toDomain(VehicleTypeJpaEntity entity) {
        return new VehicleType(
                entity.getId(),
                VehicleTypeCode.of(entity.getCode()),
                entity.getName(),
                entity.getSizeFactor(),
                entity.getDisplayOrder(),
                entity.isActive());
    }
}
