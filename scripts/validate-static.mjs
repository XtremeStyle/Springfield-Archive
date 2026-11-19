// Revisión estática de los HTML originales: enlaces locales, CSS, módulos, semántica básica y CSP.
import { readdirSync, readFileSync, existsSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
const root = resolve(fileURLToPath(new URL('../frontend/', import.meta.url)));
const errors = [];
let pages = 0;
for (const name of readdirSync(root).filter(n => n.endsWith('.html'))) {
  const html = readFileSync(join(root,name),'utf8');
  pages++;
  const report = message => errors.push(`${name}: ${message}`);
  if (!/<html\s+lang="es"/i.test(html)) report('falta <html lang="es">');
  if (!/<main\b[^>]*>/i.test(html)) report('falta elemento <main>');
  if (!/<title>[^<]+<\/title>/i.test(html)) report('falta <title>');
  if (!/name="viewport"/i.test(html)) report('falta viewport responsive');
  if (/<script\b(?![^>]*src=)[^>]*>/i.test(html)) report('script inline; bloqueado por CSP');
  if (/\bon(?:click|error|load|mouseover)\s*=/i.test(html)) report('handler inline incompatible con CSP');
  const ids = [...html.matchAll(/\bid="([^"]+)"/g)].map(m => m[1]);
  if (new Set(ids).size !== ids.length) report('IDs HTML duplicados');
  for (const [,path] of html.matchAll(/\b(?:href|src|action)="(\/[a-zA-Z0-9._/-]+)"/g)) {
    const local = path === '/' ? '/index.html' : path;
    if (!existsSync(join(root, local.slice(1)))) report(`enlace o recurso local inexistente ${path}`);
  }
}
if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}
console.log(`OK: ${pages} páginas HTML verificadas (semántica básica, CSP y archivos locales).`);
