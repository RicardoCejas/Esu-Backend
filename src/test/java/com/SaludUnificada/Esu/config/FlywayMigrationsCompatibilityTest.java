package com.SaludUnificada.Esu.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationsCompatibilityTest {

    private static final Pattern CTAS_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:TEMPORARY\\s+)?TABLE\\b[^;]*?\\bAS\\s+SELECT\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "\\bCREATE\\s+(?:TEMPORARY\\s+)?TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([`\\w.]+)\\s*\\((.*?\\))\\s*(?:ENGINE|COMMENT|DEFAULT|;|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern PACIENTE_INVALID_FK_PATTERN = Pattern.compile(
            "\\bpaciente\\b[^;]*\\b(?:paciente|pac)\\.usuario_id\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Test
    void allFlywayMigrations_mustRespectSqlRequirePrimaryKey() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/*.sql");
        List<String> violations = new ArrayList<>();

        for (Resource resource : resources) {
            String rawSql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String sqlWithoutComments = stripSqlComments(rawSql);

            if (CTAS_PATTERN.matcher(sqlWithoutComments).find()) {
                violations.add(resource.getFilename() + ": contiene CREATE [TEMPORARY] TABLE ... AS SELECT sin clave primaria");
            }

            Matcher matcher = CREATE_TABLE_PATTERN.matcher(sqlWithoutComments);
            while (matcher.find()) {
                String tableName = matcher.group(1);
                String body = matcher.group(2).toUpperCase(Locale.ROOT);
                if (!body.contains("PRIMARY KEY")) {
                    violations.add(resource.getFilename() + ": tabla " + tableName + " declarada sin PRIMARY KEY");
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "Violaciones de sql_require_primary_key detectadas:\n" + String.join("\n", violations));
    }

    @Test
    void pacienteForeignKey_mustReferenceFkUsuarioAndNotUsuarioId() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:db/migration/*.sql");
        List<String> violations = new ArrayList<>();

        for (Resource resource : resources) {
            String rawSql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String sqlWithoutComments = stripSqlComments(rawSql);
            if (PACIENTE_INVALID_FK_PATTERN.matcher(sqlWithoutComments).find()) {
                violations.add(resource.getFilename() + ": referencia inválida a usuario_id en paciente (debe usar fk_usuario)");
            }
        }

        assertTrue(violations.isEmpty(), () -> "Referencias inválidas a paciente.usuario_id:\n" + String.join("\n", violations));
    }

    @Test
    void pendingColumnMigrationsV35ToV37_mustBeIdempotentAndSafeUpdateCompatible() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String migration : List.of(
                "classpath:db/migration/V35__usuario_perfil_operativo.sql",
                "classpath:db/migration/V36__turno_planta_consultorio.sql",
                "classpath:db/migration/V37__disponibilidad_consultorio.sql"
        )) {
            Resource resource = resolver.getResource(migration);
            assertTrue(resource.exists(), "Debe existir " + migration);
            String sql = stripSqlComments(new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            String lower = sql.toLowerCase(Locale.ROOT);

            assertTrue(lower.contains("information_schema.columns"),
                    resource.getFilename() + " debe verificar information_schema.columns antes de ADD COLUMN");
            assertTrue(lower.contains("prepare ") && lower.contains("execute ") && lower.contains("deallocate prepare "),
                    resource.getFilename() + " debe usar PREPARE/EXECUTE/DEALLOCATE PREPARE para DDL idempotente en MySQL");
        }

        Resource v35 = resolver.getResource("classpath:db/migration/V35__usuario_perfil_operativo.sql");
        String v35Sql = stripSqlComments(new String(v35.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertTrue(v35Sql.contains("pac.fk_usuario"), "V35 debe unir paciente por fk_usuario");
        assertFalse(v35Sql.contains("pac.usuario_id"), "V35 no debe referenciar pac.usuario_id");
        assertTrue(v35Sql.contains("u.id > 0"), "V35 debe incluir u.id > 0 para compatibilidad con sql_safe_updates=ON");
    }

    private String stripSqlComments(String sql) {
        String noBlockComments = sql.replaceAll("/\\*.*?\\*/", " ");
        return noBlockComments.replaceAll("(?m)--.*$", "");
    }
}
