package com.SaludUnificada.Esu.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V39DemoAccountsMigrationTest {

    private static final String V38_RESOURCE =
            "classpath:db/migration/V38__ampliar_especialidades_hospital_crespo_y_turnos_diciembre.sql";
    private static final String V39_RESOURCE =
            "classpath:db/migration/V39__desactivar_cuentas_profesionales_de_prueba.sql";
    private static final Pattern V38_USER_INSERT = Pattern.compile(
            "INSERT\\s+INTO\\s+usuario\\s*\\(email,\\s*password,\\s*rol,\\s*estado_activo,\\s*fecha_alta,\\s*email_verificado\\)\\s*VALUES(.*?)ON\\s+DUPLICATE\\s+KEY\\s+UPDATE",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern EMAIL_VALUE = Pattern.compile("\\(\\s*'([^']+@[^']+)'\\s*,");
    private static final String UNRELATED_EMAIL = "unrelated-account@example.test";

    @Test
    void v39MustDeactivateEveryV38SeedAccountAndPreserveUnrelatedAccounts() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<String> seedEmails = readSeedEmails(resolver.getResource(V38_RESOURCE));
        assertTrue(!seedEmails.isEmpty(), "V38 must provide the demo account set for this migration check");

        Resource migration = resolver.getResource(V39_RESOURCE);
        assertTrue(migration.exists(), "V39 must exist to disable V38's shared-credential accounts");

        String databaseName = "v39_demo_accounts_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + databaseName)) {
            createAccountsTable(connection);
            for (String email : seedEmails) {
                insertActiveAccount(connection, email);
            }
            insertActiveAccount(connection, UNRELATED_EMAIL);

            ScriptUtils.executeSqlScript(connection, new EncodedResource(migration));

            assertSeedAccountsDisabled(connection, seedEmails);
            assertAccountActive(connection, UNRELATED_EMAIL);
        }
    }

    private List<String> readSeedEmails(Resource v38) throws IOException {
        String sql;
        try (InputStream input = v38.getInputStream()) {
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher insertMatcher = V38_USER_INSERT.matcher(sql);
        assertTrue(insertMatcher.find(), "V38 must contain the expected usuario insert section");

        List<String> emails = new ArrayList<>();
        Matcher emailMatcher = EMAIL_VALUE.matcher(insertMatcher.group(1));
        while (emailMatcher.find()) {
            emails.add(emailMatcher.group(1));
        }
        return emails;
    }

    private void createAccountsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE usuario ("
                    + "email VARCHAR(255) PRIMARY KEY, "
                    + "password VARCHAR(255) NOT NULL, "
                    + "rol VARCHAR(32) NOT NULL, "
                    + "estado_activo INTEGER NOT NULL, "
                    + "email_verificado INTEGER NOT NULL"
                    + ")");
        }
    }

    private void insertActiveAccount(Connection connection, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO usuario (email, password, rol, estado_activo, email_verificado) VALUES (?, ?, ?, 1, 1)")) {
            statement.setString(1, email);
            statement.setString(2, "test-hash-not-used-for-authentication");
            statement.setString(3, "PROFESIONAL");
            statement.executeUpdate();
        }
    }

    private void assertSeedAccountsDisabled(Connection connection, List<String> seedEmails) throws SQLException {
        for (String email : seedEmails) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT estado_activo, email_verificado FROM usuario WHERE email = ?")) {
                statement.setString(1, email);
                try (ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next(), "Every V38 demo account must remain available for migration history");
                    assertEquals(0, result.getInt("estado_activo"), "Every V38 demo account must be inactive");
                    assertEquals(0, result.getInt("email_verificado"), "Every V38 demo account must be unverified");
                }
            }
        }
    }

    private void assertAccountActive(Connection connection, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT estado_activo FROM usuario WHERE email = ?")) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next(), "The unrelated account must remain in the database");
                assertEquals(1, result.getInt("estado_activo"), "The migration must not disable unrelated accounts");
            }
        }
    }
}
