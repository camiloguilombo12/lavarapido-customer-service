package com.lavarapido.customer.domain.model;

import com.lavarapido.customer.domain.exception.InvalidValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LicensePlate")
class LicensePlateTest {

    @Nested
    @DisplayName("cuando se normaliza")
    class Normalization {

        @ParameterizedTest
        @ValueSource(strings = {"abc123", "ABC123", "abc-123", "ABC 123", "  abc-123  ", "a.b.c.1.2.3"})
        @DisplayName("queda igual sin importar como la escriban")
        void endsUpInTheSameCanonicalForm(String input) {
            assertEquals("ABC123", LicensePlate.of(input).value());
        }

        @Test
        @DisplayName("lo que sobra se corta en vez de fallar")
        void truncatesInsteadOfThrowing() {
            // Se corta aca y el detalle se pierde despues, en la politica de formato. Una placa
            // demasiado larga se rechaza igual, pero con un mensaje que si dice que paso.
            assertEquals("ABC123", LicensePlate.of("ABC1234").value());
        }
    }

    @Nested
    @DisplayName("cuando el valor no sirve")
    class Rejection {

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "---", "!!!"})
        @DisplayName("rechaza una placa sin letras ni numeros")
        void emptyOrPunctuationOnly(String input) {
            assertThrows(InvalidValueException.class, () -> LicensePlate.of(input));
        }

        @Test
        @DisplayName("rechaza null")
        void nullValue() {
            assertThrows(InvalidValueException.class, () -> LicensePlate.of(null));
        }

        @Test
        @DisplayName("pero deja los numeros solos para que los rechace la politica")
        void digitsAloneAreThePolicysJob() {
            // "123" tiene 3 caracteres, asi que como placa esta bien formada. Lo que no cumple el
            // formato de 3 letras y 3 numeros es regla del tipo de vehiculo, y esa la revisa
            // LicensePlatePolicy. Cada capa rechaza lo suyo y nada mas.
            assertEquals("123", LicensePlate.of("123").value());
        }
    }

    @Nested
    @DisplayName("cuando se muestra")
    class Formatted {

        @Test
        @DisplayName("al carro le pone guion despues de la tercera letra: ABC-123")
        void carPlate() {
            assertEquals("ABC-123", LicensePlate.of("ABC123").formatted());
        }

        @Test
        @DisplayName("a la moto tambien, en el mismo sitio: ABC-12D")
        void motorcyclePlate() {
            // La placa de moto no lleva guion de fabrica, pero el frontend siempre muestra uno
            // despues de la tercera letra, asi la vista y el input quedan iguales.
            assertEquals("ABC-12D", LicensePlate.of("ABC12D").formatted());
        }
    }

    @Nested
    @DisplayName("cuando se comparan")
    class Identity {

        @Test
        @DisplayName("dos placas escritas distinto son la misma placa")
        void normalizesBeforeComparing() {
            // Esto es lo que hace que el indice unico de la placa sirva: sin normalizar,
            // "abc-123" y "ABC123" pasarian el filtro y chocarian en la base.
            assertEquals(LicensePlate.of("abc-123"), LicensePlate.of("ABC123"));
        }

        @Test
        @DisplayName("placas distintas son placas distintas")
        void differentValuesAreNotEqual() {
            assertNotEquals(LicensePlate.of("ABC123"), LicensePlate.of("XYZ789"));
        }

        @Test
        @DisplayName("placas iguales tienen el mismo hash")
        void equalValuesShareTheHash() {
            assertEquals(LicensePlate.of("ABC-123").hashCode(), LicensePlate.of("ABC123").hashCode());
        }
    }

    @Test
    @DisplayName("matches dice si la placa cabe en un formato")
    void matchesPattern() {
        assertTrue(LicensePlate.of("ABC123").matches("^[A-Z]{3}[0-9]{3}$"));
    }
}
