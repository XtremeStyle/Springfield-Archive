# Entrega y publicación responsable (por autorizar)

1. En Omarchy, **respaldar** la instalación anterior (incluido `.env`, Git, respaldos PostgreSQL; nunca subirlos). No copiar `.git` antiguo indiscriminadamente sobre proyecto extraído.
2. Colocar el contenido limpio de la versión 1.1.0 en tu repositorio de trabajo, inspeccionar `git status`, `git diff --check` y `git diff --stat`. Evaluar cambios localmente antes del commit.
3. Ejecutar pruebas Node, Python, auditoría de secretos y `./mvnw -B verify` con Docker disponible. No hacer commit a ciegas si falla algo.
4. Revisar `LICENSE`, autores y créditos de repositorios compartidos. Solicitar foto pública, medio de contacto y presentación personal aprobada antes de incorporar esos datos.
5. Confirmar el nombre definitivo del repositorio. El proyecto ya referencia `XtremeStyle/Springfield-Archive`; no inventar un segundo repositorio sin decisión expresa.
6. **Solo tras autorización:** crear commit semántico, hacer push y confirmar que GitHub Actions (frontend + Maven/Testcontainers) queda en verde.
7. **Solo tras autorización y revisión de los planes gratuitos:** crear PostgreSQL persistente, Java y Netlify siguiendo `docs/DESPLIEGUE.md`, proteger variables y completar importación inicial.
8. Validar en la URL pública datos reales, CORS, imágenes, tres etapas del juego, sonido opcional, móvil, accesibilidad, HTTPS, errores y captura de evidencia de la entrega.

Convenciones sugeridas para commits: `fix(api): preservar imagenes ante sincronizacion parcial`, `feat(game): cerrar investigacion al agotar integridad`, `fix(deploy): configurar HTTPS directo con CORS restringido`, `docs(release): auditoria y guia final`.

No subir al repositorio: `.env`, secretos, volúmenes PostgreSQL, `target/`, `node_modules/`, caches de Python, descargas de imágenes ni respaldos personales.
