// Springfield Archive — favoritos persistentes en el navegador
const PREFIX = 'springfield-favorites:';
const VALID_KINDS = new Set(['character', 'episode', 'location']);
const VALID_ID = /^[a-z0-9][a-z0-9-]*$/;

export function isValidKind(kind) {
    return typeof kind === 'string' && VALID_KINDS.has(kind);
}

export function isValidFavoriteId(id, kind = 'character') {
    // El esquema SQL acepta hasta 120 caracteres para episodios, y hasta 80
    // para personajes/lugares. Sin este límite contextual se perdían favoritos.
    const max = kind === 'episode' ? 120 : 80;
    return typeof id === 'string' && id.length > 0 && id.length <= max
        && VALID_ID.test(id) && !id.endsWith('-') && !id.includes('--');
}

export function favoriteKey(kind, id) {
    if (!isValidKind(kind) || !isValidFavoriteId(id, kind)) {
        return null;
    }
    return `${PREFIX}${kind}:${id}`;
}

export function isFavorite(kind, id) {
    const key = favoriteKey(kind, id);
    if (!key) { return false; }
    try { return window.localStorage.getItem(key) === '1'; }
    catch (_) { return false; }
}

export function toggleFavorite(kind, id) {
    const key = favoriteKey(kind, id);
    if (!key) { return false; }
    try {
        const next = !isFavorite(kind, id);
        if (next) {
            window.localStorage.setItem(key, '1');
        } else {
            // Borrar la clave en lugar de guardar '0': sin restos muertos en localStorage.
            window.localStorage.removeItem(key);
        }
        return next;
    } catch (_) { return false; }
}

export function favoriteEntries() {
    const out = [];
    try {
        for (let i = 0; i < window.localStorage.length; i += 1) {
            const key = window.localStorage.key(i);
            if (!key || !key.startsWith(PREFIX)) { continue; }
            if (window.localStorage.getItem(key) !== '1') { continue; }
            const rest = key.slice(PREFIX.length);
            const sep = rest.indexOf(':');
            if (sep <= 0) { continue; }
            const kind = rest.slice(0, sep);
            const id = rest.slice(sep + 1);
            if (!isValidKind(kind) || !isValidFavoriteId(id, kind)) { continue; }
            out.push({ kind, id });
        }
    } catch (_) { /* localStorage no disponible */ }
    return out;
}
