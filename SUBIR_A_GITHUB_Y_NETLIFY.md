# Subir Springfield Archive — guía rápida

**IMPORTANTE:** la carpeta que vas a subir se llama `Springfield-Archive` y contiene `backend/`, `frontend/`, `netlify.toml`, `README.md` y `INICIAR_SPRINGFIELD.run`. **No subas un ZIP directamente como único archivo de tu repositorio.** No publiques `.env`, contraseñas ni archivos de PostgreSQL.

## 1. Revisión local (Omarchy)

1. **Descomprime esta versión en una carpeta nueva.** No la mezcles con una versión anterior.
2. Cierra cualquier servidor viejo que ocupe el puerto 8080.
3. Abre la terminal en la nueva carpeta y ejecuta:

```bash
npm --prefix frontend test
npm --prefix frontend run validate
./INICIAR_SPRINGFIELD.run probar
./INICIAR_SPRINGFIELD.run iniciar
```

4. Abre `http://127.0.0.1:8080/sobre-mi.html`. Debes ver **tu foto de graduación**. En la cabecera dinámica debe indicar **ARCHIVE / 1.1.1**.
5. Ejecuta la sincronización y confirma que los tres estados terminen en `completed`:

```bash
./INICIAR_SPRINGFIELD.run sincronizar
./INICIAR_SPRINGFIELD.run estado
python3 scripts/verify-apis.py --local --require-synced
```

Si no ves la versión 1.1.1, probablemente estás ejecutando una instancia anterior o el navegador usa caché. Comprueba qué proceso utiliza el puerto 8080 con `ss -ltnp '( sport = :8080 )'` y actualiza con Ctrl+Shift+R.

## 2. GitHub — usa tu repositorio existente

El repositorio previsto es `https://github.com/XtremeStyle/Springfield-Archive`. **Conserva su historial**, no hagas `git init` encima de él, y no uses `git push --force`.

En una carpeta de trabajo diferente, clónalo si no tienes una copia Git local reciente:

```bash
git clone https://github.com/XtremeStyle/Springfield-Archive.git Springfield-Archive-git
```

Copia **el contenido** de esta entrega dentro de `Springfield-Archive-git/` (conservando su carpeta `.git`). Una opción segura desde la carpeta que contiene las dos carpetas:

```bash
rsync -av --exclude='.git' --exclude='.env' --exclude='node_modules' --exclude='target' Springfield-Archive/ Springfield-Archive-git/
cd Springfield-Archive-git
git status --short
git diff --check
git add -A
git diff --cached --check
git diff --cached --stat
```

**Revisa** el resultado antes de continuar. Solo cuando estés conforme:

```bash
git commit -m "release: Springfield Archive 1.1.1"
git push origin main
```

Abre el repositorio y confirma que aparezcan la foto `frontend/assets/profile/jose-manuel.webp`, los archivos de código y el README. Comprueba que el workflow de GitHub Actions termine en verde.

## 3. Publicar el backend Java y PostgreSQL

**Netlify no ejecuta Java ni guarda PostgreSQL.** Para que el catálogo funcione públicamente necesitas:

- **PostgreSQL persistente**, por ejemplo Neon Free: crea una base y guarda de forma privada `HOST`, `BASE`, `USUARIO` y `CONTRASEÑA` (nunca los pegues en GitHub).
- **Backend Java**, por ejemplo un servicio web Docker de Render Free conectado al repositorio; configura el contexto de build como **raíz del repositorio** y `backend/Dockerfile` como ruta del Dockerfile.
- Variables del servicio **Java** (solo en el panel privado):

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST/BASE?sslmode=require
SPRING_DATASOURCE_USERNAME=TU_USUARIO
SPRING_DATASOURCE_PASSWORD=TU_CLAVE
SPRINGFIELD_ADMIN_TOKEN=TOKEN_ALEATORIO_DE_32_O_MAS_CARACTERES
SPRINGFIELD_CORS_ALLOWED_ORIGIN=https://TU-SITIO.netlify.app
```

`TU-SITIO` debe ser el nombre definitivo de tu sitio Netlify. La variable de CORS requiere el origen exacto, sin barra final. El token administrativo nunca debe estar en el frontend ni en el repositorio.

Cuando Render te dé una URL HTTPS, prueba `https://TU-BACKEND.onrender.com/actuator/health` y sus endpoints `/api/v1/...`. Importa el catálogo de forma **autorizada y segura**, sin exponer el token en capturas o archivos públicos. Confirma tres estados `completed`.

## 4. Netlify

1. Entra a https://app.netlify.com/ y escoge **Add new project → Import a Git repository → GitHub**.
2. Selecciona `XtremeStyle/Springfield-Archive`, rama `main`.
3. Deja el directorio base en **la raíz del repositorio**. El archivo `netlify.toml` ya contiene:
   - Build: `node scripts/netlify-build.mjs`
   - Publish: `frontend`
4. Antes de desplegar, en las **Environment variables**, crea:

```text
SPRINGFIELD_BACKEND_ORIGIN=https://TU-BACKEND.onrender.com
```

Es la **URL pública** del backend, sin ruta final ni barra; **no es una contraseña**. Si falta, el build falla a propósito para evitar publicar una web rota.
5. Lanza el deploy. Anota tu URL `https://TU-SITIO.netlify.app` y asegúrate de que el backend tiene esa misma URL en `SPRINGFIELD_CORS_ALLOWED_ORIGIN`. Si cambias CORS, vuelve a desplegar/reiniciar el backend.
6. Comprueba en la URL pública portada, foto de Sobre mí, personajes con imágenes, episodios por temporada, lugares, favoritos y una partida completa. Usa DevTools → Network para comprobar que los datos vienen de tu backend HTTPS, no de una maqueta. Repite con el backend frío (los servicios gratis pueden tardar en despertar).

**Si Netlify publica solo la portada y los catálogos no funcionan, falta el backend Java, la base de datos o la configuración CORS.** No entregues una URL pública como funcional hasta comprobarlo.

## 5. Estado de entrega

El proyecto fuente y la foto están incorporados. **No se realizó push ni despliegue en esta conversación.** La integración real con Docker, Flyway y The Simpsons API debe aprobarse en Omarchy o en el hosting antes de afirmar que todo funciona de extremo a extremo.
