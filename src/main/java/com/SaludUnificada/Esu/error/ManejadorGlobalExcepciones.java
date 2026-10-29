package com.SaludUnificada.Esu.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ManejadorGlobalExcepciones extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);
    private static final String BASE_PROBLEM_TYPE = "https://esu.cruzdeleje.gob.ar/errores/";

    private String sanitizarMensaje(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return "Operación rechazada por validación.";
        }
        String s = mensaje.replaceAll("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}", "***@***.***");
        s = s.replaceAll("[C-Z]:\\\\[^\\s:]+", "[RUTA_RESTRINGIDA]");
        s = s.replaceAll("/[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+/[^\\s:]+", "[RUTA_RESTRINGIDA]");
        s = s.replaceAll("(?i)(token|bearer|secret|password|jwt)=?[a-z0-9._~+/-]{16,}", "$1=[REDACTED]");
        return s;
    }

    @ExceptionHandler(NoEncontradoExcepcion.class)
    public ResponseEntity<ProblemDetail> handleNoEncontrado(NoEncontradoExcepcion ex, WebRequest request) {
        log.warn("Recurso clinico no encontrado: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), sanitizarMensaje(ex.getMessage()));
        problem.setType(URI.create(BASE_PROBLEM_TYPE + "recurso-no-encontrado"));
        problem.setTitle("Recurso No Encontrado");
        problem.setProperty("timestamp", Instant.now());

        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }

        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ProblemDetail> handleReglaNegocio(ReglaNegocioException ex, WebRequest request) {
        log.warn("Violacion de regla de negocio clinica: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), sanitizarMensaje(ex.getMessage()));
        problem.setType(URI.create(BASE_PROBLEM_TYPE + "regla-negocio-violada"));
        problem.setTitle("Regla de Negocio No Cumplida");
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

        log.warn("Fallo de validacion en datos de entrada: {} errores", ex.getBindingResult().getErrorCount());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Uno o mas campos enviados no cumplen con las restricciones de validacion clinicas."
        );
        problem.setType(URI.create(BASE_PROBLEM_TYPE + "validacion-fallida"));
        problem.setTitle("Error de Validacion de Datos");
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

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.BAD_REQUEST,
                LocalDateTime.now(),
                "Parametro requerido ausente: " + ex.getParameterName(),
                "Parametro faltante"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.PAYLOAD_TOO_LARGE,
                LocalDateTime.now(),
                "El archivo adjunto supera el limite maximo permitido de 15MB.",
                "Archivo demasiado grande"
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorMessage> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.error("Violacion de integridad de datos en base de datos: {}", exception.getMessage());
        ErrorMessage message = new ErrorMessage(
                HttpStatus.CONFLICT,
                LocalDateTime.now(),
                "Conflicto de integridad de datos: el registro ya existe o viola una restriccion relacional.",
                "Conflicto de integridad"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessage> handleIllegalArgumentException(IllegalArgumentException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.BAD_REQUEST,
                LocalDateTime.now(),
                sanitizarMensaje(exception.getMessage()),
                "Solicitud invalida"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorMessage> handleIllegalStateException(IllegalStateException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.CONFLICT,
                LocalDateTime.now(),
                sanitizarMensaje(exception.getMessage()),
                "Conflicto de estado"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessage> handleBadCredentials(BadCredentialsException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.UNAUTHORIZED,
                LocalDateTime.now(),
                "Credenciales invalidas: email o contrasena incorrectos",
                "Fallo de autenticacion"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAccessDenied(AccessDeniedException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.FORBIDDEN,
                LocalDateTime.now(),
                "Acceso denegado: no posee los permisos o rol requerido para este recurso",
                "Permisos insuficientes"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorMessage> handleSecurityException(SecurityException exception) {
        log.warn("Violacion de seguridad detectada: {}", exception.getMessage());
        ErrorMessage message = new ErrorMessage(
                HttpStatus.FORBIDDEN,
                LocalDateTime.now(),
                "Acceso denegado: la operacion fue bloqueada por politicas de seguridad del sistema.",
                "Acceso restringido"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
    }

    @ExceptionHandler(RecetaYaDispensadaException.class)
    public ResponseEntity<ErrorMessage> handleRecetaYaDispensada(RecetaYaDispensadaException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.CONFLICT,
                LocalDateTime.now(),
                exception.getMessage(),
                "Receta ya dispensada previamente"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
    }

    @ExceptionHandler(RecetaVencidaException.class)
    public ResponseEntity<ErrorMessage> handleRecetaVencida(RecetaVencidaException exception) {
        ErrorMessage message = new ErrorMessage(
                HttpStatus.GONE,
                LocalDateTime.now(),
                exception.getMessage(),
                "Receta digital vencida"
        );
        return ResponseEntity.status(HttpStatus.GONE).body(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Excepcion no controlada en el servidor ESU: ", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado en los servidores asistenciales. Por favor intente mas tarde."
        );
        problem.setType(URI.create(BASE_PROBLEM_TYPE + "error-interno"));
        problem.setTitle("Error Interno del Servidor");
        problem.setProperty("timestamp", Instant.now());

        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
