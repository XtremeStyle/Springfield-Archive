import {shortMetaEs} from './spanish-content.js';
import {episodeLabel} from './catalog-utils.js';
// Springfield Archive — Página de inicio: estadísticas + destacados
import { fetchJson } from './api.js';
import { imageForItem, escapeHtml, escapeAttr, safeCount, mountChrome } from './components.js';
import {isFavorite,toggleFavorite} from './favorites.js';
import {nextDiscovery,randomPage} from './home-discovery.js';

mountChrome('inicio');
async function renderStats() {
    const target = document.getElementById('stats');
    try {
        const data = await fetchJson('/stats');
        document.getElementById('source-status').textContent = '● Datos almacenados y servidos por Java';
        target.innerHTML = `
            <div class="stat"><span class="stat-num">${safeCount(data.characters.total)}</span><span class="stat-label">Personajes</span></div>
            <div class="stat"><span class="stat-num">${safeCount(data.episodes.total)}</span><span class="stat-label">Episodios</span></div>
            <div class="stat"><span class="stat-num">${safeCount(data.locations.total)}</span><span class="stat-label">Lugares</span></div>
            <div class="stat"><span class="stat-num">${safeCount(data.episodes.seasons)}</span><span class="stat-label">Temporadas</span></div>`;
    } catch (err) {
        document.getElementById('source-status').textContent = '● Sin conexión con el backend';
        target.innerHTML = `<p class="state error">No se pudieron cargar las estadísticas: ${escapeHtml(err.message)}</p>`;
    }
}

// Exploración con acciones reales: abrir ficha, guardar favorito y solicitar otra recomendación.
const discoveryConfig = {
  character:{endpoint:'/characters',page:'personaje',label:'Personaje',all:'/personajes.html',source:'curated'},
  episode:{endpoint:'/episodes',page:'episodio',label:'Episodio',all:'/episodios.html'},
  location:{endpoint:'/locations',page:'lugar',label:'Lugar',all:'/lugares.html',source:'curated'},
};
const discoveries=Object.fromEntries(Object.keys(discoveryConfig).map(kind=>[kind,{rows:[],seen:new Set(),pages:1,loading:false}]));
function renderDiscovery(kind,item){
  const target=document.getElementById(`discovery-${kind}`),config=discoveryConfig[kind];
  if(!target)return;
  if(!item){target.innerHTML=`<p class="discovery-error">Sin fichas disponibles. Comprueba la sincronización y vuelve a intentarlo.</p><a class="discovery-browse" href="${config.all}">Consultar catálogo ↗</a>`;return;}
  const href=`/${config.page}.html?slug=${encodeURIComponent(item.slug)}`;
  const meta=kind==='episode'?episodeLabel(item):shortMetaEs(item,kind);
  const saved=isFavorite(kind,item.slug);
  target.innerHTML=`<div class="discovery-media"><img src="${escapeAttr(imageForItem(item,kind))}" alt="Imagen de ${escapeAttr(item.name)}" loading="lazy" decoding="async"></div>
   <div class="discovery-info"><span class="discovery-source">${kind==='episode'?`Temporada ${Number.isInteger(item.season)?item.season:'—'}`:'Registro consultable'} · THE SIMPSONS API</span><h3>${escapeHtml(item.name)}</h3><p>${escapeHtml(meta||'Información disponible en su ficha')}</p></div>
   <div class="discovery-actions"><a href="${escapeAttr(href)}" class="discovery-open">Abrir ficha ↗</a><button type="button" data-favorite="${escapeAttr(item.slug)}" aria-pressed="${saved}" aria-label="${saved?'Quitar de favoritos':'Guardar en favoritos'}: ${escapeAttr(item.name)}">${saved?'♥ Guardado':'♡ Guardar'}</button></div>`;
  target.querySelector('[data-favorite]')?.addEventListener('click',event=>{
    const next=toggleFavorite(kind,item.slug);
    event.currentTarget.setAttribute('aria-pressed',String(next));
    event.currentTarget.setAttribute('aria-label',`${next?'Quitar de favoritos':'Guardar en favoritos'}: ${item.name}`);
    event.currentTarget.textContent=next?'♥ Guardado':'♡ Guardar';
  });
}
async function nextFeatured(kind){
  const config=discoveryConfig[kind],state=discoveries[kind],target=document.getElementById(`discovery-${kind}`),button=document.querySelector(`[data-next="${kind}"]`);
  if(!target||state.loading)return;
  state.loading=true;button.disabled=true;button.setAttribute('aria-busy','true');
  try{
    let entry=nextDiscovery(state.rows,state.seen);
    if(!entry){
      target.innerHTML='<p class="discovery-loading">Consultando otro expediente auténtico…</p>';
      const query={size:20,page:0,...(config.source?{source:config.source}:{})};
      if(!state.rows.length){
        const first=await fetchJson(config.endpoint,query);
        if(!Array.isArray(first.content))throw new Error('Contrato de catálogo inválido');
        state.pages=Number.isSafeInteger(first.totalPages)?first.totalPages:1;
        state.rows=first.content;
      }
      const page=randomPage(state.pages);
      if(page>0){
        const result=await fetchJson(config.endpoint,{...query,page});
        if(Array.isArray(result.content))state.rows.push(...result.content);
      }
      entry=nextDiscovery(state.rows,state.seen);
      if(!entry && state.pages>1){
        // Recuperar otro bloque sin repetir la lista inicial; evita consultar siempre la primera página.
        const result=await fetchJson(config.endpoint,{...query,page:randomPage(state.pages)});
        if(Array.isArray(result.content))state.rows.push(...result.content);
        entry=nextDiscovery(state.rows,state.seen);
      }
      if(!entry && state.rows.length){state.seen.clear();entry=nextDiscovery(state.rows,state.seen);}
    }
    if(entry)state.seen.add(entry.slug);
    renderDiscovery(kind,entry);
  }catch(error){
    target.innerHTML=`<p class="discovery-error">No se pudo consultar esta sección: ${escapeHtml(error.message)}</p><a class="discovery-browse" href="${config.all}">Abrir catálogo ↗</a>`;
  }finally{state.loading=false;button.disabled=false;button.removeAttribute('aria-busy');}
}
document.querySelectorAll('[data-next]').forEach(button=>button.addEventListener('click',()=>void nextFeatured(button.dataset.next)));
renderStats();
Object.keys(discoveryConfig).forEach(kind=>void nextFeatured(kind));


