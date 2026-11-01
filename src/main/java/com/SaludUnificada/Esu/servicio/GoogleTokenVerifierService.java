package com.SaludUnificada.Esu.servicio;

import com.SaludUnificada.Esu.dto.auth.GoogleIdTokenPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Slf4j
@Service
public class GoogleTokenVerifierService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.client.id:}")
    private String configuredClientId;

    @org.springframework.beans.factory.annotation.Autowired
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
    }

    /**
     * Valida la credencial Google OIDC ID Token consultando el servicio oficial de Google tokeninfo.
     * Verifica firma criptográfica (realizada por Google), emisor, audiencia, expiración y estado del email.
     */
    public GoogleIdTokenPayload verificarToken(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            throw new BadCredentialsException("El token de Google no puede estar vacío.");
        }

        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken.trim();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new BadCredentialsException("Respuesta inválida del proveedor de autenticación Google.");
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            // 1. Validar emisor (iss)
            String iss = root.path("iss").asText(null);
            if (iss == null || (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss))) {
                log.warn("Rechazo de Google Token por emisor no válido");
                throw new BadCredentialsException("El emisor del token de Google no es válido.");
            }

            // 2. Validar audiencia (aud)
            String aud = root.path("aud").asText(null);
            if (configuredClientId != null && !configuredClientId.isBlank()) {
                if (!configuredClientId.trim().equals(aud)) {
                    log.warn("Rechazo de Google Token por audiencia no coincidente");
                    throw new BadCredentialsException("La audiencia del token de Google no coincide con el cliente configurado.");
                }
            }

            // 3. Validar expiración (exp)
            long exp = root.path("exp").asLong(0);
            long nowSeconds = System.currentTimeMillis() / 1000L;
            if (exp > 0 && exp < nowSeconds) {
                log.warn("Rechazo de Google Token expirado");
                throw new BadCredentialsException("El token de Google ha expirado.");
            }

            // 4. Validar estado de verificación del email
            JsonNode emailVerifiedNode = root.path("email_verified");
            boolean emailVerified = emailVerifiedNode.isBoolean() ? emailVerifiedNode.asBoolean() : "true".equalsIgnoreCase(emailVerifiedNode.asText());
            if (!emailVerified) {
                log.warn("Rechazo de Google Token con email no verificado");
                throw new BadCredentialsException("El correo electrónico de Google no está verificado.");
            }

            // 5. Validar sub (identificador único del usuario en Google)
            String sub = root.path("sub").asText(null);
            if (sub == null || sub.trim().isEmpty()) {
                log.warn("Rechazo de Google Token sin sub");
                throw new BadCredentialsException("El identificador único de Google (sub) es requerido.");
            }

            String email = root.path("email").asText(null);
            if (email == null || email.trim().isEmpty()) {
                throw new BadCredentialsException("El email asociado a la cuenta de Google es requerido.");
            }

            return GoogleIdTokenPayload.builder()
                    .sub(sub)
                    .email(email.trim().toLowerCase())
                    .emailVerified(true)
                    .name(root.path("name").asText(null))
                    .givenName(root.path("given_name").asText(null))
                    .familyName(root.path("family_name").asText(null))
                    .aud(aud)
                    .iss(iss)
                    .exp(exp)
                    .build();

        } catch (HttpStatusCodeException e) {
            log.warn("Fallo en validación de token Google con status: {}", e.getStatusCode());
            throw new BadCredentialsException("La credencial de Google no es válida o ha caducado.");
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado validando credencial con proveedor Google: {}", e.getMessage());
            throw new BadCredentialsException("Error al comunicarse con el proveedor de autenticación Google.");
        }
    }
}
