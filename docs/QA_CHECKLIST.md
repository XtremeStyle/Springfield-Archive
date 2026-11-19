# Lista de aceptación técnica | Springfield Archive 1.1.0

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
- [ ] Revisión manual del sonido Duff corto con el interruptor único del encabezado en Omarchy.
- [ ] Revisión con lector de pantalla, teclado y contraste en el navegador de la entrega.

## Backend y persistencia

- [x] Pruebas Java aisladas de reglas y seguridad (`bash scripts/test-java-security.sh`).
- [x] Inspección de consultas SQL parametrizadas y controles administrativos en el código.
- [ ] `cd backend && ./mvnw -B clean verify` con Docker/Testcontainers activos.
- [ ] Aplicación de migraciones Flyway V1-V7 con la PostgreSQL de Omarchy sin datos destruidos.
- [ ] Sincronización real de characters, episodes, locations desde The Simpsons API hasta estado `completed`.
- [ ] Validación de `scripts/verify-apis.py --local --require-synced` desde el equipo del usuario.
- [ ] Comprobar fichas e imágenes reales y ausencia de duplicados en la vista consolidada.
- [ ] Confirmar un JAR actualizado con el build Maven en Omarchy (`compilar`).

## Entrega y seguridad

- [x] Archivos fuente, migraciones, pruebas, launcher y documentación incluidos; ningún `.env` real, binario obsoleto o `node_modules`.
- [x] Prototipos antiguos retirados de los archivos públicos.
- [x] README, manual Omarchy, créditos, guía de exposición y matriz de aceptación revisados.
- [ ] Revisión final del repositorio remoto después de una publicación expresamente autorizada.
- [ ] Despliegue externo autorizado, con backend HTTPS, PostgreSQL persistente y secretos de producción independientes.

**Regla:** los test de Chromium que interceptan `fetch` comprueban UX y contrato del frontend; no sustituyen Docker, Flyway o una importación real.
