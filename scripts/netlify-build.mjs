/** Produces a fail-closed, no-secret Netlify frontend build.
 * The backend is called directly via HTTPS; Netlify proxies time out after 26s.
 * No login/session tokens are shipped to the browser; this URL is public.
 */
import { writeFileSync } from 'node:fs';
const raw = process.env.SPRINGFIELD_BACKEND_ORIGIN;
if (!raw) { console.error('Falta SPRINGFIELD_BACKEND_ORIGIN, la URL HTTPS pública del backend Java.'); process.exit(1); }
let url;
try { url = new URL(raw); } catch { console.error('El origen Java no tiene formato de URL válido.'); process.exit(1); }
const forbidden = /^(?:localhost|.*\.(?:local|internal|invalid|test)|[0-9.]+|.*:.*)$/i;
const publicHost = /^(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$/i;
if (url.protocol !== 'https:' || url.username || url.password || (url.pathname !== '/' && url.pathname !== '') ||
    url.search || url.hash || url.port || !url.hostname || (forbidden.test(url.hostname) || !publicHost.test(url.hostname))) {
  console.error('Usa un origen HTTPS público sin ruta, parámetros, puertos personalizados ni credenciales.');
  process.exit(1);
}
const origin = url.origin;
// JS and CSP must be generated together. Failing the build prevents a broken deployment.
writeFileSync(new URL('../frontend/js/deployment-config.js', import.meta.url),
  `/** Archivo PUBLICO generado para Netlify. No contiene secretos. */\nexport const backendOrigin = ${JSON.stringify(origin)};\n`);
const csp = "default-src 'self'; base-uri 'self'; object-src 'none'; frame-ancestors 'none'; " +
  "form-action 'self'; "
  // La insignia HUD gratuita de Netlify (/.netlify/scripts/hud) inyecta un iframe con
  // estos dos bloques inline fijos; sus hashes son estables para la variante 'public'.
  + "script-src 'self' 'sha256-BkKP2XO6WOEc3kXOiLr2ZoY83hamalKjPY9xBcbXVuw='; "
  + "style-src 'self' 'sha256-aZ5Vesr500qgQ+yhuHm+u04dmbLmiGknJzGo7MB+XgI=' 'sha256-3y5TlF5zG6/2W5bLJcFMMnGWSDYZMPAJMXGh07elcnY='; " +
  `img-src 'self' data: https://cdn.thesimpsonsapi.com ${origin}; ` +
  "media-src 'self'; " +
  `connect-src 'self' ${origin}; font-src 'self'`;
writeFileSync(new URL('../frontend/_headers', import.meta.url), `/*\n  Content-Security-Policy: ${csp}\n`);
console.log(`Netlify configurado: API Java por HTTPS directo (${origin}); sin proxy de 26 s.`);
