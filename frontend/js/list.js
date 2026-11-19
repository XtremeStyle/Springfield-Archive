/** Catalogo publico: filtros reales y paginados por SQL, no por tarjetas ya cargadas. */
import {fetchJson} from './api.js';
import {imageForItem,escapeHtml,escapeAttr,safeCount,loadingCards,mountChrome} from './components.js';
import {isFavorite,toggleFavorite} from './favorites.js';
import {PAGE_SIZE,DEFAULTS,catalogState,backendSearch,episodeLabel,readableDate,excerpt,resultWindow} from './catalog-utils.js';
import {shortMetaEs,descriptionEs} from './spanish-content.js';

const SAFE_SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const ENDPOINTS = {character:'/characters',episode:'/episodes',location:'/locations'};
const DETAILS = {character:'/personaje.html',episode:'/episodio.html',location:'/lugar.html'};

function cardHTML(item, kind) {
    const maxSlug=kind==='episode'?120:80;
    const id=typeof item.slug==='string'&&item.slug.length<=maxSlug&&SAFE_SLUG.test(item.slug)?item.slug:'';
    const name=item.name||'Sin nombre';
    const favOn=Boolean(id)&&isFavorite(kind,id);
    const href=DETAILS[kind]+'?slug='+encodeURIComponent(id);
    const label=kind==='episode' ? `TEMPORADA ${item.season ?? '—'}`
        : kind==='character' ? 'PERSONAJE' : 'LUGAR';
    const subtitle=kind==='episode' ? episodeLabel(item) : shortMetaEs(item,kind);
    const extra=kind==='episode' ? '' : descriptionEs(item,kind).text;
    const sourceTag=kind==='episode'||Number.isInteger(item.externalId)?'API':'ARCHIVO EDITORIAL';
    const date=kind==='episode'?readableDate(item.airdate):'';
    return `<article class="card catalog-card" data-catalog-kind="${kind}">
      ${id?`<a class="card-link" href="${escapeAttr(href)}">`:'<div class="card-link">'}
        <span class="card-media"><img src="${escapeAttr(imageForItem(item,kind))}" alt="Ilustración de ${escapeAttr(name)}" loading="lazy" decoding="async"></span>
        <span class="card-body"><span class="catalog-card-label">${escapeHtml(label)}</span>
        <span class="card-title">${escapeHtml(name)}</span>${subtitle?`<span class="card-sub">${escapeHtml(subtitle)}</span>`:""}
        ${date?`<time class="catalog-card-date" datetime="${escapeAttr(item.airdate)}">${escapeHtml(date)}</time>`:''}
        ${extra?`<span class="catalog-card-excerpt">${escapeHtml(excerpt(extra,112))}</span>`:''}
        <span class="catalog-card-more">${sourceTag === "API" ? "Ficha verificada" : "Archivo original"} · Abrir <span aria-hidden="true">↗</span></span></span>
      ${id?'</a>':'</div>'}
      <div class="card-actions"><button class="fav-btn" type="button" ${id?'':'disabled'} data-fav-kind="${kind}" data-fav-id="${escapeAttr(id)}"
          aria-pressed="${favOn}" aria-label="${favOn?'Quitar de':'Añadir a'} favoritos ${escapeAttr(name)}">${favOn?'♥ Favorito':'♡ Guardar'}</button></div>
    </article>`;
}

function groupCardMarkup(items,kind) {
    let previous='';
    const html=[];
    for(const item of items) {
        let group='';
        // La agrupación solo se usa para episodios (por temporada); el resto se pinta plano.
        if(kind==='episode') group=item.season!=null?`Temporada ${item.season}`:'Temporada no registrada';
        // Agrupar solo cuando los datos siguen un orden que lo justifica.
        if(group && group!==previous) {
            html.push(`<h3 class="catalog-group-label">${escapeHtml(group)}</h3>`);
            previous=group;
        }
        html.push(cardHTML(item,kind));
    }
    return html.join('');
}

