package com.lavarapido.customer.infrastructure.adapter.in.web;

import com.lavarapido.customer.domain.exception.CustomerNotProvisionedException;
import com.lavarapido.customer.domain.exception.DomainException;
import com.lavarapido.customer.domain.exception.PlateAlreadyRegisteredException;
import com.lavarapido.customer.domain.exception.VehicleNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todos los errores salen como application/problem+json con un code fijo que el frontend
 * traduce. Los detalles internos nunca llegan al cliente.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException exception) {
        return problem(statusOf(exception), exception.code(), exception.getMessage());
    }

    /**
     * La placa es unica entre todos los clientes y eso lo pone el indice unico de la base. El
     * dominio ya revisa antes de escribir, pero dos peticiones al tiempo pueden cruzarse, y si
     * eso pasa es 409 y no 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConstraintViolation(DataIntegrityViolationException exception) {
        log.warn("Integrity constraint violated: {}", exception.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "CONFLICT", "The data conflicts with an existing record");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request has invalid fields");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /** Los errores de Spring MVC (JSON mal formado, metodo incorrecto) tambien llevan code. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
                                                          WebRequest request) {
        if (body instanceof ProblemDetail problem && problem.getProperties() == null) {
            problem.setProperty("code", HttpStatus.valueOf(statusCode.value()).name());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static HttpStatus statusOf(DomainException exception) {
        return switch (exception) {
            // El vehiculo no existe, o es de otro cliente. Los dos casos son 404 a proposito:
            // un 403 confirmaria que ese vehiculo existe, y eso ya es informacion de otro.
            case VehicleNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case PlateAlreadyRegisteredException ignored -> HttpStatus.CONFLICT;
            // 409 y no 404: la cuenta si existe, lo que falta es que llegue el evento de
            // registro. Es un reintento, no un error del cliente.
            case CustomerNotProvisionedException ignored -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }
}
