# Lista de aceptación técnica | Springfield Archive 1.1.1 (actualizada 2026-11-19)

Los controles con marca se ejecutaron en el entorno de preparación; los controles pendientes **no** se deben declarar superados por inspección ni por simulación.

## Frontend y juego

- [x] Pruebas JavaScript (`cd frontend && npm test`).
- [x] Validación estática y rutas de los 12 documentos HTML (`npm run validate`).
- [x] Sintaxis de los módulos JavaScript (`node --check`).
- [x] Tres recorridos completos de las seis fases de Springfield Files a 1366, 390 y 320 px **con API simulada**.
- [x] Fichas de candidatos reservadas hasta su inspección; primer par de inspecciones gratuito, tercera con coste.
- [x] Apertura de pruebas, tres veredictos, auditoría del intruso, ordenación con desempate, cifrado 742 y final.
- [x] Seis catálogos y una ficha examinados en navegador con la estructura JSON del backend simulada.
- [x] Ausencia de desplazamiento horizontal en pantallas pequeñas durante estos recorridos.
- [x] Sonido Duff verificado en navegador (apertura de latas en Duff Projects) y pruebas JS de audio.
- [ ] Revisión formal con lector de pantalla (pendiente; navegación por teclado y contraste AA revisados).

## Backend y persistencia

- [x] Pruebas Java aisladas de reglas y seguridad (`bash scripts/test-java-security.sh`).
- [x] Inspección de consultas SQL parametrizadas y controles administrativos en el código.
- [x] `cd backend && ./mvnw -B clean verify` con Docker/Testcontainers activos (83/83 tests, PostgreSQL 18).
- [x] Aplicación de migraciones Flyway V1–V8 con PostgreSQL real, sin datos destruidos (local y Neon).
- [x] Sincronización real de characters, episodes, locations desde The Simpsons API hasta estado `completed` (0 errores; Kodos edad 1 000 000 corregida con V8).
- [x] Validación de `scripts/verify-apis.py --local --require-synced` (9/9 comprobaciones, equipo y CI).
- [x] Comprobar fichas e imágenes reales y deduplicación en la vista consolidada (curación con `archive_rank`).
- [x] JAR actualizado con el build Maven (`backend/target/`), utilizado por el Dockerfile de producción.

## Entrega y seguridad

- [x] Archivos fuente, migraciones, pruebas, launcher y documentación incluidos; ningún `.env` real, binario obsoleto o `node_modules`.
- [x] Prototipos antiguos retirados de los archivos públicos.
- [x] README, manual Omarchy, créditos, guía de exposición y matriz de aceptación revisados.
- [x] Revisión final del repositorio remoto tras la publicación autorizada (`main`, CI en verde, histórico preservado).
- [x] Despliegue público verificado: Netlify (frontend) + Render Free (backend HTTPS) + Neon Free (PostgreSQL 18), secretos inyectados por entorno; ver `docs/DECISIONS.md`.

**Regla:** los test de Chromium que interceptan `fetch` comprueban UX y contrato del frontend; no sustituyen Docker, Flyway o una importación real.

**Actualización 2026-11-19:** la verificación pendiente de esta lista se completó con la publicación real (ver `docs/DECISIONS.md` y `README.md`); permanece sin certificar el uso con lector de pantalla.
