package com.lavarapido.customer.infrastructure.adapter.out.booking;

import com.lavarapido.customer.domain.exception.DependencyUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BIZ-08: customer le pregunta a booking por las reservas activas de un vehiculo. */
@DisplayName("Cliente de booking-service")
class BookingServiceDirectoryTest {

    private static final String KEY = "internal-test-key";

    private HttpServer server;
    private String baseUrl;
    private volatile int status = 200;
    private volatile String lastPathAndQuery;
    private volatile String lastKey;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastPathAndQuery = exchange.getRequestURI().toString();
            lastKey = exchange.getRequestHeaders().getFirst("X-Internal-Key");
            byte[] body = "{\"customerVehicleId\":15,\"activeBookings\":2}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        // la raiz de booking, sin ruta (BOOKING_SERVICE_URL)
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    @DisplayName("pide active-count con la llave interna y devuelve cuantas hay")
    void asksTheInternalRouteWithTheKey() {
        BookingServiceDirectory directory = new BookingServiceDirectory(baseUrl, Duration.ofSeconds(2), KEY);

        assertEquals(2L, directory.activeBookingsOf(15L));
        assertEquals("/internal/v1/bookings/active-count?customerVehicleId=15", lastPathAndQuery);
        assertEquals(KEY, lastKey);
    }

    @Test
    @DisplayName("sin llave, o si booking contesta con error, es 503 BOOKING_SERVICE_UNAVAILABLE")
    void withoutKeyOrWithAnErrorItIsUnavailable() {
        DependencyUnavailableException noKey = assertThrows(DependencyUnavailableException.class,
                () -> new BookingServiceDirectory(baseUrl, Duration.ofSeconds(2), " ").activeBookingsOf(15L));
        assertEquals("BOOKING_SERVICE_UNAVAILABLE", noKey.code());

        status = 401;
        assertThrows(DependencyUnavailableException.class,
                () -> new BookingServiceDirectory(baseUrl, Duration.ofSeconds(2), KEY).activeBookingsOf(15L));
        status = 500;
        assertThrows(DependencyUnavailableException.class,
                () -> new BookingServiceDirectory(baseUrl, Duration.ofSeconds(2), KEY).activeBookingsOf(15L));
    }
}
