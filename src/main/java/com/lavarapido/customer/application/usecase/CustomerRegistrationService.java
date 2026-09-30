package com.lavarapido.customer.application.usecase;

import com.lavarapido.customer.domain.model.CustomerAccount;
import com.lavarapido.customer.domain.port.in.ConsumeUserRegisteredUseCase;
import com.lavarapido.customer.domain.port.out.CustomerAccountRepository;
import com.lavarapido.customer.domain.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Reaccion al evento security.user.registered: crea la fila de customer.
 *
 * Es el unico camino por el que se crea una cuenta de cliente, y a proposito. La otra opcion
 * seria crearla la primera vez que el cliente toca un endpoint, pero esa fila necesita person_id,
 * que vive en el esquema de seguridad y que el token no lleva. El evento si lo trae, y es el
 * camino que el security-service ya dejo preparado.
 */
@Service
public class CustomerRegistrationService implements ConsumeUserRegisteredUseCase {

    private static final Logger log = LoggerFactory.getLogger(CustomerRegistrationService.class);

    private final CustomerAccountRepository accounts;
    private final DomainEventPublisher events;
    private final Clock clock;

    public CustomerRegistrationService(CustomerAccountRepository accounts, DomainEventPublisher events, Clock clock) {
        this.accounts = accounts;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Idempotente. Los mensajeros entregan al menos una vez, asi que este metodo va a recibir el
     * mismo evento varias veces y crear la fila solo tiene que salir bien una.
     */
    @Override
    @Transactional
    public void handle(long userId, long personId) {
        if (accounts.existsByUserId(userId)) {
            log.info("Customer profile already exists for account {}, ignoring the event", userId);
            return;
        }

        // El reloj se lee una vez: la fecha y la hora no pueden quedar de dias distintos si la
        // llamada cae justo en la medianoche.
        Instant now = clock.instant();
        CustomerAccount account = CustomerAccount.provision(personId, userId,
                LocalDate.ofInstant(now, clock.getZone()), now);
        CustomerAccount saved = accounts.save(account);
        events.publish(saved.pullEvents());

        log.info("Created customer profile {} for account {}", saved.customerId(), userId);
    }
}
