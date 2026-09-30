package com.lavarapido.customer.domain.model;

import com.lavarapido.customer.domain.exception.InvalidValueException;

import java.time.Instant;
import java.util.Objects;

/**
 * Un vehiculo registrado por un cliente. Es la fila de customer_vehicle.
 *
 * El borrado es logico, nunca se borra la fila. Por eso despues se puede volver a registrar
 * la misma placa: lo deja el indice unico, que solo mira las filas que no estan borradas.
 *
 * updated_at y row_version no aparecen aqui porque los pone el trigger de la base.
 */
public class CustomerVehicle {

    private long customerVehicleId;
    private final long customerId;
    private LicensePlate licensePlate;
    private VehicleType vehicleType;
    private VehicleText brand;
    private VehicleText model;
    private VehicleText color;
    private Instant createdAt;
    private Instant deletedAt;

    private CustomerVehicle(long customerVehicleId, long customerId, LicensePlate licensePlate,
                            VehicleType vehicleType, VehicleText brand, VehicleText model, VehicleText color,
                            Instant createdAt, Instant deletedAt) {
        this.customerVehicleId = customerVehicleId;
        this.customerId = customerId;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /** Vehiculo nuevo. El id lo pone la base. */
    public static CustomerVehicle register(long customerId, LicensePlate plate, VehicleType type,
                                           VehicleText brand, VehicleText model, VehicleText color,
                                           Instant now) {
        return new CustomerVehicle(0L, requireCustomerId(customerId), plate, requireType(type),
                orEmpty(brand), orEmpty(model), orEmpty(color), now, null);
    }

    /** Uno que ya estaba en la base. */
    public static CustomerVehicle restore(long id, long customerId, LicensePlate plate, VehicleType type,
                                          VehicleText brand, VehicleText model, VehicleText color,
                                          Instant createdAt, Instant deletedAt) {
        return new CustomerVehicle(id, customerId, plate, requireType(type), orEmpty(brand), orEmpty(model),
                orEmpty(color), createdAt, deletedAt);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * La base le devuelve el id al vehiculo nuevo y el dominio lo guarda, para poder responderlo.
     * Solo lo llama la capa de persistencia, y una sola vez. Si ya tuviera id es un error de
     * programacion (una identidad no se cambia) y revienta en vez de dejarlo pasar.
     */
    public void assignId(long assignedId) {
        if (customerVehicleId != 0L) {
            throw new IllegalStateException("This vehicle already has an id and cannot be reassigned");
        }
        this.customerVehicleId = assignedId;
    }

    public void update(LicensePlate newPlate, VehicleType newType, VehicleText newBrand,
                       VehicleText newModel, VehicleText newColor) {
        if (newPlate != null && !newPlate.equals(this.licensePlate)) {
            this.licensePlate = newPlate;
        }
        this.vehicleType = requireType(newType);
        this.brand = orEmpty(newBrand);
        this.model = orEmpty(newModel);
        this.color = orEmpty(newColor);
    }

    public void remove(Instant now) {
        this.deletedAt = now;
    }

    /**
     * El 0 quiere decir "todavia no guardada", que es el estado normal de un vehiculo nuevo.
     * Un id negativo si es un error de programacion y se rechaza.
     */
    private static long requireCustomerId(long customerId) {
        if (customerId < 0L) {
            throw new InvalidValueException("INVALID_CUSTOMER", "A vehicle must belong to a customer");
        }
        return customerId;
    }

    private static VehicleType requireType(VehicleType type) {
        if (type == null) {
            throw new InvalidValueException("INVALID_VEHICLE_TYPE", "Vehicle type is required");
        }
        return type;
    }

    private static VehicleText orEmpty(VehicleText text) {
        return text == null ? VehicleText.empty() : text;
    }

    public long customerVehicleId() {
        return customerVehicleId;
    }

    public long customerId() {
        return customerId;
    }

    public LicensePlate licensePlate() {
        return licensePlate;
    }

    public VehicleType vehicleType() {
        return vehicleType;
    }

    public VehicleText brand() {
        return brand;
    }

    public VehicleText model() {
        return model;
    }

    public VehicleText color() {
        return color;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CustomerVehicle vehicle)) {
            return false;
        }
        // Sin id todavia no esta guardado, asi que solo puede ser el mismo.
        return customerVehicleId != 0L && customerVehicleId == vehicle.customerVehicleId;
    }

    @Override
    public int hashCode() {
        return customerVehicleId == 0L ? System.identityHashCode(this) : Objects.hash(customerVehicleId);
    }
}
