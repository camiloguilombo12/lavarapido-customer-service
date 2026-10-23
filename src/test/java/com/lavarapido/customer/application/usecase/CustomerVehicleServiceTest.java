package com.lavarapido.customer.application.usecase;

import com.lavarapido.customer.domain.event.CustomerProvisioned;
import com.lavarapido.customer.domain.exception.CustomerNotProvisionedException;
import com.lavarapido.customer.domain.exception.VehicleNotFoundException;
import com.lavarapido.customer.domain.model.CustomerAccount;
import com.lavarapido.customer.domain.model.CustomerVehicle;
import com.lavarapido.customer.domain.model.LicensePlate;
import com.lavarapido.customer.domain.model.VehicleText;
import com.lavarapido.customer.domain.model.VehicleType;
import com.lavarapido.customer.domain.model.VehicleTypeCode;
import com.lavarapido.customer.domain.port.in.CustomerVehicleView;
import com.lavarapido.customer.domain.port.in.OwnedVehicleView;
import com.lavarapido.customer.domain.port.out.CustomerAccountRepository;
import com.lavarapido.customer.domain.port.out.CustomerVehicleRepository;
import com.lavarapido.customer.domain.port.out.DomainEventPublisher;
import com.lavarapido.customer.domain.port.out.IdentityDirectory;
import com.lavarapido.customer.domain.port.out.VehicleTypeRepository;
import com.lavarapido.customer.domain.service.LicensePlatePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("Vehiculos del cliente (casos de uso)")
class CustomerVehicleServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-30T15:00:00Z");
    private static final VehicleType CAR =
            new VehicleType((short) 1, VehicleTypeCode.CAR, "Automovil", new BigDecimal("1.00"), (short) 1, true);

    private final CustomerAccountRepository accounts = mock(CustomerAccountRepository.class);
    private final CustomerVehicleRepository vehicles = mock(CustomerVehicleRepository.class);
    private final VehicleTypeRepository types = mock(VehicleTypeRepository.class);
    private final DomainEventPublisher events = mock(DomainEventPublisher.class);
    private final IdentityDirectory identity = mock(IdentityDirectory.class);

    private CustomerVehicleService service;
    private VehicleLookupService lookup;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new CustomerVehicleService(accounts, vehicles, types, events, new LicensePlatePolicy(), identity, clock);
        lookup = new VehicleLookupService(accounts, vehicles);
    }

    private static CustomerAccount accountWithCar(long customerId, long userId, long vehicleId) {
        CustomerVehicle car = CustomerVehicle.restore(vehicleId, customerId, LicensePlate.of("ABC123"), CAR,
                VehicleText.brand("Mazda"), VehicleText.model("CX-5"), VehicleText.color("Rojo"), NOW, null);
        return CustomerAccount.restore(customerId, 50L, userId, 0, LocalDate.parse("2026-01-01"), NOW, List.of(car));
    }

    @Test
    @DisplayName("si el perfil no existe lo crea con el person_id de security-service")
    void provisionsTheProfileOnFirstUse() {
        given(accounts.findByUserId(7L)).willReturn(Optional.empty());
        given(identity.personIdOf(7L)).willReturn(Optional.of(33L));
        given(accounts.save(any())).willAnswer(invocation -> {
            CustomerAccount account = invocation.getArgument(0);
            account.assignId(99L);
            return account;
        });

        List<CustomerVehicleView> result = service.execute(7L);

        assertTrue(result.isEmpty());
        ArgumentCaptor<CustomerAccount> saved = ArgumentCaptor.forClass(CustomerAccount.class);
        verify(accounts).save(saved.capture());
        assertEquals(33L, saved.getValue().personId());
        assertEquals(7L, saved.getValue().userId());
        // el evento sale con el id real, no con 0
        verify(events).publish(List.of(new CustomerProvisioned(99L, 33L, 7L, NOW)));
    }

    @Test
    @DisplayName("si security-service no reconoce la cuenta responde 409 y no guarda nada")
    void failsWhenTheIdentityIsUnknown() {
        given(accounts.findByUserId(7L)).willReturn(Optional.empty());
        given(identity.personIdOf(7L)).willReturn(Optional.empty());

        assertThrows(CustomerNotProvisionedException.class, () -> service.execute(7L));
        verify(accounts, never()).save(any());
    }

    @Test
    @DisplayName("con el perfil ya creado no consulta a security-service")
    void existingProfileSkipsTheLookup() {
        given(accounts.findByUserId(7L)).willReturn(Optional.of(accountWithCar(1L, 7L, 5L)));

        assertEquals(1, service.execute(7L).size());
        verify(identity, never()).personIdOf(anyLong());
    }

    @Test
    @DisplayName("un vehiculo propio se devuelve; uno ajeno es 404")
    void getOnlyReturnsOwnVehicles() {
        given(accounts.findByUserId(7L)).willReturn(Optional.of(accountWithCar(1L, 7L, 5L)));

        assertEquals("ABC123", service.get(7L, 5L).licensePlate());
        assertThrows(VehicleNotFoundException.class, () -> service.get(7L, 6L));
    }

    @Test
    @DisplayName("el admin encuentra un vehiculo por placa junto con su dueño")
    void lookupByPlate() {
        given(vehicles.findActiveVehicleIdByPlate(LicensePlate.of("abc-123"))).willReturn(Optional.of(5L));
        given(vehicles.findCustomerIdOfActiveVehicle(5L)).willReturn(Optional.of(1L));
        given(accounts.findById(1L)).willReturn(Optional.of(accountWithCar(1L, 7L, 5L)));

        OwnedVehicleView found = lookup.byPlate("abc-123").orElseThrow();

        assertEquals(7L, found.ownerUserId());
        assertEquals("CAR", found.vehicle().vehicleType());
    }

    @Test
    @DisplayName("por ids ignora los que no existen o estan borrados")
    void lookupByIdsSkipsMissing() {
        given(vehicles.findCustomerIdOfActiveVehicle(5L)).willReturn(Optional.of(1L));
        given(vehicles.findCustomerIdOfActiveVehicle(8L)).willReturn(Optional.empty());
        given(accounts.findById(1L)).willReturn(Optional.of(accountWithCar(1L, 7L, 5L)));

        List<OwnedVehicleView> found = lookup.byIds(List.of(5L, 8L, 5L));

        assertEquals(1, found.size());
        assertEquals(5L, found.getFirst().vehicle().id());
    }
}
