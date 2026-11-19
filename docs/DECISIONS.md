> **Documento histórico.** La referencia de publicación vigente es [`AUDITORIA_LANZAMIENTO_2026-11-18.md`](AUDITORIA_LANZAMIENTO_2026-11-18.md) y [`DESPLIEGUE.md`](DESPLIEGUE.md).

# DECISIONS — Springfield Archive

> **DOCUMENTO HISTÓRICO DEL DISEÑO INICIAL (15/11/2026).** Sus hitos y rutas propuestas no reflejan por sí solos la implementación actual. Para el código realmente presente, los cambios y la validación, consultar `README.md`, este registro y `docs/QA_CHECKLIST.md`. Por ejemplo, el frontend implementado consume `/api/v1`, no la ruta `/api` que aquí se había propuesto inicialmente; y los episodios, la investigación y el portafolio ya están incorporados.

Registro de decisiones técnicas del proyecto. Cada decisión indica su estado (**Aprobada**, **Propuesta pendiente**, **Aplazada**), la fecha y, cuando procede, la fuente técnica. Ninguna decisión se marca como aprobada sin evidencia.

**Fecha de elaboración:** 2026-11-15
**Última actualización:** 2026-11-15 (Hito 1)

> Este documento describe decisiones y planes, no funcionalidades implementadas. Nada aquí implica que el software exista todavía.

---

## 1. Arquitectura aprobada

- Frontend estático: HTML5, CSS3, JavaScript ES Modules, sin framework ni bundler.
- Backend Java 21 + Spring Boot 4.1.x (rama soportada más longeva; 4.0.x pierde soporte OSS el 2026-12-31 y 3.5.x ya lo perdió el 2026-06-30).
- PostgreSQL 18 + Flyway (migraciones versionadas).
- Monolito modular (Controller → DTO → Repository). Sin microservicios, sin capas innecesarias.
- API pública de lectura, sin autenticación.
- Recursos iniciales: **Personajes** y **Lugares**.
- Desarrollo local como requisito principal; Netlify + Render + Neon sólo como alternativa de despliegue opcional (Hito 7, con autorización expresa previa y re-verificación de límites).

## 2. Stack y versiones

| Componente | Versión / política | Fuente (consultada 2026-11-15) |
|---|---|---|
| Java | 21 LTS (Temurin u otra distribución aprobada; el soporte depende del proveedor) | Adoptium; documentación de Temurin |
| Spring Boot | 4.1.x (≥4.1.1) | spring.io/blog (Spring Boot 4.0.0 GA, 2025-11-20); spring.io/projects/spring-boot/generations |
| Maven Wrapper | Dentro de `backend/`; ejecución `cd backend && ./mvnw …` | maven.apache.org/wrapper |
| Node.js (tooling/CI) | **26.13.1** LTS vía `.nvmrc`; sin actualizar el Node local sin autorización | nodejs.org/en/about/previous-releases (26 LTS desde 2026-10-28) |
| PostgreSQL | **18** (imagen `postgres:18`; volumen en `/var/lib/postgresql`) | Docker Library Postgres 18 |
| Flyway | `spring-boot-starter-flyway` + `org.flywaydb:flyway-database-postgresql` (PG 18 con módulo específico; Boot 4 modulariza autoconfiguración) | docs.spring.io (data-initialization) + github.com/flyway/flyway (Verified Versions: 9.2, 18) |
| springdoc-openapi | línea 3.2.0 (para Boot 4.1.x) | springdoc.org (compatibility matrix) |
| CI | GitHub Actions; valores fijados por SHA completo; minutos ilimitados en repos públicos | docs.github.com |

**Entorno auditado (2026-11-15):** Omarchy 4.5.0 (Arch), Java OpenJDK 21.0.13, Node v26.8.2 (anterior a LTS), npm 11.19.1, PostgreSQL cliente psql 18.6, Docker 29.9.2, Compose 5.6.1, Git 2.56.0, Maven no en PATH (wrapper en backend/).

## 3. Decisiones de seguridad

