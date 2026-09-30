package com.lavarapido.customer.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * La fila de customer.
 *
 * person_id apunta al esquema de seguridad SIN foreign key (ADR-003): la referencia se revisa en
 * el codigo. Igual con user_id, que ni siquiera es una referencia dura, es el vinculo con la
 * cuenta y queda en null hasta que llega el evento de registro.
 *
 * updated_at y row_version son de solo lectura a proposito, los maneja el trigger tr_touch_customer.
 */
@Entity
@Table(schema = "customer", name = "customer")
public class CustomerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long id;

    @Column(name = "person_id", nullable = false, updatable = false)
    private Long personId;

    /** La agrego el changeset 014: es el sub del JWT con el que se encuentra esta cuenta. */
    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "loyalty_points", nullable = false)
    private int loyaltyPoints;

    @Column(name = "customer_since", nullable = false, updatable = false)
    private LocalDate customerSince;

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

    protected CustomerJpaEntity() {
    }

    public CustomerJpaEntity(Long personId, Long userId, LocalDate customerSince, Instant createdAt) {
        this.personId = personId;
        this.userId = userId;
        this.loyaltyPoints = 0;
        this.customerSince = customerSince;
        this.createdAt = createdAt;
        this.createdBy = userId;
    }

    public Long getId() {
        return id;
    }

    public Long getPersonId() {
        return personId;
    }

    public Long getUserId() {
        return userId;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public LocalDate getCustomerSince() {
        return customerSince;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** El filtro lo lee igual aunque este servicio nunca borre la fila. */
    public Instant getDeletedAt() {
        return deletedAt;
    }
}