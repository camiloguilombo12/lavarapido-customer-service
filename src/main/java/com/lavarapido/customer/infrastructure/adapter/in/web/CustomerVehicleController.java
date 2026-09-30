package com.lavarapido.customer.infrastructure.adapter.in.web;

import com.lavarapido.customer.domain.port.in.ListCustomerVehiclesUseCase;
import com.lavarapido.customer.domain.port.in.RegisterCustomerVehicleUseCase;
import com.lavarapido.customer.domain.port.in.RemoveCustomerVehicleUseCase;
import com.lavarapido.customer.domain.port.in.UpdateCustomerVehicleUseCase;
import com.lavarapido.customer.infrastructure.adapter.in.web.dto.RegisterVehicleRequest;
import com.lavarapido.customer.infrastructure.adapter.in.web.dto.UpdateVehicleRequest;
import com.lavarapido.customer.infrastructure.adapter.in.web.dto.VehicleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Los vehiculos del cliente que llama.
 *
 * Ningun metodo recibe el id del cliente: sale del token. Si un cliente inventa un id en la ruta
 * no alcanza a tocar el vehiculo de otro, porque el dominio lo busca dentro de su cuenta y
 * responde 404.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
class CustomerVehicleController {

    private final ListCustomerVehiclesUseCase listVehicles;
    private final RegisterCustomerVehicleUseCase registerVehicle;
    private final UpdateCustomerVehicleUseCase updateVehicle;
    private final RemoveCustomerVehicleUseCase removeVehicle;

    CustomerVehicleController(ListCustomerVehiclesUseCase listVehicles,
                              RegisterCustomerVehicleUseCase registerVehicle,
                              UpdateCustomerVehicleUseCase updateVehicle,
                              RemoveCustomerVehicleUseCase removeVehicle) {
        this.listVehicles = listVehicles;
        this.registerVehicle = registerVehicle;
        this.updateVehicle = updateVehicle;
        this.removeVehicle = removeVehicle;
    }

    @GetMapping
    List<VehicleResponse> list(@AuthenticationPrincipal Jwt jwt) {
        long userId = AuthenticatedUser.from(jwt).userId();
        return listVehicles.execute(userId).stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @PostMapping
    ResponseEntity<VehicleResponse> register(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody RegisterVehicleRequest request) {
        long userId = AuthenticatedUser.from(jwt).userId();
        VehicleResponse created = VehicleResponse.from(registerVehicle.execute(userId, request.toCommand()));
        return ResponseEntity
                .created(UriComponentsBuilder.fromPath("/api/v1/vehicles/{id}").build(created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    VehicleResponse update(@AuthenticationPrincipal Jwt jwt,
                           @PathVariable long id,
                           @Valid @RequestBody UpdateVehicleRequest request) {
        long userId = AuthenticatedUser.from(jwt).userId();
        return VehicleResponse.from(updateVehicle.execute(userId, id, request.toCommand()));
    }

    /**
     * Borrado logico, asi que responde 204 y no un cuerpo con el vehiculo: la fila sigue ahi y
     * solo tiene deleted_at. Devolver el vehiculo "borrado" confundiria al frontend.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        long userId = AuthenticatedUser.from(jwt).userId();
        removeVehicle.execute(userId, id);
    }
}