- API pública de lectura: **sin autenticación**. Nada autentica al visitante: ni CORS, ni el proxy Netlify (si algún día existe), ni ninguna cabecera no verificada.
- Rate limiting: **Bucket4j Core 8.21.0**, único bucket **global en memoria por instancia**. Sin Caffeine y sin claves por IP/usuario (no hay fuente de IP verificable sin dominio propio + proxy adicional).
- Valor propuesto (**pendiente de aprobación**): 500 req/min global. Documentar en código y docs: **límite agregado, no individual, no protección DDoS**; un atacante puede agotarlo y afectar a todos durante la ventana; sólo protege recursos del servidor.
- Respuestas al agotar: **HTTP 429 + `Retry-After` + ProblemDetail (RFC 9457)**. Actuator `/actuator/health` exento (probe de plataforma).
- `Cache-Control: public, max-age=300` únicamente en GET 200 de listados/fichas; **nunca** en errores.
- CORS: orígenes locales explícitos sólo en perfil `local` (el origen concreto del servidor local elegido); producción same-origin vía rewrite Netlify (cuando sea Hito 7). Sin orígenes comodín.
- Swagger/springdoc: **desactivado en producción** (`application-prod.yml`); visible en `local|test`.
- HikariCP acotado (`maximum-pool-size=5`), conexiones < límites de infraestructura; query timeout 3 s; Tomcat acotado.
- Sin secretos en repositorio ni frontend; `.env` ignorado; `gitleaks` en CI y local.
- Springfield Files (fases posteriores): puntuaciones/desbloqueos **en servidor**; nunca confiar progreso del cliente.

## 4. Decisiones de backend

