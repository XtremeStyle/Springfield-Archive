import test from 'node:test';
import assert from 'node:assert/strict';
import {fetchJson} from '../js/api.js';

const origin = 'https://springfield.example';
const originalFetch = globalThis.fetch;
const originalWindow = globalThis.window;
function mock(fn) {
    globalThis.window = {location:{origin}};
    globalThis.fetch = fn;
}
test.afterEach(() => {
    globalThis.fetch = originalFetch;
    globalThis.window = originalWindow;
});

test('usa exclusivamente el endpoint de Java y codifica la búsqueda', async () => {
    let requested;
    mock((url, options) => {
        requested = { url: String(url), options };
        return Promise.resolve(new Response(JSON.stringify({content: []}), {headers:{'content-type':'application/json'}}));
    });
    await fetchJson('/characters', {q:'Homer & <script>',page:1,size:24});
    const url = new URL(requested.url);
    assert.equal(url.origin, origin);
    assert.equal(url.pathname, '/api/v1/characters');
    assert.equal(url.searchParams.get('q'), 'Homer & <script>');
    assert.equal(url.searchParams.get('page'), '1');
    assert.equal(requested.options.credentials, 'same-origin');
});

test('rechaza rutas no autorizadas', async () => {
    mock(() => { throw new Error('No debería llamar a fetch'); });
    await assert.rejects(fetchJson('//evil.example/characters'), /Ruta de la API/);
    await assert.rejects(fetchJson('https://evil.example'), /Ruta de la API/);
    await assert.rejects(fetchJson('/../actuator/health'), /Ruta de la API/);
    await assert.rejects(fetchJson('/characters/%2e%2e'), /Ruta de la API/);
    await assert.rejects(fetchJson('/sync/start'), /Ruta de la API/);
});

test('rechaza HTML en vez de JSON', async () => {
    mock(async () => new Response('<html>login</html>', {headers:{'content-type':'text/html'}}));
    await assert.rejects(fetchJson('/characters'), /no devolvió JSON/);
});

test('conserva el HTTP 404 con mensaje JSON para fichas ausentes', async () => {
    mock(async () => new Response(JSON.stringify({detail:'No existe'}), {status:404,headers:{'content-type':'application/problem+json'}}));
    await assert.rejects(fetchJson('/characters/inexistente'), e => e.status===404 && e.message==='No existe');
});

test('informa problemas de conexión de forma comprensible', async () => {
    mock(async () => { throw new TypeError('Failed to fetch'); });
    await assert.rejects(fetchJson('/stats'), /No se pudo conectar con Java/);
});

test('respeta cancelaciones de navegación o búsqueda', async () => {
    mock((url,{signal}) => new Promise((resolve,reject) => {
        signal.addEventListener('abort', () => reject(new DOMException('aborted','AbortError')), {once:true});
    }));
    const c = new AbortController();
    const result = fetchJson('/characters', {}, {signal: c.signal});
    c.abort();
    await assert.rejects(result, /abort/i);
});
