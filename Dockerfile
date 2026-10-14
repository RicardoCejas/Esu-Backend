# =============================================================================
# ESU: Ecosistema de Salud Unificado (Cruz del Eje)
# HU-86: Dockerfile Multietapa (Multi-Stage Build)
# =============================================================================

# -----------------------------------------------------------------------------
# Etapa 1: Compilación y empaquetado (Builder)
# -----------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Optimización de caché de dependencias Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilación del código fuente excluyendo pruebas en el build de contenedor
COPY src ./src
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Etapa 2: Imagen de ejecución ligera y segura (Runner)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runner
WORKDIR /app

# Crear usuario de sistema no-root para seguridad clínica
RUN groupadd -r esugroup && useradd -r -g esugroup esuuser

# Copiar el artefacto ejecutable generado en la etapa 1
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R esuuser:esugroup /app

# Ejecución como usuario sin privilegios
USER esuuser

# Variables de entorno JVM recomendadas para contenedores
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080

EXPOSE 8080

# Verificación de salud del contenedor
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
