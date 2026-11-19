#!/usr/bin/env python3
"""Smoke test read-only para proveedor externo y/o backend local.

No descarga catálogos completos, no inicia sync y nunca solicita tokens.
Uso:
    python3 scripts/verify-apis.py --upstream
    python3 scripts/verify-apis.py --local
    python3 scripts/verify-apis.py --local --require-synced
    python3 scripts/verify-apis.py --all --local-url http://127.0.0.1:8080
"""
from __future__ import annotations
import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

KINDS = ('characters', 'episodes', 'locations')
USER_AGENT = 'SpringfieldArchiveQA/1.0.0 (read-only smoke check)'

class VerificationError(Exception):
    pass


def require(condition: bool, message: str):
    if not condition:
        raise VerificationError(message)


def request_json(url: str, timeout: int):
    req = urllib.request.Request(url, headers={
        'Accept': 'application/json', 'User-Agent': USER_AGENT,
    }, method='GET')
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            status = response.status
            require(status == 200, f'HTTP {status} para {url}')
            size_limit = 1024 * 1024
            raw = response.read(size_limit + 1)
            require(len(raw) <= size_limit, f'JSON demasiado grande en {url}')
            return json.loads(raw.decode('utf-8'))
    except (urllib.error.URLError, TimeoutError, ValueError, UnicodeError) as e:
        raise VerificationError(f'No se pudo verificar {url}: {e}') from e


def smoke_upstream(base: str, timeout: int):
    print('\nORIGEN: API pública (peticiones de lectura; no sincroniza la base local)')
    checked = 0
    for kind in KINDS:
        data = request_json(f'{base}/api/{kind}?page=1', timeout)
        require(isinstance(data, dict), f'{kind}: respuesta no es objeto')
        require(isinstance(data.get('count'), int) and data['count'] >= 0, f'{kind}: count inválido')
        require(isinstance(data.get('pages'), int) and data['pages'] >= 1, f'{kind}: pages inválido')
        results = data.get('results')
        require(isinstance(results, list) and results, f'{kind}: results ausentes/vacíos')
        require(all(isinstance(x, dict) and isinstance(x.get('id'), int) and x['id'] > 0
                    and isinstance(x.get('name'), str) and x['name'].strip() for x in results),
                f'{kind}: elementos sin ID o nombre verificable')
        print(f'  OK {kind}: {len(results)} elementos página 1; count={data["count"]}; pages={data["pages"]}')
        checked += 1
        first_id = results[0]['id']
        detail = request_json(f'{base}/api/{kind}/{first_id}', timeout)
        require(isinstance(detail, dict) and detail.get('id') == first_id,
                f'{kind}: ID de ficha individual no coincide')
        print(f'  OK {kind}#{first_id}: detalle individual coincide')
        checked += 1
    return checked


def smoke_local(base: str, timeout: int, require_synced: bool):
    print('\nORIGEN: backend Spring Boot; todas las lecturas ocurren a través de nuestra API REST')
    checked = 0
    health = request_json(f'{base}/actuator/health', timeout)
    require(isinstance(health, dict) and health.get('status') == 'UP', 'health debe estar UP')
    print('  OK /actuator/health: UP')
    checked += 1
    stats = request_json(f'{base}/api/v1/stats', timeout)
    require(isinstance(stats, dict), 'stats no es un objeto JSON')
    for kind in KINDS:
        record = stats.get(kind)
        require(isinstance(record, dict) and isinstance(record.get('total'), int) and record['total'] >= 0,
                f'stats.{kind}.total no es entero >=0')
    print('  OK /api/v1/stats: ' + ', '.join(f'{k}={stats[k]["total"]}' for k in KINDS))
    checked += 1
    status = request_json(f'{base}/api/v1/sync/status', timeout)
    require(isinstance(status, list), 'sync/status no es una lista JSON')
    statuses = {entry.get('resource'): entry.get('status') for entry in status if isinstance(entry, dict)}
    require(set(KINDS).issubset(statuses), 'sync/status no contiene los tres recursos')
    require(not require_synced or all(statuses[k] == 'completed' for k in KINDS),
            'La importación todavía no se completó en los tres recursos')
    print('  OK /api/v1/sync/status: ' + ', '.join(f'{k}={statuses[k]}' for k in KINDS))
    checked += 1
    for kind in KINDS:
        data = request_json(f'{base}/api/v1/{kind}?page=0&size=3', timeout)
        require(isinstance(data, dict), f'{kind}: listado debe ser objeto')
        require(isinstance(data.get('content'), list), f'{kind}: content no es lista')
        require(isinstance(data.get('totalElements'), int) and data['totalElements'] >= 0,
                f'{kind}: totalElements inválido')
        require(data['totalElements'] == stats[kind]['total'],
                f'{kind}: stats.total y totalElements no coinciden')
        for record in data['content']:
            require(isinstance(record, dict) and isinstance(record.get('name'), str)
                    and isinstance(record.get('slug'), str) and record['slug'],
                    f'{kind}: ficha sin name o slug')
        print(f'  OK /api/v1/{kind}: {len(data["content"])} elementos, total={data["totalElements"]}')
        checked += 1
        if data['content']:
            slug = urllib.parse.quote(data['content'][0]['slug'], safe='')
            detail = request_json(f'{base}/api/v1/{kind}/{slug}', timeout)
            require(isinstance(detail, dict) and detail.get('slug') == data['content'][0]['slug'],
                    f'{kind}: ficha individual no coincide')
            print(f'  OK /api/v1/{kind}/{slug}: ficha individual')
            checked += 1
    return checked


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument('--upstream', action='store_true', help='Solo la API pública The Simpsons API')
    mode.add_argument('--local', action='store_true', help='Solo Java/Spring Boot local')
    mode.add_argument('--all', action='store_true', help='API pública y backend local')
    parser.add_argument('--local-url', default='http://127.0.0.1:8080', help='URL del backend local')
    parser.add_argument('--upstream-url', default='https://thesimpsonsapi.com', help='Dominio del proveedor')
    parser.add_argument('--timeout', type=int, default=12, help='Timeout por petición en segundos')
    parser.add_argument('--require-synced', action='store_true', help='Exigir 3 recursos completed (modo local)')
    args = parser.parse_args()
    require(args.timeout >= 1, '--timeout debe ser >=1')
    total = 0
    try:
        if args.upstream or args.all:
            total += smoke_upstream(args.upstream_url.rstrip('/'), args.timeout)
        if args.local or args.all:
            total += smoke_local(args.local_url.rstrip('/'), args.timeout, args.require_synced)
        print(f'\nRESULTADO: {total} comprobaciones exitosas; solo lecturas, sin cambios en la BD.')
        return 0
    except VerificationError as error:
        print(f'\nFALLO: {error}', file=sys.stderr)
        return 1

if __name__ == '__main__':
    raise SystemExit(main())
