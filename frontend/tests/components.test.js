// Pruebas de contrato para los helpers de UI críticos (anti-XSS y sanitización).
import test from 'node:test';
import assert from 'node:assert/strict';

import {
    escapeHtml,
    escapeAttr,
    sanitizeImagePath,
    imageFor,
    imageForItem,
    imageProxyFor,
    IMG_PLACEHOLDER,
    safeCount,
} from '../js/components.js';

test('escapeHtml escapa caracteres peligrosos', () => {
    assert.equal(escapeHtml('<img src=x onerror=alert(1)>'),
        '&lt;img src=x onerror=alert(1)&gt;');
    assert.equal(escapeHtml('"quoted\'single"'), '&quot;quoted&#39;single&quot;');
    assert.equal(escapeHtml(null), '');
    assert.equal(escapeHtml(undefined), '');
    assert.equal(escapeHtml(42), '42');
});

test('escapeAttr es equivalente a escapeHtml (cubre comillas dobles y simples)', () => {
    assert.equal(escapeAttr('" onerror="alert(1)"'), '&quot; onerror=&quot;alert(1)&quot;');
});

test('sanitizeImagePath acepta rutas seguras del CDN', () => {
    assert.equal(sanitizeImagePath('/character/1.webp'), '/character/1.webp');
    assert.equal(sanitizeImagePath('/episode/42.png'), '/episode/42.png');
    assert.equal(sanitizeImagePath('/location/99.jpeg'), '/location/99.jpeg');
    assert.equal(sanitizeImagePath('/short/7.webp'), '/short/7.webp');
});

test('sanitizeImagePath rechaza vectores de ataque y rutas hostiles', () => {
    assert.equal(sanitizeImagePath('javascript:alert(1)'), null);
    assert.equal(sanitizeImagePath('data:text/html,<script>alert(1)</script>'), null);
    assert.equal(sanitizeImagePath('https://evil.example.com/x.png'), null);
    assert.equal(sanitizeImagePath('/character/../../etc/passwd'), null);
    assert.equal(sanitizeImagePath('/character/1.webp?onerror=alert(1)'), null);
    assert.equal(sanitizeImagePath('/character/1.svg'), null);
    assert.equal(sanitizeImagePath('/character/1.webp"><script>'), null);
    assert.equal(sanitizeImagePath(''), null);
    assert.equal(sanitizeImagePath(42), null);
    assert.equal(sanitizeImagePath(null), null);
});

test('imageFor cae siempre en placeholder cuando la ruta no es válida', () => {
    assert.equal(imageFor('/character/1.webp'), 'https://cdn.thesimpsonsapi.com/500/character/1.webp');
    assert.equal(imageFor('javascript:alert(1)'), IMG_PLACEHOLDER);
    assert.equal(imageFor(null), IMG_PLACEHOLDER);
    assert.equal(imageFor(undefined), IMG_PLACEHOLDER);
});

test('safeCount coerciona con seguridad y rechaza salidas negativas/NaN/Infinity', () => {
    assert.equal(safeCount(0), 0);
    assert.equal(safeCount(1222), 1222);
    assert.equal(safeCount('37'), 37);
    assert.equal(safeCount(NaN), 0);
    assert.equal(safeCount(-5), 0);
    assert.equal(safeCount(Infinity), 0);
    assert.equal(safeCount(undefined), 0);
});


test('retrato importado por ruta o ID externo real (sin inventar el ID)', () => {
    assert.equal(imageForItem({portraitPath:'/character/1.webp', externalId:1},'character'),
        'https://cdn.thesimpsonsapi.com/500/character/1.webp');
    assert.equal(imageForItem({portraitPath:null, externalId:42},'character'),
        'https://cdn.thesimpsonsapi.com/500/character/42.webp');
    assert.equal(imageForItem({portraitPath:null, externalId:null},'character'), IMG_PLACEHOLDER);
    assert.equal(imageForItem({imagePath:'/episode/7.webp',externalId:7},'episode'),
        'https://cdn.thesimpsonsapi.com/500/episode/7.webp');
    assert.equal(imageForItem({portraitPath:'javascript:alert(1)', externalId:3},'character'),
        'https://cdn.thesimpsonsapi.com/500/character/3.webp');
});

test('fallback proxy no permite hosts, extensiones ni IDs arbitrarios', () => {
    assert.equal(imageProxyFor('https://cdn.thesimpsonsapi.com/500/character/1.webp'),
        '/api/v1/media/character/1');
    assert.equal(imageProxyFor('https://cdn.thesimpsonsapi.com/200/episode/12.webp'),
        '/api/v1/media/episode/12');
    for (const url of [
        'https://evil.example/500/character/1.webp',
        'https://cdn.thesimpsonsapi.com.evil.example/500/character/1.webp',
        'https://cdn.thesimpsonsapi.com/500/character/0.webp',
        'https://cdn.thesimpsonsapi.com/500/character/../../1.webp',
        'https://cdn.thesimpsonsapi.com/500/character/1.svg',
        'https://cdn.thesimpsonsapi.com/500/character/1.webp?foo=bar',
    ]) { assert.equal(imageProxyFor(url),null); }
});
