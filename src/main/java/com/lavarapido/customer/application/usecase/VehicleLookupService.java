package com.lavarapido.customer.application.usecase;

import com.lavarapido.customer.domain.exception.InvalidValueException;
import com.lavarapido.customer.domain.model.CustomerAccount;
import com.lavarapido.customer.domain.model.LicensePlate;
import com.lavarapido.customer.domain.port.in.LookupVehiclesUseCase;
import com.lavarapido.customer.domain.port.in.OwnedVehicleView;
import com.lavarapido.customer.domain.port.out.CustomerAccountRepository;
import com.lavarapido.customer.domain.port.out.CustomerVehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Consultas del administrador (y de booking-service con el token del admin): un vehiculo de
 * cualquier cliente junto con el user_id de su dueño.
 */
@Service
@Transactional(readOnly = true)
public class VehicleLookupService implements LookupVehiclesUseCase {

    /** Tope de ids por consulta: una pantalla de reservas no pide mas que una pagina. */
    static final int MAX_IDS = 100;

    private final CustomerAccountRepository accounts;
    private final CustomerVehicleRepository vehicles;

    public VehicleLookupService(CustomerAccountRepository accounts, CustomerVehicleRepository vehicles) {
        this.accounts = accounts;
        this.vehicles = vehicles;
    }

    @Override
    public List<OwnedVehicleView> byIds(List<Long> vehicleIds) {
        if (vehicleIds.size() > MAX_IDS) {
            throw new InvalidValueException("TOO_MANY_IDS", "At most " + MAX_IDS + " vehicle ids per request");
        }
        Set<Long> ids = new LinkedHashSet<>(vehicleIds);
        ids.removeIf(Objects::isNull);
        // un solo SELECT para los dueños y cada cuenta se carga una vez, aunque tenga varios vehiculos
        Map<Long, Long> ownerOf = vehicles.findCustomerIdsOfActiveVehicles(ids);
        Map<Long, Optional<CustomerAccount>> accountsById = new HashMap<>();
        List<OwnedVehicleView> found = new ArrayList<>();
        for (Long vehicleId : ids) {
            Long customerId = ownerOf.get(vehicleId);
            if (customerId != null) {
                accountsById.computeIfAbsent(customerId, accounts::findById)
                        .map(account -> toView(account, vehicleId))
                        .ifPresent(found::add);
            }
        }
        return found;
    }

    @Override
    public Optional<OwnedVehicleView> byPlate(String licensePlate) {
        return vehicles.findActiveVehicleIdByPlate(LicensePlate.of(licensePlate)).flatMap(this::find);
    }

    private Optional<OwnedVehicleView> find(long vehicleId) {
        return vehicles.findCustomerIdOfActiveVehicle(vehicleId)
                .flatMap(accounts::findById)
                .map(account -> toView(account, vehicleId));
    }

    private static OwnedVehicleView toView(CustomerAccount account, long vehicleId) {
        return new OwnedVehicleView(VehicleViews.of(account.findActiveVehicle(vehicleId)), account.userId());
    }
}
