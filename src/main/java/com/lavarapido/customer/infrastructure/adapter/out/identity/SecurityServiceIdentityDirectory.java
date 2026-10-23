package com.lavarapido.customer.infrastructure.adapter.out.identity;

import com.lavarapido.customer.domain.port.out.IdentityDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/**
 * Le pregunta al security-service GET /users/me con el mismo token del que llama (REST, ADR-004).
 *
 * Se reenvia el token del usuario y no uno propio del servicio: asi security-service solo
 * responde sobre la cuenta dueña del token, y este servicio no necesita credenciales extra.
 * Ademas se revisa que el id que responde sea el mismo que se pidio.
 */
@Component
class SecurityServiceIdentityDirectory implements IdentityDirectory {

    private static final Logger log = LoggerFactory.getLogger(SecurityServiceIdentityDirectory.class);

    private final RestClient client;

    SecurityServiceIdentityDirectory(@Value("${app.security-service.base-url}") String baseUrl,
                                     @Value("${app.security-service.timeout:3s}") Duration timeout) {
        // tiempo maximo corto: si security-service no responde, el cliente ve 409 y no se queda colgado
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Optional<Long> personIdOf(long userId) {
        Optional<String> token = currentToken();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        try {
            CurrentUser user = client.get()
                    .uri("/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get())
                    .retrieve()
                    .body(CurrentUser.class);
            if (user == null || user.id() != userId || user.personId() <= 0) {
                return Optional.empty();
            }
            return Optional.of(user.personId());
        } catch (RestClientException e) {
            log.warn("Could not read the person of account {} from security-service: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<String> currentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return Optional.of(jwt.getToken().getTokenValue());
        }
        return Optional.empty();
    }

    /** Solo los campos que se usan de la respuesta de /users/me. */
    record CurrentUser(long id, long personId) {
    }
}
