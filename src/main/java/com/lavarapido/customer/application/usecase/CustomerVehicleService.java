package com.lavarapido.customer.application.usecase;

import com.lavarapido.customer.domain.exception.CustomerNotProvisionedException;
import com.lavarapido.customer.domain.exception.InvalidValueException;
import com.lavarapido.customer.domain.exception.PlateAlreadyRegisteredException;
import com.lavarapido.customer.domain.model.CustomerAccount;
import com.lavarapido.customer.domain.model.CustomerVehicle;
import com.lavarapido.customer.domain.model.LicensePlate;
import com.lavarapido.customer.domain.model.VehicleText;
import com.lavarapido.customer.domain.model.VehicleType;
import com.lavarapido.customer.domain.model.VehicleTypeCode;
import com.lavarapido.customer.domain.port.in.CustomerVehicleView;
import com.lavarapido.customer.domain.port.in.GetCustomerVehicleUseCase;
import com.lavarapido.customer.domain.port.in.ListCustomerVehiclesUseCase;
import com.lavarapido.customer.domain.port.in.RegisterCustomerVehicleCommand;
import com.lavarapido.customer.domain.port.in.RegisterCustomerVehicleUseCase;
import com.lavarapido.customer.domain.port.in.RemoveCustomerVehicleUseCase;
import com.lavarapido.customer.domain.port.in.UpdateCustomerVehicleCommand;
import com.lavarapido.customer.domain.port.in.UpdateCustomerVehicleUseCase;
import com.lavarapido.customer.domain.port.out.CustomerAccountRepository;
import com.lavarapido.customer.domain.port.out.CustomerVehicleRepository;
import com.lavarapido.customer.domain.port.out.DomainEventPublisher;
import com.lavarapido.customer.domain.port.out.IdentityDirectory;
import com.lavarapido.customer.domain.port.out.VehicleTypeRepository;
import com.lavarapido.customer.domain.service.LicensePlatePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Alta, edicion, borrado y consulta de los vehiculos de un cliente.
 *
 * El id del cliente no se recibe en ningun metodo, sale del token. Esa es la diferencia entre
 * "el cliente gestiona sus vehiculos" y "cualquiera gestiona los vehiculos de cualquiera".
 */
