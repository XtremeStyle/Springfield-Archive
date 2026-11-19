#!/usr/bin/env python3
"""Verificador no destructivo de los listados SQL de Springfield Archive.

Compara la vista consolidada con los registros originales sin leer PostgreSQL
ni publicar tokens. Sirve para detectar si Omarchy ejecuta una versión antigua.
"""
import argparse
import json
import re
import sys
import unicodedata
from collections import Counter
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urljoin
from urllib.request import urlopen


def canonical(name):
    text = unicodedata.normalize('NFKD', name.casefold())
    return ''.join(ch for ch in text if ch.isalnum() and not unicodedata.combining(ch))


def get_page(base, entity, source, page):
    path = f'/api/v1/{entity}?'+urlencode({'source': source, 'page': page, 'size': 50, 'scope':'name', 'order':'az'})
    with urlopen(urljoin(base.rstrip('/')+'/',path), timeout=8) as response:
        if response.status != 200 or 'json' not in response.headers.get_content_type():
            raise RuntimeError(f'Respuesta no JSON de {entity}, pagina {page}')
        data = json.load(response)
    if not isinstance(data, dict) or not isinstance(data.get('content'), list):
        raise RuntimeError(f'Respuesta incompatible de {entity}, pagina {page}')
    return data


def inspect(base, entity, all_pages):
    curated = get_page(base, entity, 'curated', 0)
    original = get_page(base, entity, 'all', 0)
    page_count = min(101, curated.get('totalPages',0)) if all_pages else min(3, curated.get('totalPages',0))
    rows = list(curated['content'])
    for i in range(1,page_count):
        rows.extend(get_page(base, entity, 'curated', i)['content'])
    names = [r.get('name','') for r in rows if isinstance(r.get('name'),str)]
    duplicates = {k:n for k,n in Counter(canonical(x) for x in names).items() if k and n>1}
    print(f'[{entity}] vista principal: {curated.get("totalElements", "?")} | registros originales: {original.get("totalElements", "?")} | inspeccionados: {len(rows)}')
    print(f'[{entity}] nombres repetidos en muestra: {len(duplicates)}')
    if duplicates:
        for k, n in list(duplicates.items())[:6]:print(f'  {k[:65]}: {n} veces')
    if original.get('totalElements') == curated.get('totalElements'):
        print(f'[{entity}] AVISO: ambos totales coinciden. Puede ser normal si no hay duplicados; revisar si el backend se reinicio y ejecuta la edicion 02.')
    if all_pages and duplicates:
        print(f'[{entity}] REVISION: persisten nombres equivalentes despues de consolidar.')
    return not duplicates


def main():
    parser=argparse.ArgumentParser(description='Diagnostica catalogos sin modificar datos')
    parser.add_argument('--url', default='http://127.0.0.1:8080', help='Origen del backend Java')
    parser.add_argument('--all', action='store_true', help='Revisar todas las paginas (limitadas a 101)')
    args=parser.parse_args()
    try:
        results=[inspect(args.url, entity, args.all) for entity in ['characters','locations']]
    except (HTTPError, URLError, ValueError, RuntimeError, TimeoutError) as error:
        print('No se pudo consultar el backend:', str(error)[:300], file=sys.stderr)
        return 2
    return 0 if all(results) else 1

if __name__=='__main__':sys.exit(main())
