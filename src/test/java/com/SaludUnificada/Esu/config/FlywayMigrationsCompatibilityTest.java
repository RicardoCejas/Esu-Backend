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

    private String stripSqlComments(String sql) {
        String noBlockComments = sql.replaceAll("/\\*.*?\\*/", " ");
        return noBlockComments.replaceAll("(?m)--.*$", "");
    }
}
