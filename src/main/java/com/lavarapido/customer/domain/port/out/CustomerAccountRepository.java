package com.lavarapido.customer.domain.port.out;

import com.lavarapido.customer.domain.model.CustomerAccount;

import java.util.Optional;

/** Acceso a las cuentas de cliente. El id que se busca siempre es el del token. */
public interface CustomerAccountRepository {

    Optional<CustomerAccount> findById(long customerId);

    /** Busca por el user_id del token, que es como se llega a la cuenta en cada peticion. */
    Optional<CustomerAccount> findByUserId(long userId);

    boolean existsByUserId(long userId);

    CustomerAccount save(CustomerAccount account);
}