export function initList({endpoint,kind,emptyText}) {
    mountChrome(kind==='character'?'personajes':kind==='episode'?'episodios':'lugares');
    const results=document.getElementById('results');
    const pager=document.getElementById('pager');
    const count=document.getElementById('count');
    const search=document.getElementById('search');
    const source=document.getElementById('source');
    const type=document.getElementById('type');
    const season=document.getElementById('season');
    const scope=document.getElementById('scope');
    const order=document.getElementById('order');
    const notice=document.getElementById('filter-notice');
    const form=document.getElementById('catalog-filter-form');
    let state=catalogState(kind,new URLSearchParams(location.search));
    let activeRequest=null;
    let searchTimer;

    function paintControls() {
        search.value=state.q;
        if(source)source.value=state.source;
        if(type)type.value=state.type;
        if(scope)scope.value=state.scope;
        if(order)order.value=state.order;
        if(season)season.value=state.season||'';
    }
    function writeLocation(mode='replace') {
        const url=new URL(location.href);
        const defaults=DEFAULTS[kind]||{};
        for(const key of ['q','page','source','scope','order','season','type'])url.searchParams.delete(key);
        if(state.q)url.searchParams.set('q',state.q);
        if(state.page>0)url.searchParams.set('page',String(state.page));
        for(const key of ['source','scope','order','season','type']){
            if(state[key] && state[key]!==defaults[key])url.searchParams.set(key,String(state[key]));
        }
        history[mode==='push'?'pushState':'replaceState'](null,'',url);
    }
    function refreshNotice(alias=null) {
        if(!notice)return;
        const message=[];
        if(kind==='character') {
            message.push(sourceNotice(state.source));
        } else if(kind==='location') {
            message.push(sourceNotice(state.source));
            if(state.type!=='all')message.push('Clasificación interna activada; no procede de la API original.');
        } else if(kind==='episode') {
            message.push(state.season?`Temporada ${state.season} seleccionada.`:'Todas las temporadas disponibles.');
            message.push('El número que proporciona la API es global, no el número dentro de la temporada.');
        }
        if(alias)message.push(`Alias de búsqueda: ${alias}.`);
        notice.textContent=message.join(' ');
    }
    function sourceNotice(value) {
        return {curated:'Colección principal: solo registros importados de The Simpsons API, sin duplicados por nombre.',api:'Datos del proveedor, agrupados por nombre para facilitar la consulta.',editorial:'Archivo histórico separado: estas fichas NO provienen de The Simpsons API.',all:'Vista técnica: registros sin agrupar, incluye duplicados y fuentes diferentes.'}[value]||'';
    }
    function renderPager(data) {
        const current=safeCount(data.page),total=safeCount(data.totalPages);
        if(total<=1){pager.replaceChildren();return;}
        const items=[];
        const add=(label,page,disabled=false,active=false)=>{
            items.push(`<button type="button" data-page="${page}" ${disabled?'disabled':''} ${active?'aria-current="page"':''}
                 aria-label="${active?'Página actual:':'Ir a página'} ${page+1}">${label}</button>`);
        };
        add('Anterior',current-1,current<=0);
        if(current>2) add('1',0);
        for(let i=Math.max(0,current-2);i<=Math.min(total-1,current+2);i++)add(String(i+1),i,false,i===current);
        if(current+2<total-1 && total-1<=100)add(String(total),total-1);
        add('Siguiente',current+1,current>=total-1||current>=100);
        pager.innerHTML=items.join('');
    }
    async function load(){
        activeRequest?.abort();
        const request=new AbortController();activeRequest=request;
        count.textContent='Consultando registros…';
        results.setAttribute('aria-busy','true');
        results.innerHTML=loadingCards(8);
        pager.replaceChildren();
        const {query,alias}=backendSearch(state.q,kind);
        refreshNotice(alias);
        try {
            const params={q:query||undefined,page:state.page,size:PAGE_SIZE};
            if(kind==='character')Object.assign(params,{source:state.source,scope:state.scope,order:state.order});
            if(kind==='location')Object.assign(params,{source:state.source,scope:state.scope,order:state.order,type:state.type});
            if(kind==='episode')Object.assign(params,{season:state.season||undefined,scope:state.scope,order:state.order});
            let data=await fetchJson(endpoint,params,{signal:request.signal});
            if(request.signal.aborted)return;
            if(!Array.isArray(data?.content)||!Number.isSafeInteger(data.totalPages))throw new Error('El servidor devolvió una página de catálogo inválida.');
            const windowStats=resultWindow(data);
            count.textContent=windowStats.total?`${windowStats.start}–${windowStats.end} de ${windowStats.total} registros`:'0 registros';
            if(!data.content.length){
                results.innerHTML=`<div class="state catalog-empty"><strong>${!state.q && kind!=='episode' && state.source==='curated' ? 'Esperando importación de la API.' : 'No encontramos coincidencias.'}</strong><p>${!state.q && kind!=='episode' && state.source==='curated' ? 'Este catálogo muestra únicamente datos externos verificados. Sincroniza The Simpsons API en el servidor Java para empezar.' : escapeHtml(emptyText)}</p><button type="button" class="retry-button" data-reset>Restablecer búsqueda</button></div>`;
                return;
            }
            // Los grupos A-Z dentro de CSS grid desplazaban tarjetas y creaban desorden.
            // Los episodios conservan su agrupación por temporada.
            const grouped=kind==='episode'&&state.order!=='title';
            results.innerHTML=`<div class="cards catalog-grid" data-kind="${kind}">${grouped?groupCardMarkup(data.content,kind):data.content.map(item=>cardHTML(item,kind)).join('')}</div>`;
            renderPager(data);
        }catch(error){
            if(request.signal.aborted)return;
            count.textContent='No disponible';
            results.innerHTML=`<div class="state error"><strong>No pudimos abrir el catálogo.</strong><p>${escapeHtml(error.message)}</p><button type="button" data-retry class="retry-button">Reintentar consulta</button></div>`;
        }finally{if(activeRequest===request)results.removeAttribute('aria-busy');}
    }
    function resetAll(){
        state={...catalogState(kind,new URLSearchParams())};
        paintControls();writeLocation('push');load();
    }
    function searchNow() {
        state.q=search.value.trim().slice(0,120);state.page=0;
        writeLocation('replace');load();
    }
    paintControls();
    if(kind==='episode'&&season){
        fetchJson('/episodes/seasons').then(rows=>{
            if(!Array.isArray(rows))return;
            const seasons=rows.filter(r=>Number.isInteger(r?.season)&&r.season>=1&&r.season<=100&&Number.isInteger(r?.episodes)&&r.episodes>0);
            const existing=state.season;
            season.innerHTML='<option value="">Todas las temporadas</option>'+seasons.map(r=>
                `<option value="${r.season}">Temporada ${r.season} · ${r.episodes} episodios</option>`).join('');
            if(existing&&!seasons.some(r=>String(r.season)===existing)){
                season.innerHTML+=`<option value="${existing}">Temporada ${existing} · sin resultados</option>`;
            }
            season.value=existing||'';
        }).catch(()=>{/* El filtro sigue funcionando si el índice de temporadas no carga. */});
    }
    search.addEventListener('input',()=>{clearTimeout(searchTimer);searchTimer=setTimeout(searchNow,290);});
    form?.addEventListener('submit',event=>{event.preventDefault();clearTimeout(searchTimer);searchNow();});
    for(const control of [source,scope,order,season,type]){
        control?.addEventListener('change',()=>{
            clearTimeout(searchTimer);
            state[control.id]=control.value;state.q=search.value.trim().slice(0,120);state.page=0;
            writeLocation('push');load();
        });
    }
    document.getElementById('clear-search')?.addEventListener('click',()=>{
        clearTimeout(searchTimer);search.value='';searchNow();search.focus();
    });
    document.getElementById('reset-filters')?.addEventListener('click',resetAll);
    window.addEventListener('popstate',()=>{
        clearTimeout(searchTimer);
        state=catalogState(kind,new URLSearchParams(location.search));paintControls();load();
    });
    pager.addEventListener('click',event=>{
        const button=event.target.closest('button[data-page]');
        if(!button||button.disabled)return;
        const next=Number(button.dataset.page);
        if(!Number.isInteger(next)||next<0||next>100)return;
        state.page=next;writeLocation('push');load();
        document.getElementById('results')?.scrollIntoView({behavior:window.matchMedia?.('(prefers-reduced-motion: reduce)').matches?'instant':'smooth',block:'start'});
    });
    results.addEventListener('click',event=>{
        if(event.target.closest('[data-retry]')){load();return;}
        if(event.target.closest('[data-reset]')){resetAll();return;}
        const button=event.target.closest('button[data-fav-id]');
        if(!button||button.disabled)return;
        const next=toggleFavorite(button.dataset.favKind,button.dataset.favId);
        button.setAttribute('aria-pressed',String(next));
        const title=button.closest('.card')?.querySelector('.card-title')?.textContent||'';
        button.setAttribute('aria-label',`${next?'Quitar de':'Añadir a'} favoritos ${title}`);
        button.textContent=next?'♥ Favorito':'♡ Guardar';
    });
    load();
}
