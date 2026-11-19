import {shortMetaEs,ocupacionEs} from './spanish-content.js';
// Favoritos reales de la aplicación: persistencia local y consulta por slug a la API Java.
import { fetchJson } from './api.js';
import {episodeLabel} from './catalog-utils.js';
import { favoriteEntries, toggleFavorite } from './favorites.js';
import { imageForItem, escapeHtml, escapeAttr, mountChrome } from './components.js';
mountChrome('favoritos');
const results = document.getElementById('results');
const count = document.getElementById('count');
let renderVersion = 0;
const endpointFor = (kind) => kind === 'character' ? '/characters' : kind === 'episode' ? '/episodes' : '/locations';
const hrefFor = (kind) => kind === 'character' ? '/personaje.html' : kind === 'episode' ? '/episodio.html' : '/lugar.html';

// Carga un lote limitado para no saturar el servidor con peticiones simultáneas.
async function loadEntries(entries) {
    const out = [];
    for (let i = 0; i < entries.length; i += 6) {
        const batch = await Promise.all(entries.slice(i, i + 6).map(async ({kind, id}) => {
            try { return { kind, item: await fetchJson(`${endpointFor(kind)}/${encodeURIComponent(id)}`) }; }
            catch (error) { return { kind, id, error }; }
        }));
        out.push(...batch);
    }
    return out;
}

async function render() {
    const version = ++renderVersion;
    const favorites = favoriteEntries();
    count.textContent = `${favorites.length} expedientes guardados`;
    if (!favorites.length) {
        results.innerHTML = '<p class="state">Todavía no tienes favoritos. Visita <a href="/personajes.html">Personajes</a>, <a href="/episodios.html">Episodios</a> o <a href="/lugares.html">Lugares</a> y guarda tus fichas.</p>';
        return;
    }
    results.innerHTML = '<p class="state">Consultando tus fichas guardadas…</p>';
    const loaded = await loadEntries(favorites);
    if (version !== renderVersion) { return; }
    const good = loaded.filter(entry => !entry.error);
    const missing = loaded.filter(entry => entry.error);
    if (!good.length) {
        results.innerHTML = `<p class="state error">No se pudieron consultar tus favoritos. El catálogo podría no estar disponible. Tus selecciones siguen guardadas en este navegador. <button class="btn" type="button" id="retry-favorites">Reintentar</button></p>`;
        document.getElementById('retry-favorites').addEventListener('click', render, {once:true});
        return;
    }
    results.innerHTML = `${missing.length ? `<p class="state">No se pudieron consultar ${missing.length} ficha(s). Permanecen guardadas y podrás reintentarlo al volver a entrar.</p>` : ''}<div class="cards">${good.map(({kind, item}) => card(kind, item)).join('')}</div>`;
}

function card(kind, item) {
    const href = `${hrefFor(kind)}?slug=${encodeURIComponent(item.slug)}`;
    const sub=kind==='episode'?episodeLabel(item):shortMetaEs(item,kind);
    return `<article class="card">
      <a class="card-link" href="${escapeAttr(href)}"><span class="card-media"><img src="${escapeAttr(imageForItem(item, kind))}" alt="Imagen de ${escapeAttr(item.name)}" loading="lazy"></span>
      <span class="card-body"><span class="card-title">${escapeHtml(item.name)}</span><span class="card-sub">${escapeHtml(sub)}</span></span></a>
      <div class="card-actions"><button type="button" class="fav-btn" data-fav-kind="${escapeAttr(kind)}" data-fav-id="${escapeAttr(item.slug)}" aria-pressed="true" aria-label="Quitar de favoritos ${escapeAttr(item.name)}">♥ En favoritos</button></div>
    </article>`;
}
// Delegación registrada una sola vez (antes se añadía un listener en cada render).
results.addEventListener('click', (event) => {
    const button = event.target.closest('[data-fav-kind]');
    if (!button) { return; }
    toggleFavorite(button.dataset.favKind, button.dataset.favId);
    // Actualiza el DOM en sitio: quitar una tarjeta no re-consulta toda la API.
    const cardEl = button.closest('.card');
    if (cardEl) { cardEl.remove(); }
    const left = favoriteEntries();
    count.textContent = `${left.length} expedientes guardados`;
    if (!left.length) {
        results.innerHTML = '<p class="state">Todavía no tienes favoritos. Visita <a href="/personajes.html">Personajes</a>, <a href="/episodios.html">Episodios</a> o <a href="/lugares.html">Lugares</a> y guarda tus fichas.</p>';
        return;
    }
    if (!results.querySelector('.card')) { render(); }
});
render();
