package com.SaludUnificada.Esu.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * HU-88: Manejador Global de Excepciones Estandarizado con RFC 7807 (Problem Details for HTTP APIs).
 * Genera respuestas de error uniformes, semánticas y legibles tanto para máquinas como humanos.
 */
@ControllerAdvice
public class ManejadorGlobalExcepciones extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);
    private static final String BASE_PROBLEM_TYPE = "https://esu.cruzdeleje.gob.ar/errores/";

    @ExceptionHandler(NoEncontradoExcepcion.class)
    public ResponseEntity<ProblemDetail> handleNoEncontrado(NoEncontradoExcepcion ex, WebRequest request) {
        log.warn("Recurso clínico no encontrado: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setType(URI.create(BASE_PROBLEM_TYPE + "recurso-no-encontrado"));
        problem.setTitle("Recurso No Encontrado");
        problem.setProperty("timestamp", Instant.now());

        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }

        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("Fallo de validación en los datos de entrada: {} errores", ex.getBindingResult().getErrorCount());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Uno o más campos enviados no cumplen con las restricciones de validación clínicas."
        );
        problem.setType(URI.create(BASE_PROBLEM_TYPE + "validacion-fallida"));
        problem.setTitle("Error de Validación de Datos");
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> invalidParams = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                invalidParams.put(error.getField(), error.getDefaultMessage())
        );
        problem.setProperty("invalidParams", invalidParams);

        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Excepción no controlada en el servidor ESU: ", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado en los servidores asistenciales. Por favor intente más tarde."
        );
        problem.setType(URI.create(BASE_PROBLEM_TYPE + "error-interno"));
        problem.setTitle("Error Interno del Servidor");
        problem.setProperty("timestamp", Instant.now());

        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessage> handleIllegalArgumentException(IllegalArgumentException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.BAD_REQUEST,
                LocalDateTime.now(),
                exception.getMessage(),
                "Solicitud inválida"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorMessage> handleIllegalStateException(IllegalStateException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.CONFLICT,
                LocalDateTime.now(),
                exception.getMessage(),
                "Conflicto de estado"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErrorMessage> handleBadCredentials(org.springframework.security.authentication.BadCredentialsException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.UNAUTHORIZED,
                LocalDateTime.now(),
                "Credenciales inválidas: email o contraseña incorrectos",
                "Fallo de autenticación"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAccessDenied(org.springframework.security.access.AccessDeniedException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.FORBIDDEN,
                LocalDateTime.now(),
                "Acceso denegado: no posee los permisos o rol requerido para este recurso",
                "Permisos insuficientes"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleGlobalException(Exception exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                LocalDateTime.now(),
                exception.getMessage(),
                "Error interno del servidor"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
    }
}
