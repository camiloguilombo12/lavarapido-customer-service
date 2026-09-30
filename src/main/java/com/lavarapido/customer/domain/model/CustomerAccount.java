package com.lavarapido.customer.domain.model;

import com.lavarapido.customer.domain.event.CustomerProvisioned;
import com.lavarapido.customer.domain.event.DomainEvent;
import com.lavarapido.customer.domain.event.VehicleRegistered;
import com.lavarapido.customer.domain.event.VehicleRemoved;
import com.lavarapido.customer.domain.event.VehicleUpdated;
import com.lavarapido.customer.domain.exception.InvalidValueException;
import com.lavarapido.customer.domain.exception.VehicleNotFoundException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * La cuenta del cliente y los vehiculos que tiene. Es el objeto principal del servicio.
 *
 * La fila de customer la crea este servicio cuando le llega el evento security.user.registered,
 * porque ese evento es el unico que trae el person_id (el token no lo lleva).
 *
 * Los vehiculos se guardan junto con la cuenta y no aparte, porque la placa es unica entre todos
 * los clientes, no solo dentro de una cuenta. Asi esa regla se ve desde un solo lado.
 */
public class CustomerAccount {

    private long customerId;
    private final long personId;
    private final Long userId;
    private int loyaltyPoints;
    private LocalDate customerSince;
    private Instant createdAt;
    private final List<CustomerVehicle> vehicles = new ArrayList<>();
    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    private CustomerAccount(long customerId, long personId, Long userId, int loyaltyPoints,
                             LocalDate customerSince, Instant createdAt, List<CustomerVehicle> vehicles) {
        this.customerId = customerId;
        this.personId = personId;
        this.userId = userId;
        this.loyaltyPoints = loyaltyPoints;
        this.customerSince = customerSince;
        this.createdAt = createdAt;
        this.vehicles.addAll(vehicles);
    }

    /** Crea la cuenta a partir del evento de registro. */
    public static CustomerAccount provision(long personId, Long userId, LocalDate today, Instant now) {
        if (personId <= 0) {
            throw new InvalidValueException("INVALID_PERSON", "A customer must reference a person");
        }
        CustomerAccount account = new CustomerAccount(0L, personId, userId, 0, today, now, List.of());
        account.pendingEvents.add(new CustomerProvisioned(0L, personId, userId == null ? 0L : userId, now));
        return account;
    }

    /** Una cuenta que ya esta en la base. */
    public static CustomerAccount restore(long customerId, long personId, Long userId, int loyaltyPoints,
                                          LocalDate customerSince, Instant createdAt,
                                          List<CustomerVehicle> vehicles) {
        if (loyaltyPoints < 0) {
            throw new InvalidValueException("INVALID_POINTS", "Loyalty points cannot be negative");
        }
        return new CustomerAccount(customerId, personId, userId, loyaltyPoints, customerSince, createdAt, vehicles);
    }

    /**
     * La base asigna el id al insertar la cuenta (IDENTITY). Se pasa aqui para que los vehiculos
     * que se registren despues, y el evento CustomerProvisioned, lleven el id real y no 0.
     */
    public void assignId(long newCustomerId) {
        if (customerId != 0L) {
            throw new IllegalStateException("The customer already has an id");
        }
        this.customerId = newCustomerId;
        pendingEvents.replaceAll(event -> event instanceof CustomerProvisioned provisioned
                ? new CustomerProvisioned(newCustomerId, provisioned.personId(), provisioned.userId(),
                        provisioned.occurredAt())
                : event);
    }

    /** Registra un vehiculo nuevo. El formato de la placa ya lo reviso quien llama. */
    public CustomerVehicle registerVehicle(LicensePlate plate, VehicleType type, VehicleText brand,
                                           VehicleText model, VehicleText color, Instant now) {
        CustomerVehicle vehicle = CustomerVehicle.register(customerId, plate, type, brand, model, color, now);
        vehicles.add(vehicle);
        pendingEvents.add(new VehicleRegistered(customerId, 0L, plate.value(), type.code().name(), now));
        return vehicle;
    }

    public void updateVehicle(long vehicleId, LicensePlate plate, VehicleType type, VehicleText brand,
                              VehicleText model, VehicleText color, Instant now) {
        CustomerVehicle vehicle = findActiveVehicle(vehicleId);
        vehicle.update(plate, type, brand, model, color);
        pendingEvents.add(new VehicleUpdated(customerId, vehicleId, vehicle.licensePlate().value(),
                type.code().name(), now));
    }

    public void removeVehicle(long vehicleId, Instant now) {
        CustomerVehicle vehicle = findActiveVehicle(vehicleId);
        vehicle.remove(now);
        pendingEvents.add(new VehicleRemoved(customerId, vehicleId, vehicle.licensePlate().value(), now));
    }

    /** Busca un vehiculo que no este borrado. Si esta borrado es como si no existiera. */
    public CustomerVehicle findActiveVehicle(long vehicleId) {
        return vehicles.stream()
                .filter(vehicle -> vehicle.customerVehicleId() == vehicleId && !vehicle.isDeleted())
                .findFirst()
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    }

    /** Solo los que el cliente todavia puede ver en pantalla. */
    public List<CustomerVehicle> activeVehicles() {
        return vehicles.stream().filter(vehicle -> !vehicle.isDeleted()).toList();
    }

    /**
     * Todos, incluidos los ya borrados. Lo usa la persistencia al guardar: un borrado logico
     * sigue siendo un UPDATE que hay que escribir, y si se guardara solo la lista de activos
     * el borrado se perderia sin avisar.
     */
    public List<CustomerVehicle> allVehicles() {
        return List.copyOf(vehicles);
    }

    public boolean hasActiveVehicleWithPlate(LicensePlate plate) {
        return vehicles.stream()
                .anyMatch(vehicle -> !vehicle.isDeleted() && vehicle.licensePlate().equals(plate));
    }

    /**
     * El saldo de puntos no se calcula aqui. El saldo real lo lleva payment-service y este es
     * solo el saldo que se muestra, asi que el numero llega ya listo desde afuera.
     */
    public void setLoyaltyPoints(int newBalance) {
        if (newBalance < 0) {
            throw new InvalidValueException("INVALID_POINTS", "Loyalty points cannot be negative");
        }
        this.loyaltyPoints = newBalance;
    }

    /**
     * Saca los eventos de la lista para publicarlos. Se vacia al sacarlos, asi un fallo al
     * publicar no los borra y una repeticion no los manda dos veces.
     */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> drained = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return drained;
    }

    public long customerId() {
        return customerId;
    }

    public long personId() {
        return personId;
    }

    public Long userId() {
        return userId;
    }

    public int loyaltyPoints() {
        return loyaltyPoints;
    }

    public LocalDate customerSince() {
        return customerSince;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CustomerAccount account && customerId == account.customerId && customerId != 0L;
    }

    @Override
    public int hashCode() {
        return customerId == 0L ? System.identityHashCode(this) : Objects.hash(customerId);
    }
}
