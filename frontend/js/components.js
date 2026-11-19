import { backendOrigin } from './deployment-config.js';
import { initMotion } from './motion.js';
import { initSound, emitSound } from './sound.js';
// Springfield Archive — componentes compartidos, navegación accesible y saneamiento de datos.
const NAV = [
    { href: '/index.html', label: 'Inicio', key: 'inicio' },
    { href: '/personajes.html', label: 'Personajes', key: 'personajes' },
    { href: '/episodios.html', label: 'Episodios', key: 'episodios' },
    { href: '/lugares.html', label: 'Lugares', key: 'lugares' },
    { href: '/favoritos.html', label: 'Favoritos', key: 'favoritos' },
    { href: '/sobre-mi.html', label: 'Sobre mí', key: 'sobre-mi' },
    { href: '/creditos.html', label: 'Fuentes', key: 'creditos' },
];

const ICON = `<svg viewBox="0 0 64 64" aria-hidden="true" class="brand-mark" xmlns="http://www.w3.org/2000/svg">
  <circle cx="32" cy="32" r="29" fill="#f6d44a" stroke="#111d22" stroke-width="4"/>
  <circle cx="32" cy="32" r="17" fill="#e88894" stroke="#111d22" stroke-width="3"/>
  <circle cx="32" cy="32" r="7" fill="#f6d44a" stroke="#111d22" stroke-width="3"/>
  <path d="M19 20l3-3m17 2 3 2m-24 14 3 3m21-6 2 4m-16-15 3 2" stroke="#fff2da" stroke-width="3" stroke-linecap="round"/>
</svg>`;

// Todas las imágenes externas tienen un sustituto. No utilizamos handlers inline;
// esto permite aplicar una CSP más estricta y evita interpretación accidental.
let imageFallbackInstalled = false;
export function installImageFallbacks() {
    if (imageFallbackInstalled || typeof document === 'undefined') { return; }
    document.addEventListener('error', (event) => {
        const target = event.target;
        if (!(target instanceof HTMLImageElement) || target.src === IMG_PLACEHOLDER || target.dataset.imageFallback === 'placeholder') { return; }
        // Si el navegador no consigue descargar desde el CDN, prueba la copia
        // servida desde nuestro propio backend (host externo fijo, sin SSRF).
        if (target.dataset.imageFallback !== 'proxy') {
            const proxy = imageProxyFor(target.currentSrc || target.src);
            if (proxy) {
                target.dataset.imageFallback = 'proxy';
                target.src = proxy;
                return;
            }
        }
        target.dataset.imageFallback = 'placeholder';
        target.src = IMG_PLACEHOLDER;
    }, true);
    imageFallbackInstalled = true;
}

export function mountChrome(active) {
    installImageFallbacks();
    const header = document.querySelector('[data-chrome="topbar"]');
    if (header) {
        header.innerHTML = `<div class="topbar-inner">
          <a class="brand" href="/index.html" aria-label="Springfield Archive, inicio">${ICON}
            <span class="brand-lines"><strong>SPRINGFIELD</strong><em>ARCHIVE / 1.1.1</em></span></a>
          <button type="button" class="menu-button" id="menu-toggle" aria-controls="primary-nav" aria-expanded="false">☰ Menú</button>
          <nav class="nav" id="primary-nav" aria-label="Navegación principal">
            ${NAV.map(i => `<a href="${i.href}" ${i.key === active ? 'aria-current="page"' : ''}>${i.label}</a>`).join('')}
          </nav>
          <div class="header-extras"><button class="header-donut" id="donut-surprise" type="button" aria-label="Descubrir una dona misteriosa" title="No pulsar (dice el Sr. Burns)">🍩</button>
          <button class="sound-toggle" id="sound-toggle" type="button" aria-pressed="false">♪ Sonido: no</button></div>
          <a class="nav-highlight" href="/game.html" ${active === 'game' ? 'aria-current="page"' : ''}>◈ Springfield Files</a>
        </div>`;
        const menu = header.querySelector('#menu-toggle');
        const nav = header.querySelector('#primary-nav');
        menu.addEventListener('click', () => {
            const open = menu.getAttribute('aria-expanded') !== 'true';
            menu.setAttribute('aria-expanded', String(open));
            nav.classList.toggle('is-open', open);
        });
        const closeMenu = (restoreFocus = false) => {
            nav.classList.remove('is-open'); menu.setAttribute('aria-expanded', 'false');
            if (restoreFocus) { menu.focus(); }
        };
        nav.addEventListener('click', (event) => {
            if (event.target.closest('a')) { closeMenu(); }
        });
        document.addEventListener('click', (event) => {
            if (!header.contains(event.target)) { closeMenu(); }
        });
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && nav.classList.contains('is-open')) { closeMenu(true); }
        });
        window.addEventListener('resize', () => {
            if (window.innerWidth > 870 && nav.classList.contains('is-open')) { closeMenu(); }
        });
    }
    const footer = document.querySelector('[data-chrome="credits"]');
    if (footer) {
        footer.innerHTML = `<div class="credits-inner">
          <div><strong>SPRINGFIELD ARCHIVE</strong><p>Una pequeña enciclopedia con demasiado papeleo y suficiente café.</p>
            <p>Creado por <a href="/sobre-mi.html">José Manuel Valencia Gallego</a> · 2026</p></div>
          <div><p><b>FUENTES Y RESPONSABILIDAD</b></p><p>Información: <a href="https://thesimpsonsapi.com/" target="_blank" rel="noopener noreferrer">The Simpsons API</a> y <a href="https://simpsons.fandom.com/wiki/Simpsons_Wiki" target="_blank" rel="noopener noreferrer">The Simpsons Wiki</a> (CC BY-SA 3.0 según el origen de los datos). Consulta <a href="/creditos.html">fuentes y créditos</a>.</p>
            <p class="disclaimer">Proyecto académico independiente, no afiliado a los titulares de Los Simpson. Los personajes, imágenes y marcas pertenecen a sus titulares respectivos.</p></div>
        </div>`;
    }
    initSound();
    const donut=header?.querySelector('#donut-surprise');
    donut?.addEventListener('click',()=>{
        emitSound('donut');donut.classList.remove('is-celebrating');
        void donut.offsetWidth;donut.classList.add('is-celebrating');
        const toast=document.getElementById('springfield-toast')||document.createElement('div');
        toast.id='springfield-toast';toast.className='springfield-toast';toast.setAttribute('role','status');
        try { localStorage.setItem('springfield:donut-clue:v2','yes'); } catch {}
        document.dispatchEvent(new CustomEvent('springfield:donut-found'));
        toast.textContent='🍩 ¡Pase de inspección! Encontraste una ayuda para Springfield Files.';
        document.body.append(toast);
        clearTimeout(donut._toastTimer);
        donut._toastTimer=setTimeout(()=>{toast.remove();donut.classList.remove('is-celebrating');},2900);
    });
    initMotion();
}

