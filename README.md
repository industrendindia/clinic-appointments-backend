# Clinic Appointments API

Spring Boot and PostgreSQL backend for the CareNest multi-clinic appointment platform.

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 16+

## Configuration

Supply these environment variables in production:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`

All database objects are created in the `clinic_app` schema by Flyway.

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/clinic-api-1.0.0.jar
```

