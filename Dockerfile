# Multi-stage build for Spring Boot
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as non-privileged system user
RUN groupadd -r esugroup && useradd -r -g esugroup -d /app -s /sbin/nologin esuuser \
    && mkdir -p /app/uploads/estudios \
    && chown -R esuuser:esugroup /app

COPY --from=build --chown=esuuser:esugroup /app/target/Esu-0.0.1-SNAPSHOT.jar app.jar

USER esuuser
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
