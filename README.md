# 3D Cost Manager

Aplicación orientada a la **gestión y estimación de costes de impresión 3D y post-procesado**.

El proyecto tiene como objetivo facilitar la gestión de los recursos utilizados durante la fabricación y ayudar a generar presupuestos a partir de los diferentes costes asociados a cada proyecto.

## 🎯 Objetivos

* Gestionar máquinas y equipos de impresión 3D.
* Gestionar materiales y consumibles.
* Gestionar herramientas y recursos de taller.
* Crear y administrar proyectos de fabricación.
* Calcular automáticamente los costes asociados a cada proyecto.
* Tener en cuenta materiales, electricidad, mantenimiento y mano de obra.
* Facilitar la creación y comparación de presupuestos.
* Mantener un historial de proyectos y costes.
* Automatizar la actualización de determinados costes.
* Crear una base sólida y escalable para futuras funcionalidades.

## 🛠️ Tecnologías

* Java 21
* Spring Boot 3
* PostgreSQL 16
* Docker & Docker Compose
* Flyway (Migrations)
* Spring Security & JWT
* ESIOS API Integration

## 🐳 Ejecución con Docker

El proyecto está preparado para ejecutarse mediante Docker Compose, lo que permite levantar el stack completo (Backend + Base de Datos) de forma reproducible.

### Requisitos

* Docker Desktop o Docker Engine.

### Pasos para el arranque

1. **Configurar variables de entorno:**
   Copia el archivo de ejemplo y configura tus credenciales reales en el nuevo archivo `.env`:
   ```bash
   cp .env.example .env
   ```

2. **Construir y levantar el stack:**
   ```bash
   docker compose up --build -d
   ```

3. **Verificar el estado:**
   Espera a que los servicios informen estado `healthy`. Puedes monitorizar los logs:
   ```bash
   docker compose logs -f backend
   ```

4. **Acceso a la API:**
   El backend estará disponible en `http://localhost:8080`.
   - Health check: `GET /api/v1/health`

### Detención y Limpieza

Para detener los contenedores manteniendo los datos:
```bash
docker compose stop
```

Para eliminar los contenedores (los datos persisten en el volumen `postgres_data`):
```bash
docker compose down
```

## 🧪 Testing

Para ejecutar la suite completa de pruebas (Unit, Integration con Testcontainers, REST y Security):
```bash
cd backend
./mvnw clean verify
```

El reporte de cobertura de **JaCoCo** se generará en `target/site/jacoco/index.html`.

## 📌 Estado

> 🚧 Proyecto en desarrollo
