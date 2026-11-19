import { backendOrigin } from './deployment-config.js';
// Único cliente del frontend: consume /api/v1 del backend Java, jamás la API externa.
const API_BASE = '/api/v1';
// El servicio gratuito puede despertar lentamente tras inactividad.
// La política de timeout se mantiene acotada; no realizamos llamadas en bucle.
const DEFAULT_TIMEOUT = 75000;
// Rutas de solo lectura de nuestro backend. Evita que un nuevo módulo acceda
// accidentalmente a endpoints internos mediante /.. o %2f.
const PUBLIC_API_PATH = /^\/(?:characters|episodes|locations)(?:\/(?:external\/)?[a-z0-9-]+)?$|^\/(?:stats|sync\/status)$/;

export async function fetchJson(path, params = {}, options = {}) {
    if (typeof path !== 'string' || !PUBLIC_API_PATH.test(path)) {
        throw new Error('Ruta de la API no permitida');
    }
    const url = new URL(API_BASE + path, backendOrigin || window.location.origin);
    for (const [key, value] of Object.entries(params)) {
        if (value !== undefined && value !== null && value !== '') {
            url.searchParams.set(key, String(value));
        }
    }
    const controller = new AbortController();
    const onExternalAbort = () => controller.abort();
    if (options.signal) {
        if (options.signal.aborted) { controller.abort(); }
        else { options.signal.addEventListener('abort', onExternalAbort, {once: true}); }
    }
    const timeoutMs = Number.isFinite(options.timeoutMs) && options.timeoutMs > 0
        ? options.timeoutMs : DEFAULT_TIMEOUT;
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
        let response;
        try {
            response = await fetch(url, {
                headers: {'Accept': 'application/json'},
                signal: controller.signal,
                credentials: 'same-origin',
            });
        } catch (error) {
            if (controller.signal.aborted) {
                if (options.signal?.aborted) { throw error; }
                throw new Error('La consulta tardó demasiado. Comprueba la conexión con el servidor Java.');
            }
            throw new Error('No se pudo conectar con Java. Comprueba que Java esté iniciado y que el origen HTTPS del backend esté configurado.');
        }
        const type = response.headers.get('content-type') || '';
        if (!response.ok) {
            let message = `Error HTTP ${response.status}`;
            if (type.includes('json')) {
                try {
                    const body = await response.json();
                    message = String(body.detail || body.title || message).slice(0, 350);
                } catch { /* algunos errores no devuelven JSON */ }
            }
            const error = new Error(message);
            error.status = response.status;
            throw error;
        }
        if (!type.includes('json')) {
            throw new Error('El servidor no devolvió JSON. Revisa la conexión con Spring Boot y la configuración de origen del backend.');
        }
        try { return await response.json(); }
        catch { throw new Error('El servidor devolvió un JSON inválido.'); }
    } finally {
        clearTimeout(timer);
        options.signal?.removeEventListener('abort', onExternalAbort);
    }
}