@Service
@Transactional
public class CustomerVehicleService implements
        RegisterCustomerVehicleUseCase,
        UpdateCustomerVehicleUseCase,
        RemoveCustomerVehicleUseCase,
        ListCustomerVehiclesUseCase,
        GetCustomerVehicleUseCase {

    private final CustomerAccountRepository accounts;
    private final CustomerVehicleRepository vehicles;
    private final VehicleTypeRepository vehicleTypes;
    private final DomainEventPublisher events;
    private final LicensePlatePolicy platePolicy;
    private final IdentityDirectory identity;
    private final Clock clock;

    public CustomerVehicleService(CustomerAccountRepository accounts,
                                  CustomerVehicleRepository vehicles,
                                  VehicleTypeRepository vehicleTypes,
                                  DomainEventPublisher events,
                                  LicensePlatePolicy platePolicy,
                                  IdentityDirectory identity,
                                  Clock clock) {
        this.accounts = accounts;
        this.vehicles = vehicles;
        this.vehicleTypes = vehicleTypes;
        this.events = events;
        this.platePolicy = platePolicy;
        this.identity = identity;
        this.clock = clock;
    }

    @Override
    public List<CustomerVehicleView> execute(long userId) {
        return requireAccount(userId).activeVehicles().stream()
                .map(VehicleViews::of)
                .toList();
    }

    /** Un vehiculo de SU cuenta: si es de otro cliente o no existe, 404 (VehicleNotFoundException). */
    @Override
    public CustomerVehicleView get(long userId, long vehicleId) {
        return VehicleViews.of(requireAccount(userId).findActiveVehicle(vehicleId));
    }

    @Override
    public CustomerVehicleView execute(long userId, RegisterCustomerVehicleCommand command) {
        CustomerAccount account = requireAccount(userId);

        VehicleType type = requireSelectableType(command.vehicleType());
        LicensePlate plate = LicensePlate.of(command.licensePlate());
        platePolicy.validate(plate, type.code());
        requirePlateFree(plate, 0L);

        CustomerVehicle registered = account.registerVehicle(plate, type,
                VehicleText.brand(command.brand()),
                VehicleText.model(command.model()),
                VehicleText.color(command.color()),
                now());

        accounts.save(account);
        events.publish(account.pullEvents());

        return VehicleViews.of(registered);
    }

    @Override
    public CustomerVehicleView execute(long userId, long vehicleId, UpdateCustomerVehicleCommand command) {
        CustomerAccount account = requireAccount(userId);
        // Al buscar dentro de SU cuenta, un vehiculo ajeno no aparece: 404 y no 403.
        CustomerVehicle existing = account.findActiveVehicle(vehicleId);

        VehicleType type = requireSelectableType(command.vehicleType());
        LicensePlate plate = LicensePlate.of(command.licensePlate());
        platePolicy.validate(plate, type.code());
        // Se excluye el propio vehiculo: si la placa no cambio, el conflicto es consigo mismo.
        requirePlateFree(plate, vehicleId);

        account.updateVehicle(vehicleId, plate, type,
                VehicleText.brand(command.brand()),
                VehicleText.model(command.model()),
                VehicleText.color(command.color()),
                now());

        accounts.save(account);
        events.publish(account.pullEvents());

        return VehicleViews.of(existing);
    }

    @Override
    public void execute(long userId, long vehicleId) {
        CustomerAccount account = requireAccount(userId);
        account.removeVehicle(vehicleId, now());
        accounts.save(account);
        events.publish(account.pullEvents());
    }

    /**
     * La cuenta se busca por el user_id del token. Si no esta es porque el evento de registro no
     * llego (RabbitMQ apagado, o cuentas creadas antes de conectar los eventos): se le pide el
     * person_id al security-service y se crea el perfil en ese momento. Si ni asi se puede, 409:
     * la cuenta si existe, lo que falta es su perfil.
     */
    private CustomerAccount requireAccount(long userId) {
        return accounts.findByUserId(userId).orElseGet(() -> provision(userId));
    }

    private CustomerAccount provision(long userId) {
        long personId = identity.personIdOf(userId)
                .orElseThrow(() -> new CustomerNotProvisionedException(userId));
        Instant now = now();
        CustomerAccount saved = accounts.save(CustomerAccount.provision(personId, userId,
                LocalDate.ofInstant(now, clock.getZone()), now));
        events.publish(saved.pullEvents());
        return saved;
    }

    /**
     * El tipo tiene que existir en el catalogo y estar activo. Resolverlo aqui, en vez de mandar
     * el texto crudo a la base, convierte un codigo inventado en un 400 con mensaje claro y no en
     * un error de clave foranea.
     */
    private VehicleType requireSelectableType(String rawCode) {
        VehicleTypeCode code = VehicleTypeCode.of(rawCode);
        VehicleType type = vehicleTypes.findByCode(code)
                .orElseThrow(() -> new InvalidValueException("INVALID_VEHICLE_TYPE",
                        "The vehicle type " + code + " is not in the catalog"));
        if (!type.isSelectable()) {
            throw new InvalidValueException("INVALID_VEHICLE_TYPE",
                    "The vehicle type " + code + " is no longer available");
        }
        return type;
    }

    private void requirePlateFree(LicensePlate plate, long vehicleIdBeingEdited) {
        vehicles.findActiveVehicleIdByPlate(plate)
                .filter(existingId -> existingId != vehicleIdBeingEdited)
                .ifPresent(existingId -> {
                    throw new PlateAlreadyRegisteredException(plate.value());
                });
    }


    private Instant now() {
        return clock.instant();
    }
}
