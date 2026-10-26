package com.lavarapido.customer.infrastructure.adapter.out.persistence;

import com.lavarapido.customer.domain.model.CustomerVehicle;
import com.lavarapido.customer.domain.model.VehicleType;
import com.lavarapido.customer.domain.model.VehicleTypeCode;
import com.lavarapido.customer.infrastructure.adapter.out.persistence.entity.CustomerVehicleJpaEntity;
import com.lavarapido.customer.infrastructure.adapter.out.persistence.repository.CustomerVehicleJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("Vehículos en la base")
class CustomerVehiclePersistenceAdapterTest {

    private static final VehicleType CAR =
            new VehicleType((short) 1, VehicleTypeCode.CAR, "Automovil", new BigDecimal("1.00"), (short) 1, true);

    private final CustomerVehicleJpaRepository repository = mock(CustomerVehicleJpaRepository.class);
    private final VehicleTypePersistenceAdapter catalog = mock(VehicleTypePersistenceAdapter.class);
    private final CustomerVehiclePersistenceAdapter adapter = new CustomerVehiclePersistenceAdapter(repository, catalog);

    @Test
    @DisplayName("E-16: un vehículo guardado con una placa vieja de más de 6 caracteres se sigue leyendo")
    void readsAVehicleWithAnOldLongPlate() {
        // la columna es NVARCHAR(10): antes de E-16 pudo quedar "ABC12345"; leerla no puede fallar
        CustomerVehicleJpaEntity old = mock(CustomerVehicleJpaEntity.class);
        given(old.getId()).willReturn(15L);
        given(old.getCustomerId()).willReturn(9L);
        given(old.getLicensePlate()).willReturn("ABC12345");
        given(old.getVehicleTypeId()).willReturn((short) 1);
        given(old.getBrand()).willReturn("Mazda");
        given(old.getCreatedAt()).willReturn(Instant.parse("2026-09-01T12:00:00Z"));
        given(catalog.requireById((short) 1)).willReturn(CAR);
        given(repository.findByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(9L)).willReturn(List.of(old));

        List<CustomerVehicle> vehicles = adapter.loadByCustomerId(9L);

        assertEquals(1, vehicles.size());
        assertEquals("ABC12345", vehicles.getFirst().licensePlate().value());
    }
}