export function escapeHtml(text) {
    if (text === null || text === undefined) { return ''; }
    return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}
export const escapeAttr = escapeHtml;

export const IMG_PLACEHOLDER = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 275"><rect width="220" height="275" fill="#e6ddc9"/><circle cx="110" cy="119" r="65" fill="#f6d44a" stroke="#18262e" stroke-width="5"/><text x="110" y="127" text-anchor="middle" font-family="Arial Black" font-size="38" fill="#18262e">SA</text><text x="110" y="225" text-anchor="middle" font-family="Arial" font-size="13" fill="#18262e">IMAGEN NO DISPONIBLE</text></svg>`);
const SAFE_IMAGE_PATH = /^\/(character|episode|location|short)\/[A-Za-z0-9_-]+\.(webp|png|jpe?g)$/i;
export function sanitizeImagePath(path) {
    if (typeof path !== 'string') { return null; }
    const trimmed = path.trim();
    return SAFE_IMAGE_PATH.test(trimmed) ? trimmed : null;
}
const IMAGE_BASE = 'https://cdn.thesimpsonsapi.com/500';
const SAFE_MEDIA_IMAGE = /^https:\/\/cdn\.thesimpsonsapi\.com\/(?:200|500|1280)\/(character|episode|location|short)\/(\d+)\.webp$/i;
export function imageProxyFor(url) {
    if (typeof url !== 'string') { return null; }
    const match = SAFE_MEDIA_IMAGE.exec(url);
    if (!match || !Number.isSafeInteger(Number(match[2])) || Number(match[2]) < 1 || Number(match[2]) > 100000) { return null; }
    return `${backendOrigin || ""}/api/v1/media/${match[1].toLowerCase()}/${match[2]}`;
}
export function imageFor(path) {
    const safePath = sanitizeImagePath(path);
    return safePath ? IMAGE_BASE + safePath : IMG_PLACEHOLDER;
}
// El ID externo proviene de nuestra API Java, no del slug editorial.
// Si el proveedor omitió portrait_path pero sí entregó un ID, usamos su
// convención publicada para intentar cargar el retrato; una ausencia real
// termina en el placeholder local, nunca en una imagen inventada.
export function imageForItem(item, kind) {
    const rawPath = kind === 'character' ? item?.portraitPath : item?.imagePath;
    if (sanitizeImagePath(rawPath)) { return imageFor(rawPath); }
    const id = item?.externalId;
    const types = {character:'character',episode:'episode',location:'location'};
    const resource = types[kind];
    if (resource && Number.isSafeInteger(id) && id > 0 && id <= 100000) {
        return imageFor(`/${resource}/${id}.webp`);
    }
    return IMG_PLACEHOLDER;
}
// Esqueletos de carga: representan espera, nunca registros inventados.
export function loadingCards(number = 4) {
    const total = Math.min(12, Math.max(1, Number(number) || 4));
    return `<div class="cards skeleton-grid" aria-hidden="true">${Array.from({length: total}, () =>
        '<div class="card skeleton-card"><div class="skeleton-media"></div><div class="skeleton-lines"><i></i><i></i></div></div>').join('')}</div>
        <p class="sr-only" role="status">Cargando expedientes desde la API Java…</p>`;
}

export function safeCount(value) {
    const n = Number(value);
    return Number.isFinite(n) && n >= 0 ? Math.floor(n) : 0;
}
