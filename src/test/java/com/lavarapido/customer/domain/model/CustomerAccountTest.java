package com.lavarapido.customer.domain.model;

import com.lavarapido.customer.domain.event.DomainEvent;
import com.lavarapido.customer.domain.event.VehicleRegistered;
import com.lavarapido.customer.domain.event.VehicleRemoved;
import com.lavarapido.customer.domain.exception.InvalidValueException;
import com.lavarapido.customer.domain.exception.VehicleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CustomerAccount")
class CustomerAccountTest {

    private static final Instant NOW = Instant.parse("2026-09-29T15:00:00Z");

    private VehicleType car;
    private VehicleType moto;
    private CustomerAccount account;

    @BeforeEach
    void setUp() {
        car = new VehicleType((short) 1, VehicleTypeCode.CAR, "Automovil", new BigDecimal("1.00"), (short) 1, true);
        moto = new VehicleType((short) 6, VehicleTypeCode.MOTO, "Motocicleta", new BigDecimal("0.80"), (short) 6, true);
        account = CustomerAccount.provision(10L, 7L, LocalDate.parse("2026-09-29"), NOW);
    }

    private CustomerVehicle addCar() {
        return account.registerVehicle(LicensePlate.of("ABC123"), car,
                VehicleText.brand("Mazda"), VehicleText.model("CX-5"), VehicleText.color("Rojo"), NOW);
    }

    @Nested
    @DisplayName("cuando se crea la cuenta")
    class Provisioning {

        @Test
        @DisplayName("empieza con cero puntos y la fecha de hoy")
        void startsEmpty() {
            assertEquals(0, account.loyaltyPoints());
            assertEquals(LocalDate.parse("2026-09-29"), account.customerSince());
            assertTrue(account.activeVehicles().isEmpty());
        }

        @Test
        @DisplayName("avisa que se creo, para que booking-service sepa de que cliente es")
        void announcesItself() {
            // booking-service guarda el customer_id en sus reservas justamente porque no puede
            // llamar a este servicio en cada lectura.
            List<DomainEvent> events = account.pullEvents();
            assertEquals(1, events.size());
            assertEquals("customer.provisioned", events.getFirst().eventType());
        }

        @Test
        @DisplayName("sacar los eventos los vacia, para no mandarlos dos veces")
        void pullEmptiesTheQueue() {
            account.pullEvents();
            assertTrue(account.pullEvents().isEmpty());
        }
    }

    @Nested
    @DisplayName("cuando se registra un vehiculo")
    class Registering {

        @Test
        @DisplayName("aparece entre los activos")
        void showsUp() {
            addCar();
            assertEquals(1, account.activeVehicles().size());
        }

        @Test
        @DisplayName("avisa con la placa, que es lo que necesita booking-service")
        void announcesIt() {
            addCar();
            assertTrue(account.pullEvents().stream().anyMatch(VehicleRegistered.class::isInstance));
        }

        @Test
        @DisplayName("reconoce su propia placa, que se revisa antes de insertar")
        void recognizesItsOwnPlate() {
            addCar();
            assertTrue(account.hasActiveVehicleWithPlate(LicensePlate.of("abc-123")));
        }
    }

    @Nested
    @DisplayName("cuando se borra un vehiculo")
    class Removing {

        @Test
        @DisplayName("deja de estar activo pero la fila se queda, y eso libera la placa")
        void becomesInactive() {
            CustomerVehicle vehicle = addCar();
            account.removeVehicle(vehicle.customerVehicleId(), NOW);

            assertTrue(account.activeVehicles().isEmpty());
            assertEquals(1, account.allVehicles().size());
        }

        @Test
        @DisplayName("borrarlo otra vez es 404 y no mueve la fecha del primer borrado")
        void secondRemovalIsNotFound() {
            CustomerVehicle vehicle = addCar();
            vehicle.assignId(1L);
            account.removeVehicle(1L, NOW);
            Instant firstDeletion = account.allVehicles().getFirst().deletedAt();

            // Borrado dos veces: el segundo responde 404 igual que si nunca hubiera existido, y la
            // fecha del primer borrado no se toca.
            assertThrows(VehicleNotFoundException.class, () -> account.removeVehicle(1L, NOW.plusSeconds(60)));
            assertEquals(firstDeletion, account.allVehicles().getFirst().deletedAt());
        }

        @Test
        @DisplayName("avisa el borrado, para que booking-service deje de ofrecerlo")
        void announcesIt() {
            CustomerVehicle vehicle = addCar();
            account.pullEvents();
            account.removeVehicle(vehicle.customerVehicleId(), NOW);
            assertTrue(account.pullEvents().stream().anyMatch(VehicleRemoved.class::isInstance));
        }
    }

    @Nested
    @DisplayName("cuando se busca un vehiculo")
    class LookingUp {

        @Test
        @DisplayName("uno borrado es igual a uno que nunca existio")
        void removedLooksLikeMissing() {
            CustomerVehicle vehicle = addCar();
            vehicle.assignId(1L);
            account.removeVehicle(1L, NOW);

            // Un 403 confirmaria que ese vehiculo existe, y eso ya es informacion de otro.
            assertThrows(VehicleNotFoundException.class, () -> account.findActiveVehicle(1L));
        }

        @Test
        @DisplayName("un id que nunca estuvo en la cuenta tampoco se encuentra")
        void unknownIdIsNotFound() {
            assertThrows(VehicleNotFoundException.class, () -> account.findActiveVehicle(999L));
        }
    }

    @Nested
    @DisplayName("los puntos de fidelizacion")
    class Points {

        @Test
        @DisplayName("son el saldo que llega de payment-service, no un calculo")
        void areAssigned() {
            // El saldo real esta en payment.loyalty_transaction. Aca solo se copia para mostrarlo.
            account.setLoyaltyPoints(350);
            assertEquals(350, account.loyaltyPoints());
        }

        @Test
        @DisplayName("no pueden ser negativos, y el CHECK de la base tampoco los dejaria")
        void cannotBeNegative() {
            assertThrows(InvalidValueException.class, () -> account.setLoyaltyPoints(-1));
        }
    }

    @Test
    @DisplayName("un carro y una moto pueden tener formatos de placa distintos al tiempo")
    void mixesPlateFormats() {
        // No hay un solo formato global: la regla depende del tipo, y un cliente puede tener las
        // dos cosas registradas a la vez.
        account.registerVehicle(LicensePlate.of("ABC123"), car, VehicleText.empty(), VehicleText.empty(),
                VehicleText.empty(), NOW);
        account.registerVehicle(LicensePlate.of("XYZ12D"), moto, VehicleText.empty(), VehicleText.empty(),
                VehicleText.empty(), NOW);

        assertEquals(2, account.activeVehicles().size());
    }
}
