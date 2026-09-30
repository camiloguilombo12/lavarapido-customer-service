package com.lavarapido.customer.infrastructure.adapter.in.web.dto;

import com.lavarapido.customer.domain.port.in.RegisterCustomerVehicleCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de POST /api/v1/vehicles. Aqui solo se revisa lo que el bean validation puede:
 * que no venga vacio y que no sea enorme. El formato de la placa lo decide LicensePlatePolicy.
 */
public record RegisterVehicleRequest(
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

    public RegisterCustomerVehicleCommand toCommand() {
        return new RegisterCustomerVehicleCommand(licensePlate, vehicleType, brand, model, color);
    }
}