- Monolito modular; DTOs propios (no se expone `Page`/`PageImpl`); `PageResponse<T>` con `content, page, size, totalElements, totalPages`.
- Paginación determinista: `size` 1–50 (def. 20), `page` 0–100 (def. 0); orden fijo **`name ASC, id ASC`**.
- Búsqueda `q` **literal** (escapando `%`, `_`, `\`), sin comodines de usuario.
- Validación con Bean Validation; errores RFC 9457; sin trazas al cliente.
- Exposición de Actuator: sólo se expone `health`; el resto, cerrado.
- HikariCP + Tomcat acotados.
- Datos editoriales **originales**: nombres, ocupaciones, hechos verificables; sin guiones, descripciones protegidas ni imágenes de terceros. Sin `age` (no verificable/estable). Restricciones SQL: longitudes, patrón y unicidad de `slug`, `status ∈ {Alive, Deceased, Unknown}`, `type ∈ {home, business, landmark, school, park, plant}`; `occupation` NULLABLE; cadenas vacías y cadenas compuestas exclusivamente por espacios se normalizan a NULL en la aplicación; adicionalmente, PostgreSQL tendrá una restricción CHECK que impida almacenar cadenas vacías o de sólo espacios si la normalización en aplicación fallase. Personajes/marcas siguen siendo propiedad de sus titulares; nada aquí otorga licencia sobre PI de terceros.

## 5. Decisiones de frontend

- HTML5 + CSS3 + ES Modules, sin framework ni bundler (re-evaluable sólo si la interactividad lo exige con evidencia).
- `API_BASE` por defecto `/api`; **configuración local separada y excluida de Git** (`frontend/js/config.local.js`, ignorada) apunta a `http://localhost:8080/api` en desarrollo; **no se usa `localStorage`** como configuración permanente.
- `js/config.js` puede utilizar `js/config.local.js` **sólo cuando esté disponible en desarrollo**; su ausencia **nunca** debe romper producción (el build/distribución de producción debe funcionar con el valor por defecto `/api`). El archivo local está siempre excluido de Git.
- Documentar cómo activar/desactivar la configuración local en README/técnica en su hito.
- Mejora progresiva honesta: sin JS, página informativa estática + `<noscript>` con mensaje; el catálogo requiere JS.
- WCAG 2.2 AA: `lang="es"`, semántica, foco visible, teclado, contraste ≥4.5:1, `prefers-reduced-motion`, `aria-current`.
- Identidad visual propia ("archivo retro de Springfield"); assets sólo originales; ninguna imagen de terceros.

## 6. Estándares de configuración

- Perfil predeterminado al arrancar sin `SPRING_PROFILES_ACTIVE`: **`local`**. `prod` siempre explícito por variable de entorno.
- **`test` es un perfil explícito de pruebas** (selección manual o por CI); no se activa automáticamente junto con `local` ni por defecto.
- Spring Boot **no carga `.env`** automáticamente; las variables se inyectan via entorno de proceso (Docker Compose `env_file:` en su caso, o variables del sistema); nombres compatibles con Relaxed Binding (`SPRING_DATASOURCE_URL`, etc.); correspondencias personalizadas documentadas (`application.yml` vía `${DATABASE_URL:…}`), jamás mágicas.
- Variables sensibles nunca commiteadas (`.env.example` se crea en Hito 3, cuando exista configuración de PostgreSQL).

## 7. Hitos previstos

| Hito | Alcance | Estado |
|---|---|---|
| H1 | Estructura documental + directorios + `.nvmrc` + DECISIONS.md | **Pendiente de aprobación final** |
| H2 | Backend mínimo ejecutable (Boot 4.1, wrapper, `/actuator/health`) | Pendiente |
| H3 | PostgreSQL 18 + Flyway + Testcontainers + semilla original | Pendiente |
| H4 | API del catálogo (4 endpoints, OpenAPI, Bucket4j global, 429/Retry-After) | Pendiente |
| H5 | Frontend funcional: inicio, personajes, lugares, fichas, 404 | Pendiente |
| H6 | Calidad: CI GitHub Actions, gitleaks, README de funcionalidades reales | Pendiente |
| H7 | Despliegue opcional Netlify + Render Free + Neon Free | Sólo con autorización expresa |

**Metodología por hito:** implementar sólo lo autorizado → pruebas → revisión de seguridad → evidencia → detenerse para revisión. Sin commit/push sin tu autorización.

## 8. Decisiones aplazadas

- Despliegue real (Netlify rewrite `/api/* → Render`, Neon) y re-verificación de límites/costes → Hito 7, con autorización expresa previa.
- Episodios vía TVMaze (referencia editorial, CC BY-SA) → fase posterior.
- Autenticación, favoritos, Springfield Files → fase posterior.
- ESLint/Prettier con `package-lock.json` → opcional en Hito 6.
- Selección definitiva de distribución Java y políticas de soporte (Temurin/others) según contexto de despliegue.

## 9. Riesgos relevantes

1. Free tiers cambian (Netlify pasó de "100 GB" a 300 créditos/mes; Render ya redujo egress a 5 GB/mes; Koyeb cerró free a cuentas nuevas en 2026-02) → H7 condicionado a re-verificación en el momento.
2. Cold start ~1 min en Render Free (spin-down 15 min, 750 h/mes).
3. Datos con PI de terceros → semilla original + revisión de PR; nada de imagen/audio/guion ajeno.
4. IP real del visitante no es verificable sin dominio propio + proxy adicional → rate limiting **global** únicamente; documentado como limitación.
5. Agotamiento del bucket global y 429 colectivo → mitigado con cache HTTP, tamaños acotados; documentado.
6. Contradicción docs vs realidad → sólo se documenta lo implementado y verificado.

## 10. Fuentes técnicas (consultadas 2026-11-15)

- spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now y la matriz de soporte en spring.io/projects/spring-boot/generations (4.1.x, OSS hasta 2027-07-31).
- docs.spring.io/spring-boot/how-to (webserver: forwarded headers y estrategia `NONE/NATIVE/FRAMEWORK`; data-initialization: `spring-boot-starter-flyway` + `flyway-database-postgresql` obligatorios para PG).
- github.com/flyway/flyway — PostgreSQL Verified Versions: **9.2, 18**.
- nodejs.org/en/about/previous-releases — Node 26 'Lithium' Active LTS desde 2026-10-28; 26.13.1 (2026-11-10).
- springdoc.org — matriz Boot 4.1.x ↔ springdoc 3.1.x–3.2.x.
- docs.netlify.com — credit-based Free plan (300 créditos/mes); rewrites externos y signed redirects (no usados en este fase; documentados como alternativa futura).
- render.com/docs/free · neon.com/docs/introduction/plans · docs.github.com (SHA-pinning).

*Esta investigación puede quedar desactualizada; cualquier cambio de límites o versiones debe re-verificarse antes de su uso.*

---

## 2026-11-16 — Hito Integral (versión 1 funcional)

Estado: implementada la arquitectura completa de la primera versión funcional.

### Componentes incorporados

- **Backend:** cliente HTTP configurables hacia The Simpsons API (`thesimpsonsapi`),
  DTO externos (`External*`), importador reanudable en segundo plano (`sync/ExternalImportService`),
  escritura idempotente en PostgreSQL (`CatalogSyncWriter`) con upsert por `external_id`
  y resolución determinista de colisiones de slug (sufijo `-external_id` si el slug ya existe para otro registro).
- **Esquema:** migración `V3__extend_for_external_sync.sql`
  (episodios nuevos, `sync_state`, idempotencia, índices, `birthdate/gender/phrases` en personajes,
  `town/use/image_path` en lugares; `description` pasa a NULLABLE en ambos recursos).
- **API pública (prefijo `/api/v1`):** listados y detalle de personajes, episodios y lugares
  (búsqueda literal, `q`, paginación determinista `name/season/episode_number/id`),
  estadísticas (`/api/v1/stats`), estado de sync (`/api/v1/sync/status`)
  e importación controlada tras token (`POST /api/v1/sync/start` con `X-Admin-Token = SPRINGFIELD_ADMIN_TOKEN`).
- **Frontend:** HTML/CSS/ES Modules, sin framework; servido same-origin desde `./frontend/`
  por Spring en perfil `local`; navegación, catálogo con búsqueda paginada, favoritos (localStorage),
  estadísticas en vivo, página de atribuciones y 404.
- **Atribución visible:** `docs/ATTRIBUTIONS.md` (código MIT propio; datos CC BY-SA de The Simpsons Wiki;
  marcas de *The Simpsons* → titulares).

### Principios respetados

- Idempotencia de la importación, reanudación por página y reintentos limitados con `backoff`.
- El público nunca dispara sincronizaciones; sólo el token administrativo local.
- Backend sin llamadas a Internet en caliente; toda consulta del frontend pasa por PostgreSQL local.
- Sin dependencias no autorizadas; se añadió `com.sun.net.httpserver` en tests (parte del JDK, sin POM extra)
  y Spring Boot + Apache Maven (ya existentes).

### Git

- Commit local de Hito 1 (`5ed2096`) sin push en esta iteración.
- Se conserva `V1/V2` exactamente; toda la evolución se hace por `V3` y posteriores.

---

## 2026-11-18 — Auditoría de lanzamiento 1.1.1 y correcciones

Estado: correcciones aplicadas y verificadas en Omarchy antes de publicar.

### Correcciones de código y pruebas

- `CatalogMigrationTest`: esperaba 6 migraciones y 14 columnas en `characters`; V7 añadió `age` (15 columnas) y hay 7 migraciones. Se actualizan las expectativas al esquema real (ningún cambio de esquema).
- `CatalogApiTest`: las seis pruebas de búsqueda de lugares insertaban filas editoriales pero consultaban la vista `curated` (solo `external_id NOT NULL`); se declara `source=editorial` como en el resto de fixtures editoriales. Las consultas con parámetros duplicados `source` se consolidan.
- `OpenApiConfig` y `pom.xml`: versión unificada a **1.1.1** (antes 1.0.1/1.1.0); `OpenApiDocumentationTest` actualizado en consecuencia.
- `ProdProfileSecurityTest` falló al añadir el handler genérico: `NoResourceFoundException` debe responder 404, no 500. Añadido handler explícito.
- `StatsController.sync/status`: el campo público `error` (detalle crudo de fallos de BD/API) se sustituye por `hasError` booleano; el detalle queda en los logs del servidor.
- `application-prod.yml`: `server.forward-headers-strategy: framework` (HSTS real tras TLS del hosting), `shutdown: graceful`, timeout de conexión y `server.error.include-*: never`.
- `GlobalExceptionHandler`: handler genérico `Exception` → `ProblemDetail` 500 sin detalles internos, con log del lado servidor.
- `PublicReadCorsConfig`: acepta también la variable documentada `SPRINGFIELD_CORS_ALLOWED_ORIGIN` como respaldo explícito del placeholder.
- `EpisodeRepository`: eliminado `findIdByExternalId` (código muerto con truncamiento `Long→Integer`).

### Frontend

- Contraste `.noir-kicker` corregido en superficies claras (`#8a6d1e`); variantes oscuras conservan `#e3c46b` (WCAG AA).
- `favoritos.js`: quitar un favorito actualiza el DOM en sitio (sin re-consultar toda la API).
- `favorites.js`: al desmarcar se borra la clave de localStorage (antes quedaban claves `'0'` muertas).
- `noir-core.js`: el expediente intruso se elige aleatoriamente entre todos los distractores auténticos (antes siempre el primer distractor del primer caso); `isIntruder` ya no reconstruye un paquete determinista con `rng=()=>0`.
- `noir-game.js`: la derrota ya no marca la partida como "6/6 completada"; etiqueta del código de la caja fuerte asociada al grupo de botones (`role="group"` + `aria-labelledby`).
- `list.js`: eliminada rama muerta de agrupación para personajes/lugares (solo episodios se agrupan por temporada).
- `portfolio.js`: eliminada rama muerta de foto dinámica (la foto se sirve estáticamente); `mailto:` sin `encodeURIComponent` innecesario.
- `404.html`: `noindex`. Nuevo `frontend/_headers` de respaldo (CSP `connect-src 'self' https:`); el build de Netlify lo sobrescribe con la CSP estricta del origen real.

### Verificación local

- `./mvnw -B clean verify`: **83/83 pruebas en verde** con Testcontainers (PostgreSQL 18 real).
- `npm test` (57), `npm run validate`, `python3 -m unittest discover` (19), `scripts/test-java-security.sh` (19), gitleaks (0 hallazgos).
- `verify-apis.py --upstream` y `--local --require-synced`: 6/6 y 9/9 comprobaciones.
- Navegador (Chromium, API real): portada, catálogos, fichas, favoritos, Sobre mí (foto), latas Duff, juego y 404 en 1366×768, 390×844 y 320×700, sin errores de consola.
- Decisión: `frontend/tests/` sigue publicándose con el sitio (repo público de todos modos; sin secretos ni datos privados). Riesgo aceptado y documentado.

## 2026-11-18 (tarde) — Despliegue público verificado y CSP de la insignia Netlify

- **GitHub**: PR #1 (release/springfield-1.1.1 → main, merge squash `50a8ff8`) y PR #2 (migración `V8__relax_character_age_check.sql` → `8a0c83f`). CI verde en ambos (frontend + Testcontainers).
- **Neon** (plan Free, región AWS US East 2): proyecto `springfield-archive`, PostgreSQL 18, Flyway V1–V8 aplicadas el 2026-11-18/19. Importación completa verificada vía `/api/v1/sync/status`: personajes completed (1142; 1222 con editorial), episodios 768/768, lugares 477/477, 0 errores. V7 acotaba `age` a 10 000; el proveedor publica edades ficcionales (p. ej. Kodos, 1 000 000) y la importación completa fallaba en la página 3 → V8 amplía el tope a 2·10⁹ sin reescribir V7.
- **Render** (plan Free): web service `springfield-archive-api` (Docker, `backend/Dockerfile`, health `/actuator/health`, Ohio). Primera sincronización fallida por V7; tras V8 verificada completa en el servidor. Servicio público en `https://springfield-archive-api.onrender.com`.
- **Netlify** (plan Free): `springfield-archive.netlify.app`, build `node scripts/netlify-build.mjs`, publish `frontend/`, rama `main`, visibilidad pública (estaba "Private · only you"; se abrió explícitamente). `SPRINGFIELD_BACKEND_ORIGIN` se eliminó como "secret" y se recreó como variable pública: el build fail-closed escaneaba el valor en `frontend/_headers`/`deployment-config.js` y rechazaba publicar (correcto para un secreto, falso positivo para una URL pública).
- **Insignia HUD gratuita de Netlify** (`/.netlify/scripts/hud`): inyecta un iframe same-origin con dos bloques inline fijos (`<style>`, `<script>`; hashes estables para la variante `public`). Nuestra CSP `script-src 'self'`/`style-src 'self'` los bloqueaba en cada página. `netlify-build.mjs` añade ahora los dos hashes SHA-256 a la CSP generada; no se relaja a `'unsafe-inline'`.
- **Nota operativa**: Render Free suspende el servicio por inactividad; la primera petición puede tardar ~50 s. El frontend ya muestra estado y reintenta (75 s).


## 2026-11-19 — Cierre de la publicación (PRs #3–#6) y README con URLs reales

- PR #3 `fix/netlify-csp-hud`: primer hash del HUD Netlify (style). PR #4: segundo estilo (popover). PR #5: tercero (mini). Sin `unsafe-inline`; el HUD gratuito de Netlify es inevitable.
- Consola limpia en producción en todas las páginas revisadas (0 errores) tras permitir los 4 hashes SHA-256 del HUD (1 script + 3 estilos).
- README y docs/DESPLIEGUE.md actualizados con URLs reales: https://springfield-archive.netlify.app (Netlify), https://springfield-archive-api.onrender.com (Render Free), Neon Free (Ohio, PostgreSQL 18, V1–V8).
- Verificación pública final: página inicial, personajes (24 tarjetas reales), episodios, lugares, fichas, créditos, favoritos, sobre-mí (foto visible, 720×900), juego Springfield Files (4 opciones reales, verify habilitado) — todo sin errores de consola; imágenes 24/24 tras carga diferida; CORS: fetch desde el origen Netlify → HTTP 200; /actuator/health UP tras despertar.
- Hallazgo operativo: el primer despliegue de Netlify falló porque SPRINGFIELD_BACKEND_ORIGIN estaba marcado como secreto y el escáner de Netlify detectaba su valor en el paquete; era una URL pública → se recreó como variable pública.
- Render Free se suspende por inactividad: la primera petición tras una pausa puede tardar ~50 s; el frontend ya informa y reintenta.
- Bug propio corregido en el camino: pkill -f demasiado amplio mató las instancias locales (8080/8081); se restauró el servicio local en 8080 desde el último main.
