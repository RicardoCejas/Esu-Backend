package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.auth.GoogleIdTokenPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String configuredClientId = "test-client-id.apps.googleusercontent.com";

    private GoogleTokenVerifierService verifierService;

    @BeforeEach
    void setUp() {
        verifierService = new GoogleTokenVerifierService(restTemplate, objectMapper, configuredClientId);
    }

    @Test
    @DisplayName("Debe validar exitosamente un Google ID Token válido y verificado")
    void tokenValido_retornaPayloadCorrecto() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600;
        String jsonGoogle = "{"
                + "\"iss\": \"accounts.google.com\","
                + "\"aud\": \"" + configuredClientId + "\","
                + "\"sub\": \"109876543210987654321\","
                + "\"email\": \"juan.perez@gmail.com\","
                + "\"email_verified\": true,"
                + "\"name\": \"Juan Perez\","
                + "\"given_name\": \"Juan\","
                + "\"family_name\": \"Perez\","
                + "\"exp\": " + futureExp
                + "}";

        when(restTemplate.getForEntity(contains("tokeninfo?id_token="), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonGoogle, HttpStatus.OK));

        GoogleIdTokenPayload payload = verifierService.verificarToken("valid-google-id-token");

        assertNotNull(payload);
        assertEquals("109876543210987654321", payload.getSub());
        assertEquals("juan.perez@gmail.com", payload.getEmail());
        assertTrue(payload.getEmailVerified());
        assertEquals("Juan", payload.getGivenName());
        assertEquals("Perez", payload.getFamilyName());
    }

    @Test
    @DisplayName("Debe rechazar token cuando el email no está verificado por Google")
    void emailNoVerificado_lanzaBadCredentials() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600;
        String jsonGoogle = "{"
                + "\"iss\": \"accounts.google.com\","
                + "\"aud\": \"" + configuredClientId + "\","
                + "\"sub\": \"109876543210987654321\","
                + "\"email\": \"unverified@gmail.com\","
                + "\"email_verified\": false,"
                + "\"exp\": " + futureExp
                + "}";

        when(restTemplate.getForEntity(contains("tokeninfo?id_token="), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonGoogle, HttpStatus.OK));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> verifierService.verificarToken("unverified-token"));

        assertTrue(ex.getMessage().contains("no está verificado"));
    }

    @Test
    @DisplayName("Debe rechazar token cuando la audiencia no coincide con el GOOGLE_CLIENT_ID")
    void audienciaIncorrecta_lanzaBadCredentials() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600;
        String jsonGoogle = "{"
                + "\"iss\": \"accounts.google.com\","
                + "\"aud\": \"otro-cliente-malicioso.apps.googleusercontent.com\","
                + "\"sub\": \"109876543210987654321\","
                + "\"email\": \"test@gmail.com\","
                + "\"email_verified\": true,"
                + "\"exp\": " + futureExp
                + "}";

        when(restTemplate.getForEntity(contains("tokeninfo?id_token="), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonGoogle, HttpStatus.OK));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> verifierService.verificarToken("wrong-aud-token"));

        assertTrue(ex.getMessage().contains("audiencia"));
    }

    @Test
    @DisplayName("Debe rechazar token expirado")
    void tokenExpirado_lanzaBadCredentials() {
        long pastExp = (System.currentTimeMillis() / 1000L) - 300;
        String jsonGoogle = "{"
                + "\"iss\": \"accounts.google.com\","
                + "\"aud\": \"" + configuredClientId + "\","
                + "\"sub\": \"109876543210987654321\","
                + "\"email\": \"test@gmail.com\","
                + "\"email_verified\": true,"
                + "\"exp\": " + pastExp
                + "}";

        when(restTemplate.getForEntity(contains("tokeninfo?id_token="), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonGoogle, HttpStatus.OK));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> verifierService.verificarToken("expired-token"));

        assertTrue(ex.getMessage().contains("ha expirado"));
    }

    @Test
    @DisplayName("Debe rechazar token cuando el emisor no es accounts.google.com")
    void emisorInvalido_lanzaBadCredentials() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600;
        String jsonGoogle = "{"
                + "\"iss\": \"https://rogue-issuer.com\","
                + "\"aud\": \"" + configuredClientId + "\","
                + "\"sub\": \"109876543210987654321\","
                + "\"email\": \"test@gmail.com\","
                + "\"email_verified\": true,"
                + "\"exp\": " + futureExp
                + "}";

        when(restTemplate.getForEntity(contains("tokeninfo?id_token="), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonGoogle, HttpStatus.OK));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> verifierService.verificarToken("invalid-iss-token"));

        assertTrue(ex.getMessage().contains("emisor"));
    }

    @Test
    @DisplayName("Debe manejar fallos de respuesta HTTP o error 400 devuelto por Google")
    void falloGoogle_lanzaBadCredentials() {
        when(restTemplate.getForEntity(contains("tokeninfo?id_token="), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid token"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> verifierService.verificarToken("bad-token"));

        assertTrue(ex.getMessage().contains("no es válida"));
    }
}
