# Manual de Despliegue Seguro, Backup y Operaciones (Runbook SSOT)
## Ecosistema de Salud Unificado (ESU) — Cruz del Eje, Córdoba

Este documento constituye la guía oficial y fuente de verdad operativa para el aprovisionamiento, despliegue en producción y recuperación ante desastres del backend del Ecosistema de Salud Unificado (ESU).

---

## 1. Perfil Productivo (`prod`) y Variables de Entorno Obligatorias

El backend cuenta con un validador estricto de arranque (`ProductionSecurityStartupValidator`) que impide el inicio del contenedor si faltan variables críticas o si se detectan contraseñas por defecto o placeholders.

### Variables de Configuración Mandatorias

| Variable de Entorno | Propósito | Requisitos de Seguridad |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Activa el perfil productivo | Debe ser estrictamente `prod` |
| `SPRING_DATASOURCE_URL` | Cadena JDBC de conexión a MySQL | Conexión a red interna (`jdbc:mysql://mysql:3306/db_esu`) |
| `SPRING_DATASOURCE_USERNAME` / `MYSQL_USER` | Usuario de base de datos | **Prohibido `root`**. Usuario con mínimos privilegios (`esu_app`) |
| `SPRING_DATASOURCE_PASSWORD` / `MYSQL_PASSWORD` | Contraseña de base de datos | Mínimo 16 caracteres de alta entropía, sin palabras de diccionario |
| `APP_JWT_SECRET` | Clave HMAC-SHA256 para emisión de tokens JWT | Mínimo 32 caracteres (256 bits), sin placeholders ni valores conocidos |
| `APP_DOCUMENTOS_FIRMA_SECRET` | Clave HMAC-SHA256 para firma digital de evoluciones y recetas | Mínimo 32 caracteres (256 bits) |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos para consumo de API | Dominio HTTPS productivo (ej. `https://salud.cruzdeleje.gob.ar`). **Prohibido `*` o `localhost`** |
| `FRONTEND_URL` | URL canónica de la interfaz web | URL pública HTTPS legítima (ej. `https://salud.cruzdeleje.gob.ar`) |
| `ESU_UPLOADS_ESTUDIOS_DIR` | Directorio de adjuntos médicos | Volumen persistente montado (ej. `/app/uploads/estudios`) |

---

## 2. Endurecimiento de Seguridad Aplicado

1. **Aislamiento de Red Docker:**
   - MySQL se ejecuta en la red interna `esu-internal` sin publicar el puerto `3306` al host.
   - Solo el servicio backend publica el puerto `8080` (o el puerto configurado detrás de un reverse proxy Nginx/Traefik).
2. **Cookies Seguras:**
   - Cookies de sesión configuradas con `server.servlet.session.cookie.secure=true`, `http-only=true` y `same-site=strict`.
3. **Cierre de Herramientas de Desarrollo:**
   - Endpoint `/api/auth/dev-switch-role` denegado (`denyAll()`) y bean desactivado en perfil `prod`.
   - `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false`.
   - Cuentas de prueba y seeder demo bloqueadas (`app.demo.seed-accounts=false`).
4. **Neutralización de Semillas en Migraciones:**
   - La migración `V30__seguridad_produccion_y_cuentas_demo.sql` desactiva automáticamente `dev@esu.gob.ar` y reemplaza cualquier hash conocido de desarrollo por tokens bloqueados de alta entropía.
5. **Políticas de Logging Estructurado (`logback-spring.xml`):**
   - Salida estructurada JSON en consola y archivo rotativo.
   - Rotación diaria y por tamaño de 10 MB, retención máxima de 30 días y tope de almacenamiento de 1 GB.
   - No se registran contraseñas, tokens JWT completos ni secretos en texto plano.

---

## 3. Procedimiento de Despliegue con Docker Compose

### Paso 1: Verificación de sintaxis y variables obligatorias
```bash
# Validar que compose detecte las variables y no permita valores vacíos:
docker compose config
```
Si falta alguna variable como `MYSQL_PASSWORD` o `APP_JWT_SECRET`, el comando fallará de forma inmediata (`required variable is missing a value`).

### Paso 2: Lanzamiento del stack
```bash
docker compose up -d --build
```

### Paso 3: Verificación del estado de salud (Healthchecks)
```bash
docker compose ps
```
El contenedor `esu-mysql` y `esu-backend` deben reportar `(healthy)`.

---

## 4. Manual de Operaciones: Backup y Restauración de Base de Datos

### 4.1 Generación de Copia de Respaldo (Backup)
Para generar un backup consistente sin bloquear lecturas operativas:
```bash
# Backup en caliente mediante contenedor MySQL:
docker exec esu-mysql mysqldump -u esu_app -p"${MYSQL_PASSWORD}" \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --default-character-set=utf8mb4 \
  db_esu > "backup_db_esu_$(date +%Y%m%d_%H%M%S).sql"
```

### 4.2 Restauración de Base de Datos desde Respaldo
```bash
# 1. Poner la aplicación en mantenimiento (detener backend temporalmente):
docker compose stop backend

# 2. Restaurar el archivo SQL en la base de datos:
docker exec -i esu-mysql mysql -u esu_app -p"${MYSQL_PASSWORD}" db_esu < backup_db_esu_20261108.sql

# 3. Reiniciar el servicio backend:
docker compose start backend
```

---

## 5. Procedimiento de Rollback de Migraciones Flyway

Flyway aplica migraciones de esquema versionadas de manera secuencial (`V1`, `V2`, ..., `V30`).

### 5.1 En caso de falla durante una migración
Si una migración falla a mitad de ejecución, Flyway registrará el estado como `FAILED` en la tabla `flyway_schema_history`.

1. **Revisar el error en los logs del backend:**
   ```bash
   docker logs esu-backend --tail 100
   ```
2. **Corregir la causa subyacente en el esquema de la base de datos.**
3. **Reparar los metadatos de Flyway:**
   ```bash
   # Ejecutar Flyway repair mediante Maven o CLI de Flyway:
   ./mvnw flyway:repair
   ```
4. **Estrategia de Rollback recomendada:**
   En arquitecturas de producción con alta disponibilidad, la reversión de base de datos se realiza mediante **Forward-fix Migration** (crear una nueva migración `V31__revertir_cambio_anterior.sql` que aplique las instrucciones inversas DDL/DML de forma idempotente).
5. **Rollback completo en caso de contingencia crítica:**
   Restaurar el snapshot de base de datos previo al despliegue siguiendo el paso 4.2.
