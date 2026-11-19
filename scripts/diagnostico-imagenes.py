#!/usr/bin/env python3
"""Comprueba rutas de retratos de los personajes servidos por Spring Boot.

No modifica PostgreSQL, no requiere credenciales ni llama a la API externa.
Uso: python3 scripts/diagnostico-imagenes.py --url http://127.0.0.1:8080
"""
import argparse
import json
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


def get_json(base, path):
    with urlopen(Request(base + path, headers={'Accept': 'application/json'}), timeout=10) as res:
        if 'json' not in (res.headers.get('content-type') or '').lower():
            raise ValueError('El backend respondió HTML, no JSON: revisa Java y /api/v1')
        return json.load(res)


def report(base):
    status = get_json(base, '/api/v1/sync/status')
    data = get_json(base, '/api/v1/characters?page=0&size=50')
    items = data.get('content', [])
    if not isinstance(items, list):
        raise ValueError('El catálogo no tiene content[]')
    imported = [x for x in items if isinstance(x, dict) and isinstance(x.get('externalId'), int)]
    with_photo = [x for x in items if isinstance(x, dict) and x.get('portraitPath')]
    inferred = [x for x in imported if not x.get('portraitPath')]
    manual_without = [x for x in items if isinstance(x, dict) and x.get('externalId') is None and not x.get('portraitPath')]
    print('DIAGNÓSTICO DE IMÁGENES — Springfield Archive')
    print('Personajes totales:', data.get('totalElements', '?'))
    print(f'En la página consultada: {len(items)}; con retrato: {len(with_photo)}; importados sin ruta: {len(inferred)}; editoriales sin retrato: {len(manual_without)}')
    if isinstance(status, list):
        for state in status:
            if isinstance(state, dict) and state.get('resource') == 'characters':
                print('Sincronización de personajes:', state.get('status', 'desconocida'), '- página:', state.get('lastPage', '?'))
    if manual_without:
        print('Personajes editoriales todavía sin coincidencia:', ', '.join(str(x.get('name')) for x in manual_without[:8]))
    if not imported and len(with_photo) == 0:
        print('ACCION: no se observan datos importados con fotos. Ejecuta ./INICIAR_SPRINGFIELD.run sincronizar, espera a que characters sea completed y consulta otra vez.')
    elif manual_without:
        print('ACCION: la migración V6 enlaza retratos con nombres externos únicos; comprueba que Flyway se haya aplicado al iniciar el backend actualizado.')
    else:
        print('Rutas de retrato disponibles. Si siguen sin mostrarse, abre la consola del navegador (F12 > Red) y revisa el dominio CDN y /api/v1/media/.')
    return 0


def main():
    parser = argparse.ArgumentParser(description='Diagnóstico de fotos del catálogo')
    parser.add_argument('--url', default='http://127.0.0.1:8080', help='Origen de Spring Boot, sin /api')
    args = parser.parse_args()
    parsed = urlsplit(args.url)
    if parsed.scheme not in {'http', 'https'} or not parsed.hostname or parsed.username or parsed.password or parsed.path not in {'','/'}:
        parser.error('--url debe ser el origen de Spring Boot, por ejemplo http://127.0.0.1:8080')
    try:
        return report(args.url.rstrip('/'))
    except (HTTPError, URLError, TimeoutError, ValueError, json.JSONDecodeError) as error:
        print('ERROR: no se pudo completar el diagnóstico.', type(error).__name__, file=sys.stderr)
        print('Comprueba que PostgreSQL y Spring Boot estén iniciados y el puerto sea correcto.', file=sys.stderr)
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
