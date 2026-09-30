package com.lavarapido.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * customer-service: la cuenta del cliente y los vehiculos que tiene.
 *
 * Es dueño del esquema customer y de sus tres tablas: vehicle_type, customer y customer_vehicle.
 * No tiene claves foraneas hacia ningun otro servicio. Las referencias cruzadas son columnas
 * sueltas mas un indice, y la informacion de que una cuenta ya tiene perfil de cliente viaja por
 * el evento security.user.registered (ADR-003, ADR-009).
 */
@SpringBootApplication
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
