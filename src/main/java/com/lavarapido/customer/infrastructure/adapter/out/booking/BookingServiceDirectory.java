package com.lavarapido.customer.infrastructure.adapter.out.booking;

import com.lavarapido.customer.domain.exception.DependencyUnavailableException;
import com.lavarapido.customer.domain.port.out.BookingDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Le pregunta a booking-service GET /internal/v1/bookings/active-count con la llave interna
 * (X-Internal-Key, INTERNAL_API_KEY). Es una ruta entre servicios, fuera de /api/v1: no pasa por el
 * gateway ni lleva el token del cliente. base-url es la raiz de booking, sin ruta (BOOKING_SERVICE_URL).
 *
 * Sin llave, o si booking no responde o contesta con error, es 503 BOOKING_SERVICE_UNAVAILABLE: el
 * vehiculo no se retira sin saber si tiene reservas.
 */
@Component
class BookingServiceDirectory implements BookingDirectory {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceDirectory.class);
    private static final String INTERNAL_HEADER = "X-Internal-Key";

    private final RestClient client;
    private final String internalKey;

    BookingServiceDirectory(@Value("${app.booking-service.base-url}") String baseUrl,
                            @Value("${app.booking-service.timeout:3s}") Duration timeout,
                            @Value("${app.internal.api-key:}") String internalKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.internalKey = internalKey == null ? "" : internalKey.trim();
    }

    @Override
    public long activeBookingsOf(long customerVehicleId) {
        if (internalKey.isEmpty()) {
            log.warn("INTERNAL_API_KEY is not set: cannot ask booking-service about vehicle {}", customerVehicleId);
            throw unavailable();
        }
        try {
            ActiveCount count = client.get()
                    .uri(uri -> uri.path("/internal/v1/bookings/active-count")
                            .queryParam("customerVehicleId", customerVehicleId).build())
                    .header(INTERNAL_HEADER, internalKey)
                    .retrieve()
                    .body(ActiveCount.class);
            if (count == null) {
                throw unavailable();
            }
            return count.activeBookings();
        } catch (RestClientException e) {
            log.warn("booking-service call failed for vehicle {}: {}", customerVehicleId, e.getMessage());
            throw unavailable();
        }
    }

    private static DependencyUnavailableException unavailable() {
        return new DependencyUnavailableException("BOOKING_SERVICE_UNAVAILABLE",
                "Could not check the bookings of the vehicle with booking-service");
    }

    /** La respuesta de booking-service (contrato BIZ-08). */
    record ActiveCount(long customerVehicleId, long activeBookings) {
    }
}
