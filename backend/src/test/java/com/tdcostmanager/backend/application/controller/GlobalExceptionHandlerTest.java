package com.tdcostmanager.backend.application.controller;

import com.tdcostmanager.backend.application.dto.ApiError;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = Mockito.mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(request.getMethod()).thenReturn("POST");
    }

    @Test
    void testHandleNotFound() {
        EntityNotFoundException ex = new EntityNotFoundException("Proyecto no encontrado");
        ApiError response = exceptionHandler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.status());
        assertEquals("Not Found", response.error());
        assertEquals("Proyecto no encontrado", response.message());
        assertEquals("/api/v1/test", response.path());
        assertNotNull(response.timestamp());
    }

    @Test
    void testHandleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Parámetro inválido");
        ApiError response = exceptionHandler.handleIllegalArgument(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status());
        assertEquals("Bad Request", response.error());
        assertEquals("Parámetro inválido", response.message());
    }

    @Test
    void testHandleConflict() {
        IllegalStateException ex = new IllegalStateException("Conflicto de estado");
        ApiError response = exceptionHandler.handleConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT.value(), response.status());
        assertEquals("Conflict", response.error());
        assertEquals("Conflicto de estado", response.message());
    }

    @Test
    void testHandleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate entry");
        ApiError response = exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertEquals(HttpStatus.CONFLICT.value(), response.status());
        assertEquals("Conflict", response.error());
        assertEquals("El recurso solicitado genera un conflicto de datos o duplicidad.", response.message());
    }

    @Test
    void testHandleBadCredentials() {
        BadCredentialsException ex = new BadCredentialsException("Bad creds");
        ApiError response = exceptionHandler.handleBadCredentials(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.status());
        assertEquals("Unauthorized", response.error());
        assertEquals("Credenciales inválidas", response.message());
    }

    @Test
    void testHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Denied");
        ApiError response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.status());
        assertEquals("Forbidden", response.error());
        assertEquals("No tiene permisos para acceder a este recurso", response.message());
    }

    @Test
    void testHandleValidation() {
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "name", "no puede estar vacío");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ApiError response = exceptionHandler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status());
        assertEquals("Bad Request", response.error());
        assertTrue(response.message().contains("name: no puede estar vacío"));
        assertNotNull(response.validationErrors());
        assertEquals("no puede estar vacío", response.validationErrors().get("name"));
    }

    @Test
    void testHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = Mockito.mock(HttpMessageNotReadableException.class);
        ApiError response = exceptionHandler.handleHttpMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status());
        assertEquals("Bad Request", response.error());
        assertEquals("Cuerpo de la petición JSON malformado o no válido.", response.message());
    }

    @Test
    void testHandleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ApiError response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.status());
        assertEquals("Method Not Allowed", response.error());
        assertTrue(response.message().contains("POST no está soportado"));
    }

    @Test
    void testHandleGeneralInternalServerError() {
        RuntimeException ex = new RuntimeException("NullPointerException in internal service");
        ApiError response = exceptionHandler.handleGeneral(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.status());
        assertEquals("Internal Server Error", response.error());
        assertEquals("Ha ocurrido un error interno e inesperado en el servidor.", response.message());
        assertFalse(response.message().contains("NullPointerException"));
    }

    @Test
    void testHandleGeneralEsiosServiceUnavailable() {
        RuntimeException ex = new RuntimeException("Fallo en la comunicación técnica HTTP con ESIOS");
        ApiError response = exceptionHandler.handleGeneral(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.status());
        assertEquals("Service Unavailable", response.error());
        assertEquals("Fallo en la comunicación técnica HTTP con ESIOS", response.message());
    }
}
