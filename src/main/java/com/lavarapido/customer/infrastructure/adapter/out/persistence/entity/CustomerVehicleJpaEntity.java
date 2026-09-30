package com.lavarapido.customer.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * La fila de customer_vehicle.
 *
 * La placa se guarda como la devuelve LicensePlate: sin guiones y en mayusculas. El indice
 * unico es filtrado (WHERE deleted_at IS NULL), asi que el borrado logico es lo que deja
 * volver a registrar la misma placa despues.
 *
 * updated_at y row_version los maneja el trigger tr_touch_customer_vehicle, nunca esta clase.
 */
@Entity
@Table(schema = "customer", name = "customer_vehicle")
public class CustomerVehicleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_vehicle_id")
    private Long id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private Long customerId;

    @Column(name = "license_plate", nullable = false, length = 10)
    private String licensePlate;

    @Column(name = "vehicle_type_id", nullable = false)
    private Short vehicleTypeId;

    @Column(name = "brand", length = 50)
    private String brand;

    /** La agrego el changeset 015: el formulario del frontend pedia modelo y la tabla no lo tenia. */
    @Column(name = "model", length = 50)
    private String model;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    /** El borrado logico escribe esta columna, asi que si se puede editar. El trigger solo pisa
     *  updated_at y row_version. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", insertable = false, updatable = false)
    private Long deletedBy;

    @Column(name = "row_version", insertable = false, updatable = false)
    private Integer rowVersion;

    protected CustomerVehicleJpaEntity() {
    }

    public CustomerVehicleJpaEntity(Long customerId, String licensePlate, Short vehicleTypeId,
                                    String brand, String model, String color, Instant createdAt) {
        this.customerId = customerId;
        this.licensePlate = licensePlate;
        this.vehicleTypeId = vehicleTypeId;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.createdAt = createdAt;
        this.createdBy = customerId;
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Short getVehicleTypeId() {
        return vehicleTypeId;
    }

    public void setVehicleTypeId(Short vehicleTypeId) {
        this.vehicleTypeId = vehicleTypeId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}