package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.auth.GoogleIdTokenPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Service
public class GoogleTokenVerifierService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private Environment environment;

    @Value("${app.auth.google.enabled:false}")
    private boolean googleAuthEnabled = false;

    @Value("${google.client.id:}")
    private String configuredClientId;

    @Autowired
    public GoogleTokenVerifierService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
    }

    public GoogleTokenVerifierService(RestTemplate restTemplate, ObjectMapper objectMapper, String configuredClientId) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.configuredClientId = configuredClientId;
        this.googleAuthEnabled = (configuredClientId != null && !configuredClientId.isBlank());
    }

    /**
     * Valida la credencial Google OIDC ID Token consultando el servicio oficial de Google tokeninfo.
     * Verifica firma criptográfica (realizada por Google), emisor, audiencia exacta, expiración, email verificado
     * y presencia de todos los claims obligatorios.
     */
    public GoogleIdTokenPayload verificarToken(String idToken) {
        if (!googleAuthEnabled) {
            log.info("Intento de autenticación con Google rechazado: app.auth.google.enabled=false");
            throw new BadCredentialsException("La autenticación mediante Google se encuentra deshabilitada en este entorno.");
        }

        if (idToken == null || idToken.trim().isEmpty()) {
            throw new BadCredentialsException("El token de Google no puede estar vacío.");
        }

        // Simulación controlada para desarrollo local cuando no se dispone de credenciales OAuth reales
        if (idToken.startsWith("dev-google-token:")) {
            boolean isDev = environment == null || Arrays.stream(environment.getActiveProfiles())
                    .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("local"));
            if (isDev) {
                String googleEmail = idToken.substring("dev-google-token:".length()).trim().toLowerCase();
                if (googleEmail.isBlank()) googleEmail = "paciente.google@gmail.com";
                String namePart = googleEmail.contains("@") ? googleEmail.split("@")[0] : "Usuario";
                return GoogleIdTokenPayload.builder()
                        .sub("google-dev-sub-" + Math.abs(googleEmail.hashCode()))
                        .email(googleEmail)
                        .name(namePart.replace(".", " "))
                        .givenName(namePart)
                        .familyName("Google")
                        .emailVerified(true)
                        .build();
            }
        }

        if (configuredClientId == null || configuredClientId.isBlank()) {
            log.error("Google Client ID no se encuentra configurado en el servidor");
            throw new BadCredentialsException("El servidor no tiene configurada la audiencia de Google.");
        }

        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken.trim();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new BadCredentialsException("Respuesta inválida del proveedor de autenticación Google.");
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            // 1. Validar emisor obligatorio (iss)
            String iss = root.path("iss").asText(null);
            if (iss == null || (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss))) {
                log.warn("Rechazo de Google Token por emisor no válido o ausente");
                throw new BadCredentialsException("El emisor del token de Google no es válido.");
            }

            // 2. Validar audiencia obligatoria exacta (aud)
            String aud = root.path("aud").asText(null);
            if (aud == null || aud.isBlank() || !configuredClientId.trim().equals(aud.trim())) {
                log.warn("Rechazo de Google Token por audiencia no coincidente o ausente");
                throw new BadCredentialsException("La audiencia del token de Google no coincide con el cliente configurado.");
            }

            // 3. Validar expiración obligatoria (exp)
            if (!root.hasNonNull("exp")) {
                throw new BadCredentialsException("El token de Google no contiene claim de expiración.");
            }
            long exp = root.path("exp").asLong(0);
            long nowSeconds = System.currentTimeMillis() / 1000L;
            if (exp <= 0 || exp < nowSeconds) {
                log.warn("Rechazo de Google Token expirado");
                throw new BadCredentialsException("El token de Google ha expirado.");
            }

            // 4. Validar estado de verificación obligatoria del email
            if (!root.hasNonNull("email_verified")) {
                throw new BadCredentialsException("El token de Google no especifica el estado de verificación del correo.");
            }
            JsonNode emailVerifiedNode = root.path("email_verified");
            boolean emailVerified = emailVerifiedNode.isBoolean() ? emailVerifiedNode.asBoolean() : "true".equalsIgnoreCase(emailVerifiedNode.asText());
            if (!emailVerified) {
                log.warn("Rechazo de Google Token por email no verificado en Google");
                throw new BadCredentialsException("El correo de la cuenta de Google no está verificado.");
            }

            // 5. Validar claims de identidad básicos
            String sub = root.path("sub").asText(null);
            if (sub == null || sub.isBlank()) {
                throw new BadCredentialsException("El token de Google no contiene el identificador único de usuario (sub).");
            }

            String email = root.path("email").asText(null);
            if (email == null || email.isBlank()) {
                throw new BadCredentialsException("El token de Google no contiene el correo electrónico del usuario.");
            }

            String name = root.path("name").asText(null);
            String givenName = root.path("given_name").asText(null);
            String familyName = root.path("family_name").asText(null);

            return GoogleIdTokenPayload.builder()
                    .iss(iss)
                    .aud(aud)
                    .sub(sub)
                    .email(email)
                    .emailVerified(emailVerified)
                    .exp(exp)
                    .name(name)
                    .givenName(givenName)
                    .familyName(familyName)
                    .build();

        } catch (HttpStatusCodeException ex) {
            log.warn("Fallo HTTP al verificar token contra Google: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BadCredentialsException("La credencial de Google no es válida o ha caducado.");
        } catch (BadCredentialsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Excepción inesperada al procesar respuesta del tokeninfo de Google: {}", ex.getMessage());
            throw new BadCredentialsException("Fallo en la comunicación con el servicio de autenticación de Google.");
        }
    }
}
