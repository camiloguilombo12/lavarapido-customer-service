package com.lavarapido.customer.infrastructure.config;

import com.lavarapido.customer.domain.service.LicensePlatePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Las reglas de negocio que son objetos van aqui para poder inyectarlas, en vez de ser metodos
 * estaticos. Asi el dominio no depende de Spring y las pruebas pueden cambiarlos.
 */
@Configuration
class DomainConfig {

    /**
     * El reloj entra por el contexto para que los tests fijen la hora en vez de esperar la real,
     * y para que created_at no dependa del dia en que corrio la prueba.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    LicensePlatePolicy licensePlatePolicy() {
        return new LicensePlatePolicy();
    }
}
