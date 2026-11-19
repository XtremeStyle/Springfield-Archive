#!/usr/bin/env python3
"""Release safety gate for Springfield Archive.

Checks packaging hygiene and obvious leaked secret signatures. Not a substitute
for dependency audits, a security review, or testing against a real server.
Never prints secret contents. `--package` enforces clean distributable source.
"""
from __future__ import annotations
from pathlib import Path
import argparse
import re
import sys

ROOT=Path(__file__).resolve().parent.parent
REQUIRED=(
    'README.md','LICENSE','frontend/index.html','frontend/game.html',
    'frontend/episodios.html','frontend/js/api.js','frontend/js/noir-game.js',
    'backend/pom.xml','backend/src/main/resources/application-prod.yml',
    'backend/src/main/resources/db/migration/V7__preserve_source_character_age.sql',
    'netlify.toml','scripts/netlify-build.mjs','frontend/js/deployment-config.js',
)
FORBIDDEN_PARTS={'.git','.env','node_modules','target','__pycache__','.venv','.idea','.pytest_cache'}
FORBIDDEN_SUFFIXES={'.pem','.p12','.pfx','.key','.class','.jar','.sqlite','.db'}
SECRET_SIGNATURES={
    'private key':re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    'GitHub credential':re.compile(r'\bghp_[A-Za-z0-9]{36,}\b|\bgithub_pat_[A-Za-z0-9_]{40,}\b'),
    'AWS access key':re.compile(r'\bAKIA[0-9A-Z]{16}\b'),
    'API key':re.compile(r'\bsk-(?:proj-)?[a-zA-Z0-9_-]{40,}\b'),
}
TEXT_EXT={'.java','.js','.mjs','.py','.md','.txt','.yml','.yaml','.toml','.html','.css','.xml','.sh','.run','.properties','.sql','.json'}
SKIP_DIRS={'.git','node_modules','target','__pycache__','.venv','.idea','.pytest_cache'}


def inspect(root:Path,package=False)->list[str]:
    problems=[]
    for name in REQUIRED:
        if not (root/name).is_file():problems.append(f'falta archivo obligatorio: {name}')
    for path in root.rglob('*'):
        rel=path.relative_to(root)
        if not path.is_file():continue
        # A working Git checkout and a local .env may legitimately exist, but
        # must NEVER be in exported packages. Ignore .env content either way.
        if any(p in SKIP_DIRS for p in rel.parts):
            if package:problems.append(f'archivo de desarrollo en paquete: {rel}')
            continue
        if path.name=='.env' or path.name.startswith('.env.') and path.name!='.env.example':
            if package:problems.append(f'secreto local en paquete: {rel}')
            continue
        if path.suffix.lower() in FORBIDDEN_SUFFIXES:
            if package:problems.append(f'binario o archivo sensible en paquete: {rel}')
            continue
        if path.suffix.lower() in TEXT_EXT or path.name in {'Dockerfile','.gitignore','.dockerignore'}:
            if path.stat().st_size>2_000_000:
                problems.append(f'archivo de texto excesivo: {rel}')
                continue
            try:text=path.read_text(encoding='utf-8')
            except UnicodeError:
                problems.append(f'texto ilegible UTF-8: {rel}')
                continue
            for kind,pattern in SECRET_SIGNATURES.items():
                if pattern.search(text):problems.append(f'posible {kind} en {rel} (no se muestra el valor)')
    return problems


def main(argv=None)->int:
    p=argparse.ArgumentParser(description='Auditoría de seguridad del código y paquete Springfield Archive')
    p.add_argument('--package',action='store_true',help='Exigir paquete limpio sin .git/.env ni binarios')
    args=p.parse_args(argv)
    problems=inspect(ROOT,package=args.package)
    if problems:
        print('ERROR: revisión de lanzamiento no aprobada:')
        for detail in problems:print(' - '+detail)
        return 1
    print('OK: estructura y firmas de secretos verificadas ('+('paquete limpio' if args.package else 'fuentes')+').')
    return 0

if __name__=='__main__':sys.exit(main())
