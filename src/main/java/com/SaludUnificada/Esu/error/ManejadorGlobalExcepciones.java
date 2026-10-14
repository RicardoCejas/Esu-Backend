package com.SaludUnificada.Esu.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ManejadorGlobalExcepciones extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NoEncontradoExcepcion.class)
    public ResponseEntity<ErrorMessage> notFoundException(NoEncontradoExcepcion exception) {
        // Retorna un objeto ErrorMessage con el código de estado y el mensaje configurado en el servicio
        ErrorMessage message = new ErrorMessage(
                exception.getStatus(),
                LocalDateTime.now(),
                exception.getMessage(),
                exception.getMessage() // Usamos el mismo mensaje para la descripción por simplicidad
        );

        // Devuelve de forma controlada el código HTTP (ej: 404 NOT_FOUND) y el cuerpo JSON
        return ResponseEntity.status(exception.getStatus()).body(message);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        // Colección encargada de estructurar: campoConError -> mensajePersonalizado
        Map<String, Object> errors = new HashMap<>();

        // Recorre los fallos de restricciones en los atributos del DTO (ej: @NotBlank, @NotNull)
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        // Devuelve un código HTTP 400 BAD_REQUEST junto al mapa de campos inválidos
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
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
