# Springfield Archive — Operaciones (backend/)

## Método recomendado: iniciador de Omarchy

Desde la raíz del proyecto:

```bash
./INICIAR_SPRINGFIELD.run iniciar
# En otra terminal, tras arrancar Java:
./INICIAR_SPRINGFIELD.run sincronizar
./INICIAR_SPRINGFIELD.run estado
./INICIAR_SPRINGFIELD.run probar --integracion
```

Usa perfil `local` de forma **explícita**. El perfil por defecto `prod` es
restrictivo para evitar publicar Swagger y configuraciones locales por accidente.
No elimina volúmenes ni reemplaza archivos .env existentes.

## Levantar PostgreSQL 18.7 local

```bash
cp .env.example .env     # rellena valores (son ficticios de ejemplo)
docker compose up -d
docker compose ps        # espera a "healthy"
```

## Arrancar el backend (desde el directorio `backend/`)

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/<POSTGRES_DB>
export SPRING_DATASOURCE_USERNAME=<POSTGRES_USER>
export SPRING_DATASOURCE_PASSWORD=<POSTGRES_PASSWORD>
export SPRINGFIELD_ADMIN_TOKEN=<token-local>   # necesario sólo para la importación
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run    # ejecutar dentro de backend/
```

Spring Boot **no carga `.env`**: las credenciales se inyectan como variables de entorno del proceso.
El frontend se sirve same-origin desde `../frontend` relativo a `backend/` (configuración en `application-local.yml`).

## Importación del catálogo externo (idempotente y reanudable)

```bash
# Disparar la importación en segundo plano
curl -X POST -H "X-Admin-Token: $SPRINGFIELD_ADMIN_TOKEN" \
     "http://localhost:8080/api/v1/sync/start?resource=all"

# Seguir progreso
curl -s "http://localhost:8080/api/v1/sync/status" | jq .
```

Reanuda automáticamente desde la última página completada; repite con seguridad sin duplicar registros.

## Persistencia y ciclo de vida

- `docker compose stop` detiene sin borrar el volumen `springfield-postgres-data`.
- `docker compose start` lo vuelve a levantar; migraciones Flyway V1/V2/V3 no se reejecutan.
- Jamás: `docker compose down -v`, `docker volume prune`, `docker system prune`.

## Conexión desde el host (psql)

```bash
psql "host=127.0.0.1 port=5432 dbname=<POSTGRES_DB> user=<POSTGRES_USER> password=<POSTGRES_PASSWORD>"
```

## Orden recomendado para la primera puesta en marcha

1. `docker compose up -d` + espera `healthy`.
2. Arranca el backend y abre `http://localhost:8080`.
3. Dispara la importación (`$SPRINGFIELD_ADMIN_TOKEN`); sigue con `/api/v1/sync/status`.
4. Verifica: `http://localhost:8080/api/v1/stats` (totales) y el frontend con datos reales.
