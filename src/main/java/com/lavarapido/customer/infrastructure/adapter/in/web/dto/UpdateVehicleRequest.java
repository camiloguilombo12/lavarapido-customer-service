package com.lavarapido.customer.infrastructure.adapter.in.web.dto;

import com.lavarapido.customer.domain.port.in.UpdateCustomerVehicleCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de PUT /api/v1/vehicles/{id}.
 *
 * Es un PUT completo y no un PATCH porque el formulario de Angular manda los cuatro campos
 * siempre, y un PUT deja claro que lo que no viene se borra. El id del vehiculo va en la ruta
 * y el del cliente sale del token, ninguno de los dos viene aqui.
 */
public record UpdateVehicleRequest(
        @NotBlank(message = "licensePlate is required")
        @Size(max = 10, message = "licensePlate is too long")
        String licensePlate,

        @NotBlank(message = "vehicleType is required")
        String vehicleType,

        @Size(max = 50, message = "brand is too long")
        String brand,

        @Size(max = 50, message = "model is too long")
        String model,

        @Size(max = 30, message = "color is too long")
        String color
) {

    public UpdateCustomerVehicleCommand toCommand() {
        return new UpdateCustomerVehicleCommand(licensePlate, vehicleType, brand, model, color);
    }
}