// Estado público de sincronización: evidencia verificable de la integración externa.
async function renderSyncStatus() {
    const target = document.getElementById('sync-states');
    if (!target) { return; }
    const LABELS = { characters: 'Personajes', episodes: 'Episodios', locations: 'Lugares' };
    const STATES = { completed: 'Sincronizado', running: 'En proceso', failed: 'Requiere revisión', idle: 'Pendiente' };
    try {
        const rows = await fetchJson('/sync/status');
        if (!Array.isArray(rows)) { throw new Error('Respuesta de estado inválida'); }
        const indexed = new Map(rows.filter(r => r && LABELS[r.resource]).map(r => [r.resource, r]));
        target.innerHTML = Object.entries(LABELS).map(([key, label]) => {
            const value = indexed.get(key);
            const status = STATES[value?.status] || 'Pendiente';
            const time = value?.syncedAt ? new Date(value.syncedAt) : null;
            const date = time && !Number.isNaN(time.getTime()) ? time.toLocaleDateString('es-CO', {year:'numeric',month:'short',day:'numeric'}) : 'Sin importación completa';
            return `<div class="sync-state"><span>${escapeHtml(label)}</span><strong data-state="${escapeAttr(value?.status || 'idle')}">${escapeHtml(status)}</strong><small>${escapeHtml(date)}</small></div>`;
        }).join('');
    } catch (error) {
        target.innerHTML = `<p class="state error">No se pudo consultar el estado de sincronización: ${escapeHtml(error.message)}</p>`;
    }
}
renderSyncStatus();
