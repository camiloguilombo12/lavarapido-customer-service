package com.lavarapido.customer.infrastructure.adapter.out.persistence;

import com.lavarapido.customer.domain.model.CustomerAccount;
import com.lavarapido.customer.domain.model.CustomerVehicle;
import com.lavarapido.customer.domain.port.out.CustomerAccountRepository;
import com.lavarapido.customer.infrastructure.adapter.out.persistence.entity.CustomerJpaEntity;
import com.lavarapido.customer.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Guarda y lee la cuenta de cliente junto con sus vehiculos.
 *
 * Van en la misma transaccion a proposito: un vehiculo sin la fila que lo sostiene no lo puede
 * leer ningun endpoint, asi que no tiene sentido poder separarlos.
 */
@Repository
class CustomerAccountPersistenceAdapter implements CustomerAccountRepository {

    private final CustomerJpaRepository accounts;
    private final CustomerVehiclePersistenceAdapter vehicles;

    CustomerAccountPersistenceAdapter(CustomerJpaRepository accounts,
                                      CustomerVehiclePersistenceAdapter vehicles) {
        this.accounts = accounts;
        this.vehicles = vehicles;
    }

    @Override
    public Optional<CustomerAccount> findById(long customerId) {
        return accounts.findById(customerId)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomainWithVehicles);
    }

    @Override
    public Optional<CustomerAccount> findByUserId(long userId) {
        return accounts.findActiveByUserId(userId).map(this::toDomainWithVehicles);
    }

    @Override
    public boolean existsByUserId(long userId) {
        return accounts.existsActiveByUserId(userId);
    }

    /**
     * Guarda la cuenta y todos sus vehiculos, y devuelve el mismo objeto con los ids que la base
     * asigno. No se devuelve una copia releida porque el caso de uso ya tiene el objeto que va a
     * serializar, y releerlo seria una consulta de mas para obtener lo mismo.
     */
    @Override
    public CustomerAccount save(CustomerAccount account) {
        CustomerJpaEntity entity = new CustomerJpaEntity(
                account.personId(),
                account.userId(),
                account.customerSince(),
                account.createdAt());
        accounts.save(entity);

        for (CustomerVehicle vehicle : account.allVehicles()) {
            if (vehicle.isDeleted()) {
                // El borrado logico tambien es un UPDATE, asi que va por su propio camino.
                vehicles.applyRemoval(vehicle.customerVehicleId(), vehicle.deletedAt());
            } else if (vehicle.customerVehicleId() == 0L) {
                vehicles.insert(vehicle, actorOf(account));
            } else {
                vehicles.applyUpdate(vehicle, actorOf(account));
            }
        }
        return account;
    }

    /**
     * Quien queda como autor del cambio. En esta tabla solo hay un movimiento, que es crear la
     * cuenta, asi que el autor es el mismo usuario del token.
     */
    private static long actorOf(CustomerAccount account) {
        return account.userId() == null ? 0L : account.userId();
    }

    private CustomerAccount toDomainWithVehicles(CustomerJpaEntity entity) {
        return toDomain(entity, vehicles.loadByCustomerId(entity.getId()));
    }

    private static CustomerAccount toDomain(CustomerJpaEntity entity, List<CustomerVehicle> vehicles) {
        return CustomerAccount.restore(
                entity.getId(),
                entity.getPersonId(),
                entity.getUserId(),
                entity.getLoyaltyPoints(),
                entity.getCustomerSince(),
                entity.getCreatedAt(),
                vehicles);
    }
}
