# Despliegue de Springfield Archive — COMPLETADO Y VERIFICADO (2026-11-18/19)

**Estado:** publicado. Sitio: https://springfield-archive.netlify.app · Backend: https://springfield-archive-api.onrender.com (Render Free, Docker) · Base: Neon Free (PostgreSQL 18, Flyway V1–V8, importación completa: 1142 personajes, 768 episodios, 477 lugares + catálogo editorial). Detalle y evidencia en docs/DECISIONS.md. El resto del documento conserva el plan y los riesgos originales.

## Arquitectura propuesta

```text
Netlify Free (frontend estático, HTTPS)
         | consulta HTTPS de solo lectura (CORS exacto)
         v
Render Free u otro hosting compatible (Java 21, Spring Boot)
         | JDBC con TLS
         v
Neon Free u otra base PostgreSQL persistente
         ^
The Simpsons API -> sincronizador administrativo autorizado de Java
```

**Por qué NO usar proxy de Netlify:** los proxies de redirect tienen límites de duración, inferiores al posible despertar en frío de un backend gratuito. Un servidor Java dormido puede necesitar más tiempo. El frontend permite hasta 75 segundos para recibir respuesta y muestra un error recuperable si no se logra conectar. En producción Java habilita CORS **solo para el origen Netlify especificado** y solo endpoints públicos de lectura. El token de sincronización nunca se envía al navegador.

## 1. Backend y base de datos

1. Crear PostgreSQL gratuito **persistente**, con TLS habilitado; conservar credenciales en el panel privado. No utilizar un plan de base de datos temporal para datos que deben perdurar.
2. Crear servicio Java usando `backend/Dockerfile`, contexto de build raíz (`.`) y perfil `prod`; confirmar que el proveedor admite el plan gratuito y no exige tarjeta.
3. Configurar solo en el hosting Java:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://HOST/BASE?sslmode=require` (ajustar según proveedor)
   - `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`
   - `SPRINGFIELD_ADMIN_TOKEN` (secreto fuerte aleatorio, mínimo 32 caracteres)
   - `SPRINGFIELD_CORS_ALLOWED_ORIGIN=https://TU-SITIO.netlify.app` (origen exacto, sin slash final)
4. El servicio debe usar HTTPS. Verificar `/actuator/health`, `/api/v1/stats`, `/api/v1/characters`, `/api/v1/episodes`, `/api/v1/locations` después de importar datos.
5. Ejecutar la sincronización desde terminal segura con el secreto privado; verificar tres estados `completed` y totales de la base. No registrar ni imprimir el token en capturas o informes.
6. Comprobar que `POST /api/v1/sync/start` sin token recibe 401/403, y que una petición preflight desde Netlify **no autoriza** ese POST. Swagger queda apagado en `prod`.

## 2. Netlify

1. Con autorización, conectar el repositorio público y elegir raíz (`.`) con `netlify.toml`.
2. Configurar la variable de **build pública** `SPRINGFIELD_BACKEND_ORIGIN=https://TU-BACKEND.onrender.com` (sustituir por origen real, HTTPS, sin puerto personalizado, usuario o ruta). La URL es pública, no una clave.
3. El build ejecuta `node scripts/netlify-build.mjs` y publica `frontend/`. Se generan conjuntamente `frontend/js/deployment-config.js` y `frontend/_headers`: origen de API y CSP. **Si falta la URL, el build falla** intencionadamente en lugar de publicar una wiki rota.
4. Comprobar en DevTools que las consultas van a `https://TU-BACKEND/api/v1/...`, no a The Simpsons API directamente, y que la respuesta tiene `Access-Control-Allow-Origin` del sitio exacto. Verificar con backend frío y caliente.
5. Recorrer búsqueda, paginación, fichas, favoritos, juego entero, dona, Duff y modo móvil; probar proveedor caído, backend caído, CSP, consola y teclado.

## 3. Prueba del build sin publicar

La prueba no modifica el frontend real (usa una copia temporal):

```bash
python3 -m unittest scripts/test_netlify_build.py
```

De forma manual, **en una copia de la carpeta**, `SPRINGFIELD_BACKEND_ORIGIN=https://api.ejemplo.com node scripts/netlify-build.mjs` genera configuración para Netlify. No distribuyas archivos generados con URLs de ejemplo; la versión de desarrollo conserva `backendOrigin = null`.

## 4. Riesgos y criterio de aprobación

- Render Free puede dormir tras 15 minutos; es normal una apertura lenta. Monitorizar respuesta y consumir recursos dentro de límites gratuitos.
- Netlify Free limita consumos de construcción, funciones o ancho de banda según créditos; no prometer 24/7. Verificar que la cuenta no requiera pagos ni facturación automática.
- Una base persistente requiere backups regulares; el plan gratuito no sustituye una copia de seguridad.
- Revisar imágenes de la serie y licencias antes de compartir públicamente; usar atribuciones y no incrustar vídeos completos no autorizados.
- No anunciar “publicado” hasta que una persona compruebe desde la URL pública las tres entidades y el juego usando la API Java real.

**Referencias para comprobar condiciones actuales:** documentación oficial de Netlify (plan Free, límites de proxies), Render (Free web services, suspensión) y Neon (Free plan). Las condiciones pueden cambiar.
