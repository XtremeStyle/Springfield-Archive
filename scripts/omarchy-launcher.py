#!/usr/bin/env python3
"""Lanzador seguro y reproducible de Springfield Archive en Omarchy/Linux.

No ejecuta .env como codigo, no publica puertos fuera de loopback,
no borra volúmenes ni modifica .env existente.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
BACKEND = ROOT / "backend"
ENV_FILE = BACKEND / ".env"
ENV_KEYS = ("POSTGRES_USER", "POSTGRES_PASSWORD", "POSTGRES_DB", "SPRINGFIELD_ADMIN_TOKEN")
IDENTIFIER = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,62}\Z")
HOST = "http://127.0.0.1:8080"


class LauncherError(RuntimeError):
    pass


def load_env(filename: Path = ENV_FILE) -> dict[str, str]:
    """Read plain key=value pairs, NEVER source/eval the file as shell code."""
    if not filename.is_file():
        raise LauncherError("Falta backend/.env. Ejecuta ./INICIAR_SPRINGFIELD.run configurar")
    try:
        content = filename.read_text(encoding="utf-8-sig")
    except UnicodeError as error:
        raise LauncherError("backend/.env debe estar codificado como UTF-8") from error
    values = {}
    for number, line in enumerate(content.splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise LauncherError(f"Formato inválido en .env, línea {number}; usa NOMBRE=valor")
        key, value = line.split("=", 1)
        key = key.strip()
        if not IDENTIFIER.fullmatch(key):
            raise LauncherError(f"Nombre de variable inválido en línea {number}")
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        if key in ENV_KEYS:
            if not value or "\n" in value or "\r" in value:
                raise LauncherError(f"La variable {key} está vacía o contiene saltos de línea")
            values[key] = value
    missing = [key for key in ENV_KEYS if not values.get(key)]
    if missing:
        raise LauncherError("Faltan variables en backend/.env: " + ", ".join(missing))
    for field in ("POSTGRES_USER", "POSTGRES_DB"):
        if not IDENTIFIER.fullmatch(values[field]):
            raise LauncherError(f"{field} debe ser un identificador SQL sencillo (letras, números, _)")
    if len(values["SPRINGFIELD_ADMIN_TOKEN"]) < 32:
        raise LauncherError("SPRINGFIELD_ADMIN_TOKEN requiere al menos 32 caracteres")
    if len(values["POSTGRES_PASSWORD"]) < 20:
        raise LauncherError("POSTGRES_PASSWORD requiere al menos 20 caracteres en este lanzador")
    return values


def setup() -> None:
    if ENV_FILE.exists():
        print("Configuración existente conservada: backend/.env no se modificó.")
        load_env(ENV_FILE)
        return
    values = {
        "POSTGRES_USER": "springfield_local",
        "POSTGRES_PASSWORD": secrets.token_hex(24),
        "POSTGRES_DB": "springfield_archive",
        "SPRINGFIELD_ADMIN_TOKEN": secrets.token_hex(32),
    }
    old_umask = os.umask(0o077)
    try:
        with ENV_FILE.open("x", encoding="utf-8") as output:
            output.write("# Valores locales generados para esta instalación. Nunca publicar.\n")
            for key in ENV_KEYS:
                output.write(f"{key}={values[key]}\n")
        ENV_FILE.chmod(0o600)
    finally:
        os.umask(old_umask)
    print("Configuración local creada de forma segura en backend/.env (permisos 600).")
    print("No se imprimen contraseñas ni tokens.")


def require(binary: str) -> None:
    if not shutil.which(binary):
        raise LauncherError(f"Falta {binary}. Instálalo en Omarchy antes de continuar.")


def docker_ready() -> None:
    require("docker")
    try:
        subprocess.run(["docker", "info", "--format", "{{.ServerVersion}}"],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=10)
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
        raise LauncherError("Docker no responde. Inicia Docker y comprueba los permisos de tu usuario.") from error
    try:
        subprocess.run(["docker", "compose", "version"], check=True, stdout=subprocess.DEVNULL,
                       stderr=subprocess.DEVNULL, timeout=10)
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
        raise LauncherError("Falta Docker Compose v2.") from error


def java_environment(config: dict[str, str]) -> dict[str, str]:
    env = dict(os.environ)
    env.update({
        "SPRING_PROFILES_ACTIVE": "local",
        "SPRING_DATASOURCE_URL": "jdbc:postgresql://127.0.0.1:5432/" + config["POSTGRES_DB"],
        "SPRING_DATASOURCE_USERNAME": config["POSTGRES_USER"],
        "SPRING_DATASOURCE_PASSWORD": config["POSTGRES_PASSWORD"],
        "SPRINGFIELD_ADMIN_TOKEN": config["SPRINGFIELD_ADMIN_TOKEN"],
    })
    return env


def db_start(config: dict[str, str]) -> None:
    docker_ready()
    cmd = ["docker", "compose", "--env-file", str(ENV_FILE), "up", "-d", "postgres"]
    print("Levantando PostgreSQL local sin eliminar datos...")
    subprocess.run(cmd, cwd=BACKEND, check=True, timeout=180)
    print("Comprobando disponibilidad de PostgreSQL...")
    for _ in range(35):
        result = subprocess.run(["docker", "compose", "--env-file", str(ENV_FILE),
                                 "exec", "-T", "postgres", "pg_isready",
                                 "-U", config["POSTGRES_USER"], "-d", config["POSTGRES_DB"]],
                                cwd=BACKEND, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if result.returncode == 0:
            return
        time.sleep(2)
    raise LauncherError("PostgreSQL no alcanzó el estado ready; consulta 'docker compose logs postgres' en backend/")


def run_app(config: dict[str, str], use_jar: bool = False) -> None:
    require("java")
    db_start(config)
    env = java_environment(config)
    jars = sorted((BACKEND / "target").glob("springfield-archive-api-*.jar"), reverse=True)
    if use_jar and jars:
        cmd = ["java", "-jar", str(jars[0])]
    else:
        if use_jar:
            print("No hay JAR compilado: usaré Maven Wrapper. Para generar el JAR ejecuta 'compilar'.")
        cmd = [str(BACKEND / "mvnw"), "spring-boot:run"]
    print(f"Abriendo Springfield Archive en {HOST}/index.html")
    print("El catálogo principal estará vacío hasta sincronizar la API. Para importar usa 'sincronizar' en otra terminal.")
    print("Ctrl+C detiene Java; los datos de PostgreSQL se conservan.")
    try:
        subprocess.run(cmd, cwd=BACKEND, env=env, check=True)
    except KeyboardInterrupt:
        print("Java detenido. El volumen de PostgreSQL permanece intacto.")


def build_jar() -> None:
    require("java")
    print("Compilando JAR ejecutable mediante Maven Wrapper. Requiere Internet en el primer uso.")
    subprocess.run([str(BACKEND / "mvnw"), "-B", "-DskipTests", "package"],
                   cwd=BACKEND, check=True)
    matches = sorted((BACKEND / "target").glob("springfield-archive-api-*.jar"))
    if not matches:
        raise LauncherError("Maven finalizó, pero no se encontró el JAR esperado.")
    print("JAR disponible en:", matches[0].relative_to(ROOT))
    print("Puedes arrancarlo con ./INICIAR_SPRINGFIELD.run iniciar --jar")


def sync(config: dict[str, str]) -> None:
    request = urllib.request.Request(f"{HOST}/api/v1/sync/start?resource=all", data=b"",
                                     headers={"X-Admin-Token": config["SPRINGFIELD_ADMIN_TOKEN"]},
                                     method="POST")
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            body = json.load(response)
            print("Sincronización iniciada:", json.dumps(body, ensure_ascii=False))
            print("El código 202 NO indica que ya haya terminado. Revisa 'estado'.")
    except urllib.error.HTTPError as error:
        raise LauncherError(f"El servidor Java rechazó la sincronización (HTTP {error.code}).") from error
    except urllib.error.URLError as error:
        raise LauncherError("No se pudo conectar con Java en 127.0.0.1:8080. Inicia el servicio primero.") from error


def status() -> None:
    for path in ("/actuator/health", "/api/v1/stats", "/api/v1/sync/status"):
        try:
            with urllib.request.urlopen(HOST + path, timeout=8) as response:
                payload = json.load(response)
                print(f"{path}: {json.dumps(payload, ensure_ascii=False)[:1200]}")
        except (urllib.error.URLError, TimeoutError, ValueError) as error:
            print(f"{path}: no disponible ({type(error).__name__})")


def test(integration: bool = False) -> None:
    require("node")
    require("python3")
    subprocess.run(["npm", "test"], cwd=ROOT / "frontend", check=True)
    subprocess.run(["node", "scripts/validate-static.mjs"], cwd=ROOT, check=True)
    subprocess.run([sys.executable, "-m", "unittest", "discover", "-s", "scripts", "-p", "test*.py"],
                   cwd=ROOT, check=True)
    subprocess.run(["bash", "scripts/test-java-security.sh"], cwd=ROOT, check=True)
    if integration:
        docker_ready()
        subprocess.run([str(BACKEND / "mvnw"), "-B", "clean", "verify"], cwd=BACKEND, check=True)
    else:
        print("Pruebas backend/Testcontainers no ejecutadas. Utiliza 'probar --integracion' con Docker activo.")


def main() -> int:
    parser = argparse.ArgumentParser(description="Springfield Archive · Ejecutor local seguro para Omarchy")
    sub = parser.add_subparsers(dest="action")
    sub.add_parser("configurar", help="Crear .env con credenciales aleatorias solo si no existe")
    start = sub.add_parser("iniciar", help="Arrancar PostgreSQL y Spring Boot en localhost")
    start.add_argument("--jar", action="store_true", help="Preferir el JAR compilado si existe")
    sub.add_parser("compilar", help="Construir el JAR real con Maven Wrapper")
    sub.add_parser("sincronizar", help="Importar The Simpsons API sin exponer el token en argumentos")
    sub.add_parser("estado", help="Consultar salud, estadísticas y sincronización")
    tests = sub.add_parser("probar", help="Pruebas offline de JavaScript, HTML, Python y Java")
    tests.add_argument("--integracion", action="store_true", help="Ejecutar además Maven/Testcontainers")
    args = parser.parse_args()
    action = args.action or "iniciar"
    try:
        if action == "configurar":
            setup()
        elif action == "iniciar":
            if not ENV_FILE.exists():
                setup()
            run_app(load_env(), getattr(args, "jar", False))
        elif action == "compilar":
            build_jar()
        elif action == "sincronizar":
            sync(load_env())
        elif action == "estado":
            status()
        elif action == "probar":
            test(args.integracion)
        return 0
    except (LauncherError, subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
