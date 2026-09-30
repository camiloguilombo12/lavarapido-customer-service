package com.lavarapido.customer.infrastructure.config;

import com.lavarapido.customer.domain.exception.CustomerNotProvisionedException;
import com.lavarapido.customer.domain.exception.VehicleNotFoundException;
import com.lavarapido.customer.domain.port.in.CustomerVehicleView;
import com.lavarapido.customer.domain.port.in.GetCustomerVehicleUseCase;
import com.lavarapido.customer.domain.port.in.ListCustomerVehiclesUseCase;
import com.lavarapido.customer.domain.port.in.ListVehicleTypesUseCase;
import com.lavarapido.customer.domain.port.in.LookupVehiclesUseCase;
import com.lavarapido.customer.domain.port.in.OwnedVehicleView;
import com.lavarapido.customer.domain.port.in.RegisterCustomerVehicleUseCase;
import com.lavarapido.customer.domain.port.in.RemoveCustomerVehicleUseCase;
import com.lavarapido.customer.domain.port.in.UpdateCustomerVehicleUseCase;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP y reglas de seguridad con los casos de uso simulados. */
@WebMvcTest(properties = {
        "security.jwt.secret=" + CustomerApiWebTest.SECRET,
        "security.jwt.issuer=" + CustomerApiWebTest.ISSUER,
        "security.jwt.audience=" + CustomerApiWebTest.AUDIENCE,
        "security.jwt.access-token-ttl=1h"
})
@Import({SecurityConfig.class, JwtConfig.class, ProblemDetailsSecurityHandler.class, CorrelationIdFilter.class})
class CustomerApiWebTest {

    static final String SECRET = "test-secret-with-at-least-thirty-two-bytes!!";
    static final String ISSUER = "lavarapido-security-service";
    static final String AUDIENCE = "lavarapido-api";

    private static final CustomerVehicleView CAR = new CustomerVehicleView(5L, "ABC123", "ABC-123", "CAR",
            (short) 1, "Automovil", "Mazda", "CX-5", "Rojo", Instant.parse("2026-09-30T15:00:00Z"));

    @Autowired
    private MockMvc mvc;

    @MockitoBean private ListCustomerVehiclesUseCase listVehicles;
    @MockitoBean private RegisterCustomerVehicleUseCase registerVehicle;
    @MockitoBean private UpdateCustomerVehicleUseCase updateVehicle;
    @MockitoBean private RemoveCustomerVehicleUseCase removeVehicle;
    @MockitoBean private GetCustomerVehicleUseCase getVehicle;
    @MockitoBean private LookupVehiclesUseCase lookupVehicles;
    @MockitoBean private ListVehicleTypesUseCase listVehicleTypes;

    private static String token(long userId, List<String> roles) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(AUDIENCE))
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("roles", roles)
                .build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void vehiclesNeedAToken() throws Exception {
        mvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vehicleTypesArePublic() throws Exception {
        given(listVehicleTypes.execute()).willReturn(List.of());
        mvc.perform(get("/api/v1/vehicle-types"))
                .andExpect(status().isOk());
    }

    @Test
    void clientListsOwnVehicles() throws Exception {
        given(listVehicles.execute(7L)).willReturn(List.of(CAR));
        mvc.perform(get("/api/v1/vehicles").header("Authorization", token(7L, List.of("CLIENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licensePlate").value("ABC123"));
    }

    @Test
    void missingProfileIsAConflictWithItsCode() throws Exception {
        given(listVehicles.execute(7L)).willThrow(new CustomerNotProvisionedException(7L));
        mvc.perform(get("/api/v1/vehicles").header("Authorization", token(7L, List.of("CLIENT"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_PROVISIONED"));
    }

    @Test
    void clientGetsOneOwnVehicleOr404() throws Exception {
        given(getVehicle.get(7L, 5L)).willReturn(CAR);
        given(getVehicle.get(7L, 6L)).willThrow(new VehicleNotFoundException(6L));

        mvc.perform(get("/api/v1/vehicles/5").header("Authorization", token(7L, List.of("CLIENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        mvc.perform(get("/api/v1/vehicles/6").header("Authorization", token(7L, List.of("CLIENT"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminLookupIsForbiddenForClients() throws Exception {
        mvc.perform(get("/api/v1/admin/vehicles").param("ids", "5")
                        .header("Authorization", token(7L, List.of("CLIENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminFindsVehiclesWithTheirOwner() throws Exception {
        given(lookupVehicles.byIds(List.of(5L))).willReturn(List.of(new OwnedVehicleView(CAR, 7L)));
        given(lookupVehicles.byPlate("ABC-123")).willReturn(Optional.of(new OwnedVehicleView(CAR, 7L)));

        mvc.perform(get("/api/v1/admin/vehicles").param("ids", "5")
                        .header("Authorization", token(1L, List.of("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerUserId").value(7))
                .andExpect(jsonPath("$[0].vehicle.licensePlate").value("ABC123"));
        mvc.perform(get("/api/v1/admin/vehicles").param("plate", "ABC-123")
                        .header("Authorization", token(1L, List.of("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicle.id").value(5));
    }

    @Test
    void adminLookupNeedsAFilter() throws Exception {
        mvc.perform(get("/api/v1/admin/vehicles").header("Authorization", token(1L, List.of("ADMIN"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_FILTER"));
    }
}
