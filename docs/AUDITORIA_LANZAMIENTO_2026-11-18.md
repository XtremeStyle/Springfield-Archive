# Auditoría de lanzamiento — Springfield Archive 1.1.0

**Fecha:** 18/11/2026 · **Autor:** José Manuel Valencia Gallego (XtremeStyle) · **Alcance:** revisión estática y pruebas ejecutables del paquete descargable, con limitaciones explícitas.

## Evaluación técnica

- **Arquitectura:** Java 21 / Spring Boot 4.1.2 / JDBC con PostgreSQL 18.7 / Flyway V1–V7, API REST propia, frontend sin framework, módulos ES, sonidos locales.
- **Datos:** fuente externa real The Simpsons API, importador con páginas, detalle, reintentos y checkpoints, catálogo servido desde PostgreSQL. Ningún dato externo se toma como traducción oficial. Conservación de rutas de imagen en resync (evita perder imágenes cuando el proveedor devuelve `null`).
- **Consultas:** entrada validada, paginación acotada, consultas SQL parametrizadas; deduplicación de vista sin borrar filas; números de episodio tratados como globales.
- **Seguridad de servidor:** token administrativo para la importación, comparación segura, respuesta externa con límites de bytes/tiempo, proxy de imagen restringido y CSP. Perfil `prod` sin Swagger; secrets inyectados por entorno; CORS solo para origen HTTPS permitido y GET de endpoints públicos.
- **Frontend:** escape de contenido, validación de imágenes, errores recuperables, sonido optativo, juego local de seis fases sin afirmaciones de antitrampas. Despliegue frontend/back separado con CSP generada y build que falla sin backend.
- **Calidad:** eliminación de audio remoto, nueva falla real del juego al agotarse la integridad, efecto original y corto de apertura Duff, fases de auditoría/ordenación y cifrado 742, perfil personal con foto/correo pendiente de consentimiento.

## Matriz de hallazgos y correcciones

| ID | Nivel | Hallazgo | Estado y evidencia | Prueba de regresión |
|---|---|---|---|---|
| SEC-01 | P1 | Proxy Netlify de timeout corto vs. backend gratuito dormido | **Corregido en diseño**: HTTPS directo, timeout visible, CORS explícito, build fail-closed (`scripts/netlify-build.mjs`, `PublicReadCorsConfig.java`) | `test_netlify_build.py`, `PublicReadCorsConfigTest.java` (pendiente Maven) |
| DATA-01 | P2 | Sincronización podía sobrescribir imágenes válidas con `null` | **Corregido en fuente** usando `COALESCE` para episodios y lugares | `CatalogImportTest.resyncDoesNotEraseExistingImagesWhenAnExternalFieldIsNull` (pendiente PostgreSQL) |
| UX-01 | P2 | Partida permitía continuar indefinidamente incluso al agotar integridad | **Corregido**: estado de fracaso real, reinicio; no se regalan victorias | JS `noir-mechanics.test.js` y Chromium `qa_failure_browser.py` con API simulada |
| LIC-01 | P2 | Descarga opcional de grabación sonora externa | **Eliminada**: WAV original local, licencia más clara, CSP sin audio de terceros | `release-audio.test.js`, `sound.test.js` |
| REL-01 | P2 | Publicación podía distribuir credenciales o archivos compilados | **Protección añadida:** `audit_release.py`, CI y paquete limpio | `test_audit_release.py`, escaneo `--package` sobre ZIP extraído |
| PERS-01 | P3 | Foto y correo público no entregados por autor | **Pendiente de información personal**; se usa monograma, sin inventar datos | `frontend/js/portfolio-data.js`, comprobación de URL local |
| INT-01 | P1 | Dependencias Maven, Flyway y PostgreSQL reales no disponibles en este entorno | **Pendiente de aprobación**, nunca etiquetado como validado | `cd backend && ./mvnw -B verify`, integración en Omarchy o GitHub CI |
| DEP-01 | P1 | Backend y PostgreSQL públicos aún no provisionados y probados | **Pendiente de autorización** y aceptación en URL pública | `docs/DESPLIEGUE.md` |

## Pruebas ejecutadas

- Node.js `npm --prefix frontend test`: batería de pruebas JS incluida, sin errores.
- `npm --prefix frontend run validate`: 12 HTML, enlaces y scripts locales.
- Python `python3 -m unittest discover -s scripts -p 'test*.py'`: lanzador, contratos, seguridad del paquete y build Netlify en carpeta temporal.
- Java puro `bash scripts/test-java-security.sh`: pruebas aisladas de token y límites HTTP.
- Chromium + API Java simulada: seis fases del juego en escritorio y móvil, camino de fallo, navegación, Duff, favoritos y búsquedas. **No es una prueba real contra PostgreSQL**.
- Generación y extracción de ZIP: validar integridad, estructura, ausencia de secretos y permisos ejecutables.

## Lo que NO se ha verificado todavía

1. Build completo Maven contra las dependencias actualizadas Spring Boot 4.1.2 / springdoc 3.2.0.
2. Migraciones Flyway y Testcontainers con PostgreSQL real; imports completos y reanudación bajo errores auténticos.
3. Despliegue público, SSL, CORS, CSP y cabeceras recibidas desde sitios reales Netlify/Render/Neon.
4. Auditoría externa de dependencias y pruebas dinámicas de penetración con servicio de producción activo. No afirmar "sin vulnerabilidades".
5. Datos personales del perfil sin consentimiento/documentación del usuario.

**Veredicto:** fuente preparada como candidata de publicación, no aprobada de extremo a extremo. La aceptación final requiere las verificaciones anteriores y permiso de publicación del autor. No se ha realizado push, despliegue ni facturación.
