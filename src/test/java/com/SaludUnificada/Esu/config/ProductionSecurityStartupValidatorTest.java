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

    private void setValidProductionDefaults(ProductionSecurityStartupValidator validator) {
        ReflectionTestUtils.setField(validator, "flywayEnabled", true);
        ReflectionTestUtils.setField(validator, "hibernateDdlAuto", "validate");
        ReflectionTestUtils.setField(validator, "jwtSecret", "un-token-criptografico-de-alta-entropia-random-12345aB!");
        ReflectionTestUtils.setField(validator, "documentosFirmaSecret", "secreto-firma-documental-de-alta-entropia-256bits!");
        ReflectionTestUtils.setField(validator, "datasourceUsername", "esu_app");
        ReflectionTestUtils.setField(validator, "datasourcePassword", "StrongSecretDB_889922!");
        ReflectionTestUtils.setField(validator, "allowedOrigins", "https://salud.cruzdeleje.gob.ar");
        ReflectionTestUtils.setField(validator, "frontendUrl", "https://salud.cruzdeleje.gob.ar");
        ReflectionTestUtils.setField(validator, "mailEnabled", false);
        ReflectionTestUtils.setField(validator, "mailHost", "");
        ReflectionTestUtils.setField(validator, "googleAuthEnabled", false);
        ReflectionTestUtils.setField(validator, "seedDemoAccounts", false);
        ReflectionTestUtils.setField(validator, "configuredDevSecret", "");
        ReflectionTestUtils.setField(validator, "cookieSecure", true);
        ReflectionTestUtils.setField(validator, "swaggerUiEnabled", false);
        ReflectionTestUtils.setField(validator, "apiDocsEnabled", false);
    }

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
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "jwtSecret", "123456");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConJwtPlaceholder_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "jwtSecret", "CAMBIAR_EN_PRODUCCION_CLAVE_JWT_MINIMO_32_CARACTERES_HEX_O_BASE64");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConUsuarioRoot_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "datasourceUsername", "root");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConDbPasswordPlaceholder_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "datasourcePassword", "CAMBIAR_EN_PRODUCCION_PASSWORD_MYSQL_ESU_APP");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConCorsLocalhost_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "allowedOrigins", "http://localhost:5173");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConFrontendLocalhost_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "frontendUrl", "http://localhost:5173");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConSmtpInseguro_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", true);
        ReflectionTestUtils.setField(validator, "mailHost", "localhost");
        ReflectionTestUtils.setField(validator, "mailPassword", "123456");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConConfiguracionCompletaValida_Pasa() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", true);
        ReflectionTestUtils.setField(validator, "mailHost", "smtp.sendgrid.net");
        ReflectionTestUtils.setField(validator, "mailPassword", "SG.ProductionApiKeySecret_99911");
        ReflectionTestUtils.setField(validator, "googleAuthEnabled", true);
        ReflectionTestUtils.setField(validator, "googleClientId", "123456789-prod.apps.googleusercontent.com");

        assertDoesNotThrow(validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConGoogleInseguro_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "googleAuthEnabled", true);
        ReflectionTestUtils.setField(validator, "googleClientId", "");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConFlywayDeshabilitado_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "flywayEnabled", false);

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConHibernateDdlUpdate_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "hibernateDdlAuto", "update");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConMailDeshabilitado_NoExigeSmtp() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "mailEnabled", false);
        ReflectionTestUtils.setField(validator, "mailHost", "");

        assertDoesNotThrow(validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConFirmaSecretPlaceholder_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "documentosFirmaSecret", "CAMBIAR_EN_PRODUCCION_FIRMA_SECRET_MINIMO_32");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConSeedDemoAccountsTrue_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "seedDemoAccounts", true);

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConDevSecretDefault_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "configuredDevSecret", "dev-secret-esu-2026");

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConCookieInsegura_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "cookieSecure", false);

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }

    @Test
    void testValidateConfiguration_ProdConSwaggerHabilitado_Falla() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        ProductionSecurityStartupValidator validator = new ProductionSecurityStartupValidator(environment);
        setValidProductionDefaults(validator);
        ReflectionTestUtils.setField(validator, "swaggerUiEnabled", true);

        assertThrows(IllegalStateException.class, validator::validateConfiguration);
    }
}
