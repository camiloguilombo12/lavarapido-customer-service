package com.lavarapido.customer.domain.service;

import com.lavarapido.customer.domain.exception.InvalidValueException;
import com.lavarapido.customer.domain.model.LicensePlate;
import com.lavarapido.customer.domain.model.VehicleTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LicensePlatePolicy")
class LicensePlatePolicyTest {

    private final LicensePlatePolicy policy = new LicensePlatePolicy();

    @Nested
    @DisplayName("dependiendo del tipo de vehiculo")
    class ByVehicleType {

        @ParameterizedTest
        @EnumSource(value = VehicleTypeCode.class, names = {"CAR", "SEDAN", "SUV", "PICKUP", "TRUCK"})
        @DisplayName("el carro y las camionetas aceptan ABC123")
        void acceptsCarPlate(VehicleTypeCode type) {
            LicensePlate plate = LicensePlate.of("ABC123");
            assertSame(plate, policy.validate(plate, type));
        }

        @ParameterizedTest
        @EnumSource(value = VehicleTypeCode.class, names = {"CAR", "SEDAN", "SUV", "PICKUP", "TRUCK"})
        @DisplayName("el carro y las camionetas rechazan la placa de moto")
        void rejectsMotorcyclePlateOnCar(VehicleTypeCode type) {
            LicensePlate plate = LicensePlate.of("ABC12D");
            assertThrows(InvalidValueException.class, () -> policy.validate(plate, type));
        }

        @Test
        @DisplayName("la moto si acepta ABC12D")
        void acceptsMotorcyclePlate() {
            LicensePlate plate = LicensePlate.of("ABC12D");
            assertSame(plate, policy.validate(plate, VehicleTypeCode.MOTO));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABC123", "123ABC", "ABCD12", "ABC1D"})
        @DisplayName("la moto rechaza todo lo demas")
        void rejectsCarPlateOnMotorcycle(String input) {
            LicensePlate plate = LicensePlate.of(input);
            assertThrows(InvalidValueException.class, () -> policy.validate(plate, VehicleTypeCode.MOTO));
        }
    }

    @Nested
    @DisplayName("el error que lanza")
    class TheError {

        @Test
        @DisplayName("trae un code fijo que el frontend traduce")
        void hasAStableCode() {
            // El frontend mapea este codigo a un mensaje en español o ingles. Si cambia, hay que
            // cambiar los dos lados.
            InvalidValueException thrown = assertThrows(InvalidValueException.class,
                    () -> policy.validate(LicensePlate.of("ABC12D"), VehicleTypeCode.CAR));
            assertEquals("INVALID_PLATE", thrown.code());
        }

        @Test
        @DisplayName("dice cual era el formato esperado, para que el formulario lo muestre")
        void explainsTheExpectedFormat() {
            InvalidValueException thrown = assertThrows(InvalidValueException.class,
                    () -> policy.validate(LicensePlate.of("ABC123"), VehicleTypeCode.MOTO));
            assertTrue(thrown.getMessage().contains("ABC12D"),
                    "el mensaje deberia decir el formato esperado, pero fue: " + thrown.getMessage());
        }
    }

    @Test
    @DisplayName("expone el formato y un ejemplo de cada tipo, para el formulario")
    void exposesPatternAndExample() {
        assertEquals("^[A-Z]{3}[0-9]{2}[A-Z]$", policy.patternFor(VehicleTypeCode.MOTO));
        assertEquals("^[A-Z]{3}[0-9]{3}$", policy.patternFor(VehicleTypeCode.CAR));
        assertEquals("ABC12D", policy.exampleFor(VehicleTypeCode.MOTO));
        assertEquals("ABC123", policy.exampleFor(VehicleTypeCode.SUV));
    }
}
