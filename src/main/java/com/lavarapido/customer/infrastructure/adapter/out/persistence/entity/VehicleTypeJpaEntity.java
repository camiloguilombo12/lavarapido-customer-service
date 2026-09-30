package com.lavarapido.customer.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * La fila de vehicle_type. Solo lectura desde la aplicacion: el catalogo cambia por migracion
 * o por consola, no por endpoint, porque son seis filas fijas que siembra el changeset 102.
 *
 * size_factor es la sugerencia de precio para el administrador, no un factor de calculo. El
 * precio sale de catalog.service_price.
 */
@Entity
@Table(schema = "customer", name = "vehicle_type")
public class VehicleTypeJpaEntity {

    @Id
    @Column(name = "vehicle_type_id")
    private Short id;

    @Column(name = "code", nullable = false, length = 30, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 60, updatable = false)
    private String name;

    @Column(name = "size_factor", nullable = false, updatable = false)
    private BigDecimal sizeFactor;

    @Column(name = "display_order", nullable = false, updatable = false)
    private short displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private Instant deletedAt;

    @Column(name = "deleted_by", insertable = false, updatable = false)
    private Long deletedBy;

    @Column(name = "row_version", insertable = false, updatable = false)
    private Integer rowVersion;

    protected VehicleTypeJpaEntity() {
    }

    public Short getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSizeFactor() {
        return sizeFactor;
    }

    public short getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }
}