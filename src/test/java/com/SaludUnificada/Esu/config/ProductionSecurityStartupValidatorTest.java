package com.SaludUnificada.Esu.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionSecurityStartupValidatorTest {

    @Mock
    private Environment environment;

    @Test
    void testValidateConfiguration_PerfilDesarrollo_NoEjecutaValidacionesEstrictas() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);

        assertDoesNotThrow(validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ConflictoProdYDev_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod", "dev"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConJwtInseguro_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "123456");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConJwtPlaceholder_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "CAMBIAR_EN_PRODUCCION_CLAVE_JWT_MINIMO_32_CARACTERES_HEX_O_BASE64");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConUsuarioRoot_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "un-token-criptografico-de-alta-entropia-random-12345aB!");
        ReflectionTestUtils.setField(validator, "datasourceUsername", "root");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConDbPasswordPlaceholder_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "un-token-criptografico-de-alta-entropia-random-12345aB!");
        ReflectionTestUtils.setField(validator, "datasourceUsername", "esu_app");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "CAMBIAR_EN_PRODUCCION_PASSWORD_MYSQL_ESU_APP");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConCorsLocalhost_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "un-token-criptografico-de-alta-entropia-random-12345aB!");
        ReflectionTestUtils.setField(validator, "datasourceUsername", "esu_app");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "StrongSecretDB_889922!");
        ReflectionTestUtils.setField(validator, "allowedOrigins", "http://localhost:5173");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConFrontendLocalhost_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "un-token-criptografico-de-alta-entropia-random-12345aB!");
        ReflectionTestUtils.setField(validator, "datasourceUsername", "esu_app");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "StrongSecretDB_889922!");
        ReflectionTestUtils.setField(validator, "allowedOrigins", "https://salud.cruzdeleje.gob.ar");
        ReflectionTestUtils.setField(validator, "frontendUrl", "http://localhost:5173");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConSmtpPlaceholder_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "un-token-criptografico-de-alta-entropia-random-12345aB!");
        ReflectionTestUtils.setField(validator, "datasourceUsername", "esu_app");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "StrongSecretDB_889922!");
        ReflectionTestUtils.setField(validator, "allowedOrigins", "https://salud.cruzdeleje.gob.ar");
        ReflectionTestUtils.setField(validator, "frontendUrl", "https://salud.cruzdeleje.gob.ar");
        ReflectionTestUtils.setField(validator, "mailHost", "smtp.ejemplo.gob.ar");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdValido_PasaSatisfactoriamente() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        ReflectionTestUtils.setField(validator, "jwtSecret", "un-token-criptografico-de-alta-entropia-random-12345aB!");
        ReflectionTestUtils.setField(validator, "datasourceUsername", "esu_app");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "StrongSecretDB_889922!");
        ReflectionTestUtils.setField(validator, "allowedOrigins", "https://salud.cruzdeleje.gob.ar");
        ReflectionTestUtils.setField(validator, "frontendUrl", "https://salud.cruzdeleje.gob.ar");
        ReflectionTestUtils.setField(validator, "mailHost", "smtp.sendgrid.net");
        ReflectionTestUtils.setField(validator, "mailPassword", "SG.ProductionApiKeySecret_99911");

        assertDoesNotThrow(validator::validateConfiguration);
    }
}