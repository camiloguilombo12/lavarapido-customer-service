package com.lavarapido.customer.infrastructure.adapter.out.persistence.repository;

import com.lavarapido.customer.infrastructure.adapter.out.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Solo la fila de la cuenta. Los vehiculos van en su propio repositorio porque se consultan
 * sueltos (la busqueda de placa es general) y no siempre se necesitan.
 */
public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, Long> {

    /**
     * La busqueda de todo: el id del token, que es un user_id y no un customer_id.
     *
     * Filtra por deleted_at IS NULL porque el borrado logico tiene que verse igual desde el
     * dominio: una cuenta borrada no existe a los ojos de quien la usa.
     */
    @Query("""
            SELECT c FROM CustomerJpaEntity c
            WHERE c.userId = :userId AND c.deletedAt IS NULL
            """)
    Optional<CustomerJpaEntity> findActiveByUserId(@Param("userId") long userId);

    @Query("""
            SELECT COUNT(c) > 0 FROM CustomerJpaEntity c
            WHERE c.userId = :userId AND c.deletedAt IS NULL
            """)
    boolean existsActiveByUserId(@Param("userId") long userId);
}