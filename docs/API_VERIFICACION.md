# Springfield Archive — Verificación del requisito principal (API pública)

**Propósito:** demostrar y comprobar que el proyecto consulta The Simpsons API desde Java, persiste los datos en PostgreSQL y muestra información real desde una API REST propia. No se requiere inicio de sesión para explorar el catálogo.

Este documento describe el procedimiento de validación. Los resultados históricos al final corresponden a una versión anterior; para los resultados de la entrega 1.0 consulta [`ENTREGA_1_0.md`](ENTREGA_1_0.md).

## Flujo real

`https://thesimpsonsapi.com/api` → `TheSimpsonsApiClient` → `ExternalImportService` → `CatalogSyncWriter` → PostgreSQL → controladores `GET /api/v1/...` → ES Modules del frontend.

La consulta al proveedor **no ocurre en cada visita**. La sincronización es una operación administrativa explícita. Si el proveedor falla temporalmente, los datos previamente guardados siguen disponibles desde PostgreSQL.

## Verificación técnica en Omarchy

1. Desde `backend/`, arrancar PostgreSQL con Docker Compose según `backend/docs/OPERATIONS.md`. Esperar a que el contenedor esté `healthy`.
2. Con Java 21 y las variables `SPRING_DATASOURCE_*` correctas, ejecutar `./mvnw spring-boot:run` **dentro de `backend/`**.
3. En otra terminal, consultar:

```bash
curl -i http://localhost:8080/actuator/health
curl -sS http://localhost:8080/api/v1/stats | jq .
curl -sS 'http://localhost:8080/api/v1/characters?page=0&size=3' | jq '.content[] | {name,slug,externalId}'
curl -sS 'http://localhost:8080/api/v1/episodes?page=0&size=3' | jq '.content[] | {name,season,episodeNumber}'
curl -sS 'http://localhost:8080/api/v1/locations?page=0&size=3' | jq '.content[] | {name,town}'
```

4. El endpoint de sincronización requiere `SPRINGFIELD_ADMIN_TOKEN` configurado en el **proceso Java** y enviado solo desde tu terminal (nunca desde el frontend ni en Git). Si ya hay datos y no quieres hacer llamadas al proveedor, no dispares una nueva importación. Cuando decidas sincronizar:

```bash
curl -sS -X POST -H "X-Admin-Token: $SPRINGFIELD_ADMIN_TOKEN" \
  'http://localhost:8080/api/v1/sync/start?resource=all' | jq .
curl -sS 'http://localhost:8080/api/v1/sync/status' | jq .
```

5. La respuesta inicial de sync será **202** (trabajo en segundo plano), no significa importación finalizada. Esperar a que los tres recursos indiquen `completed` y que los errores estén en `0`. Si un recurso aparece `failed`, revisar el campo `error`, subsanar el problema y ejecutar nuevamente: la página fallida se reintenta sin duplicar los datos ya persistidos.
6. Verificar las listas, la búsqueda y la ficha de un personaje desde `http://localhost:8080/`. Comprobar con la pestaña *Red/Network* del navegador que la web pide `/api/v1/...` al backend Java y **no** consulta directamente `thesimpsonsapi.com/api`.
7. Opcional: detener temporalmente la conectividad externa **sin apagar PostgreSQL ni el backend**. El catálogo almacenado debe seguir mostrándose. No ejecutar nuevas sincronizaciones durante esta prueba.


### Comprobación de solo lectura (sin importar miles de fichas)

El proyecto incluye `scripts/verify-apis.py`, sin dependencias externas de Python. Desde la raíz:

```bash
python3 scripts/verify-apis.py --upstream
python3 scripts/verify-apis.py --local
python3 scripts/verify-apis.py --local --require-synced
```

`--upstream` valida seis peticiones reales al proveedor (3 listados paginados y 3 fichas individuales), incluidos el formato JSON, IDs y nombres. `--local` valida salud, estadísticas, estado de sincronización, listados y fichas de la API Spring Boot. `--require-synced` rechaza cualquier recurso que aún no esté en `completed`. **Son comprobaciones HTTP GET solamente**: no requieren un token y no modifican la base de datos. No confundir el paso anterior con el POST administrativo de importación.

El propio verificador tiene cuatro pruebas sin red: `python3 scripts/test_verify_apis.py`.

## Criterios de aceptación

- [ ] Responde `GET /actuator/health`.
- [ ] `GET /api/v1/characters`, `/episodes` y `/locations` devuelven JSON paginado con campos `content`, `page`, `size`, `totalElements`, `totalPages`.
- [ ] La importación completa consulta al proveedor con límites y persiste los datos sin duplicarlos.
- [ ] `sync/status` indica realmente el estado; **una página incompleta nunca marca `completed`**.
- [ ] Si una ficha externa falla, la importación se declara `failed` y puede reintentarse desde la página correcta.
- [ ] Swagger muestra los endpoints públicos de catálogo (perfil `local`); operaciones administrativas protegidas.
- [ ] El frontend muestra personajes, episodios y lugares reales y recupera favoritos después de recargar.
- [ ] No se expone una contraseña en Git, README, capturas, consola compartida ni frontend.
- [ ] Búsqueda, paginación, fichas y responsive funcionan en navegador real.

## Evidencia y limitaciones actualizadas (v0.9.6-rc2)

- Además de la documentación pública, se recuperaron y comprobaron respuestas JSON de los endpoints públicos `/api/characters`, `/api/episodes`, `/api/locations` y las fichas `/{id}=1` mediante navegación web. Los listados contenían estructuras `count/pages/results` y los detalles, identificadores coincidentes. **Esto no certifica que el cliente Java, PostgreSQL ni la importación completa funcionen en Omarchy**; el entorno de código no pudo conectarse directamente al proveedor.
- El cliente HTTP Java se probó contra un servidor simulado local: 12 aserciones aisladas exitosas para JSON, errores, reintentos, controles y recuperación. Se compilaron 35 fuentes Java con JDK 21 (41 clases). No equivale a Maven completo.
- `npm test`: 38/38; validación estática: 13 páginas HTML. Chromium: 8 flujos con respuestas simuladas del contrato REST, incluyendo búsqueda, Duff, dos niveles de juego, sellos, errores y responsive. Las capturas están marcadas **datos de prueba**.
- El script de verificación se probó con 4 casos offline, pero **debe ejecutarse en Omarchy con `--upstream` y `--local`** para certificar el proveedor real y Java/PostgreSQL.
- **Pendiente:** Maven/Testcontainers, PostgreSQL, sincronización real completa e idempotencia, y los tres catálogos reales en navegador. No se ha efectuado publicación ni push.
