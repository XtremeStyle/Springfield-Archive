# Springfield Archive — Guía de ejecución segura en Omarchy

**Esta aplicación es real:** utiliza Java 21 + Spring Boot 4.1.2 y PostgreSQL 18.7.
El frontend consume los endpoints locales `/api/v1`; la importación a PostgreSQL
es explícita y requiere Internet. Las pruebas de navegador offline no son una
sustitución de esta integración.

## Entrega actual

Este ZIP incluye `INICIAR_SPRINGFIELD.run`, un lanzador Linux, y el código fuente actual. No incluye un instalador autoextraíble de la antigua versión 0.9.8, un JAR precompilado ni credenciales.

## 1. Requisitos

- Omarchy Linux con **Java 21** (`java -version`), **Docker Engine** con
  Docker Compose v2 (`docker compose version`), **Python 3** y **Node.js 22** para
  las pruebas. En el primer uso Maven Wrapper descargará dependencias de Internet.
- Acceso a Docker como usuario normal (`docker info`). No uses `sudo` con el
  lanzador ni copies contraseñas a la terminal.

## 2. Primer arranque

Extrae el ZIP a una carpeta nueva y ejecuta:

```bash
cd Springfield-Archive
chmod +x INICIAR_SPRINGFIELD.run
./INICIAR_SPRINGFIELD.run iniciar
```

Si `backend/.env` no existe, el iniciador crea credenciales aleatorias con permisos
`600`. Nunca modifica un `.env` existente. A continuación arranca exclusivamente
el servicio `postgres` de Docker Compose y ejecuta el backend con
`SPRING_PROFILES_ACTIVE=local` y enlace a `127.0.0.1`.

Abre **http://127.0.0.1:8080/index.html**. El arranque inicial puede tardar más
al descargar dependencias y aplicar Flyway V1–V7. No actualices la BD manualmente:
Flyway conserva el historial aplicado.

## 3. Cargar el catálogo externo

En otra terminal, desde la raíz:

```bash
./INICIAR_SPRINGFIELD.run sincronizar
./INICIAR_SPRINGFIELD.run estado
```

La respuesta `started` no significa completado. Repite `estado` hasta que los
recursos **characters, episodes y locations** estén `completed`. El historial de
sincronización se guarda en PostgreSQL. Si un recurso está `failed`, revisa
la causa y vuelve a ejecutar `sincronizar` cuando la hayas resuelto.

Verifica manualmente:

```bash
python3 scripts/verify-apis.py --upstream
python3 scripts/verify-apis.py --local --require-synced
```

No confundas los 40 personajes y 20 lugares semilla con una importación completa.

## 4. Pruebas y creación del JAR ejecutable

```bash
./INICIAR_SPRINGFIELD.run probar
./INICIAR_SPRINGFIELD.run probar --integracion
./INICIAR_SPRINGFIELD.run compilar
./INICIAR_SPRINGFIELD.run iniciar --jar
```

`probar --integracion` requiere Docker y descarga de dependencias Maven. El JAR
se crea dentro de `backend/target/` después de que **Maven compile los fuentes
actualizados**. No incluimos JARs antiguos de terceros como falso ejecutable.

## 5. Si algo falla

- **Docker no responde:** comprueba `docker info` y permisos de tu usuario.
- **Puerto 5432 ocupado:** hay otro PostgreSQL en ejecución; no borres el
  volumen de Springfield. Revisa primero qué servicio lo usa.
- **Contraseña incorrecta:** quizá el volumen existente se creó con otra
  contraseña. Recupera la configuración original en lugar de reiniciar la BD.
- **No abre la página:** comprueba que Spring Boot se ejecute desde `backend/`
  con perfil `local`, y que su servicio escuche en `127.0.0.1:8080`.
- **No hay datos externos:** revisa `estado`; si solo aparecen registros semilla,
  la sincronización no ha terminado.
- **Se corta la importación:** no borres datos. El sincronizador conserva el
  checkpoint de la última página terminada y permite repetir sin duplicar por ID.

**No hagas** `docker compose down -v`, `docker volume prune`, `git reset --hard`
ni reemplacemientos de `.env`. Para cambiar de rama o versión, conserva la BD y
las credenciales antes de copiar archivos de código.

## 6. Límites de la entrega

El ejecutable `.run` automatiza el arranque local, pero **no contiene** una JVM,
Docker ni los archivos binarios de Maven. Requiere estas herramientas instaladas.
Tampoco implementa registro de usuarios, puntuaciones de servidor ni rankings
antitrampas; los logros y favoritos se almacenan solo en el navegador.
