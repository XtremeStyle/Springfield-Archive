// Pruebas de contrato para favorites (kind/id estrictamente validados; roundtrip honesto).
import test from 'node:test';
import assert from 'node:assert/strict';

// localStorage stub mínimo (node no tiene DOM en este contexto)
function localStorageStub() {
    const map = new Map();
    return {
        getItem: (k) => (map.has(k) ? map.get(k) : null),
        setItem: (k, v) => map.set(k, String(v)),
        removeItem: (k) => map.delete(k),
        key: (i) => Array.from(map.keys())[i] ?? null,
        get length() { return map.size; },
        clear: () => map.clear(),
    };
}

globalThis.window = { localStorage: localStorageStub() };

const {
    isValidKind,
    isValidFavoriteId,
    favoriteKey,
    isFavorite,
    toggleFavorite,
    favoriteEntries,
} = await import('../js/favorites.js');

test('isValidKind acepta sólo character|episode|location', () => {
    assert.equal(isValidKind('character'), true);
    assert.equal(isValidKind('episode'), true);
    assert.equal(isValidKind('location'), true);
    assert.equal(isValidKind('Character'), false);
    assert.equal(isValidKind('anything'), false);
    assert.equal(isValidKind(null), false);
    assert.equal(isValidKind(''), false);
});

test('isValidFavoriteId acepta slugs 1..80 chars [a-z0-9-], empieza por alnum', () => {
    assert.equal(isValidFavoriteId('homer-simpson'), true);
    assert.equal(isValidFavoriteId('s1-e1-simpsons-roasting-on-an-open-fire'), true);
    assert.equal(isValidFavoriteId('-leading-dash'), false);
    assert.equal(isValidFavoriteId('HOMER'), false);
    assert.equal(isValidFavoriteId('has space'), false);
    assert.equal(isValidFavoriteId('a'.repeat(81)), false);
    assert.equal(isValidFavoriteId(''), false);
});

test('los episodios aceptan slugs largos dentro del esquema real de PostgreSQL', () => {
    const long = 'e' + 'p'.repeat(119);
    assert.equal(isValidFavoriteId(long,'episode'), true);
    assert.equal(isValidFavoriteId(long,'character'), false);
    assert.equal(favoriteKey('episode',long),`springfield-favorites:episode:${long}`);
    assert.equal(favoriteKey('episode',long+'z'),null);
    assert.equal(toggleFavorite('episode',long),true);
    assert.equal(favoriteEntries().some(x=>x.id===long),true);
    assert.equal(toggleFavorite('episode',long),false);
});
test('favoriteKey devuelve null para combinaciones inválidas', () => {
    assert.equal(favoriteKey('character', 'homer-simpson'), 'springfield-favorites:character:homer-simpson');
    assert.equal(favoriteKey('Character', 'homer-simpson'), null);
    assert.equal(favoriteKey('character', '" onerror="x'), null);
    assert.equal(favoriteKey(null, null), null);
});

test('isFavorite/toggleFavorite roundtrip con stub de localStorage', () => {
    window.localStorage.clear();
    assert.equal(isFavorite('character', 'homer-simpson'), false);
    assert.equal(toggleFavorite('character', 'homer-simpson'), true);
    assert.equal(isFavorite('character', 'homer-simpson'), true);
    assert.equal(toggleFavorite('character', 'homer-simpson'), false);
    assert.equal(isFavorite('character', 'homer-simpson'), false);
    // Quitar sin añadir no rompe
    assert.equal(toggleFavorite('episode', 's1-e1-x'), true);
    assert.equal(isFavorite('episode', 's1-e1-x'), true);
});

test('favoriteEntries sólo devuelve pares válidos y lee claves correctas', () => {
    window.localStorage.clear();
    window.localStorage.setItem('springfield-favorites:character:homer-simpson', '1');
    window.localStorage.setItem('springfield-favorites:episode:s1-e1-x', '1');
    window.localStorage.setItem('springfield-favorites:Character:homer-simpson', '1'); // kind inválido
    window.localStorage.setItem('springfield-favorites:character:Bad Slug', '1');      // id inválido
    window.localStorage.setItem('unrelated:key', '1');
    window.localStorage.setItem('springfield-favorites:character:a-b', '0');           // valor 0

    const entries = favoriteEntries();
    const ids = entries.map(e => e.id).sort();
    assert.deepEqual(ids, ['homer-simpson', 's1-e1-x']);
    assert.equal(entries.every(e => ['character', 'episode', 'location'].includes(e.kind)), true);
});
