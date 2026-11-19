#!/usr/bin/env bash
# Diagnóstico no destructivo de dependencias y servicios locales; no imprime secretos.
set -u
printf 'Springfield Archive | Diagnóstico de Omarchy / Linux\n'
for app in java node npm docker curl; do
  if command -v "$app" >/dev/null 2>&1; then
    printf 'OK  %-8s %s\n' "$app" "$(command -v "$app")"
  else
    printf 'NO  %-8s No instalado o fuera de PATH\n' "$app"
  fi
done
printf '\n'
if command -v java >/dev/null 2>&1; then java -version 2>&1 | head -n 1; fi
if command -v node >/dev/null 2>&1; then node --version; fi
if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    printf 'Docker Engine: accesible\n'
    docker compose -f backend/docker-compose.yml ps 2>/dev/null || true
  else
    printf 'Docker Engine: instalado, pero no accesible o no iniciado\n'
  fi
fi
if command -v curl >/dev/null 2>&1; then
  printf '\nAPI local (http://127.0.0.1:8080/actuator/health): '
  code=$(curl -sS --max-time 3 -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/actuator/health 2>/dev/null) || code='SIN RESPUESTA'
  printf '%s\n' "$code"
fi
printf '\nEste diagnóstico no inicia servicios, no accede a credenciales y no borra datos.\n'
