/** Fichas verificables: nunca se mezclan los textos editoriales y el proveedor. */
import {fetchJson} from './api.js';
import {imageForItem, escapeHtml, escapeAttr, mountChrome} from './components.js';
import {isFavorite,toggleFavorite} from './favorites.js';
import {recordResearch} from './research-log.js';
import {descriptionEs,shortMetaEs,estadoEs,generoEs,ocupacionEs,usoEs,originalText,verifiedFacts} from './spanish-content.js';
import {episodeLabel,readableDate} from './catalog-utils.js';

const KIND=document.body.dataset.kind || (location.pathname.includes('personaje')?'character':location.pathname.includes('episodio')?'episode':'location');
const CATEGORY={character:'personajes',episode:'episodios',location:'lugares'};
const ROUTE={character:'personaje',episode:'episodio',location:'lugar'};
const ENDPOINT={character:'/characters',episode:'/episodes',location:'/locations'}[KIND];
const MAX=KIND==='episode'?120:80;
const slug=new URLSearchParams(location.search).get('slug')||'';
const root=document.getElementById('ficha');
mountChrome(CATEGORY[KIND]);
if(!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(slug)||slug.length>MAX){
 root.innerHTML='<div class="state error">La dirección no contiene un identificador válido. <a href="/'+CATEGORY[KIND]+'.html">Volver al catálogo</a></div>';
}else{void load();}

async function load(){
 root.setAttribute('aria-busy','true');
 try{
  const item=await fetchJson(`${ENDPOINT}/${encodeURIComponent(slug)}`);
  render(item);
 }catch(err){
  root.innerHTML=`<section class="state error"><h2>Este expediente no se pudo abrir</h2><p>${escapeHtml(err?.message||'Error de lectura')}</p><button type="button" id="retry-ficha" class="retry-button">Reintentar</button><a href="/${CATEGORY[KIND]}.html">Volver al catálogo</a></section>`;
  root.querySelector('#retry-ficha')?.addEventListener('click',()=>void load(),{once:true});
 }finally{root.removeAttribute('aria-busy');}
}
function render(item){
 const imported=Number.isInteger(item.externalId)&&item.externalId>0;
 const kindLabel={character:'Personaje',episode:'Episodio',location:'Lugar'}[KIND];
 const description=descriptionEs(item,KIND);
 const facts=verifiedFacts(item,KIND).map(([name,value])=>[name, name==='Emisión'?readableDate(value)||value:value]);
 if(KIND==='character'&&Number.isInteger(item.age)&&item.age>=0)facts.splice(0,0,['Edad registrada',String(item.age)]);
 const original=originalText(item,KIND);
 const originalFields=[];
 if(original)originalFields.push(['Descripción del proveedor',original]);
 if(KIND==='character' && item.occupation && !ocupacionEs(item.occupation))originalFields.push(['Ocupación sin traducción verificada',item.occupation]);
 if(KIND==='location' && item.use && !usoEs(item.use))originalFields.push(['Uso sin traducción verificada',item.use]);
 const link=`https://thesimpsonsapi.com/api/${CATEGORY[KIND]}/${item.externalId}`;
 const validSourceUrl=imported?link:null;
 root.innerHTML=`<section class="ficha final-ficha" data-kind="${KIND}">
   <div class="final-ficha-cover">
     <div class="final-ficha-cover-top"><span>ARCHIVO DE SPRINGFIELD</span><span>${escapeHtml(kindLabel.toUpperCase())}</span></div>
     <img src="${escapeAttr(imageForItem(item,KIND))}" alt="Retrato o imagen de ${escapeAttr(item.name)}" decoding="async">
     <div class="final-ficha-cover-bottom">${imported?'ID externo '+item.externalId:'EDICIÓN HISTÓRICA'} · Ficha verificable</div>
   </div>
   <div class="final-ficha-info">
     <a class="final-back-link" href="/${CATEGORY[KIND]}.html">← Volver a ${CATEGORY[KIND]}</a>
     <span class="eyebrow">${imported?'THE SIMPSONS API / REGISTRO IMPORTADO':'ARCHIVO EDITORIAL / NO IMPORTADO'}</span>
     <h1>${escapeHtml(item.name)}</h1>
     ${KIND==='episode'?`<p class="final-lede">${escapeHtml(episodeLabel(item))}</p>`:shortMetaEs(item,KIND)?`<p class="final-lede">${escapeHtml(shortMetaEs(item,KIND))}</p>`:''}
     ${description.text?`<section class="final-bio"><h2>Contexto en español</h2><p>${escapeHtml(description.text)}</p><small>${escapeHtml(description.source)}</small></section>`:''}
     ${facts.length?`<section class="final-facts"><h2>Datos que sí constan</h2><dl>${facts.map(([label,value])=>`<dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd>`).join('')}</dl></section>`:'<p class="final-no-data">La API no proporciona más campos estructurados para esta ficha.</p>'}
     ${KIND==='episode'?`<p class="final-original-note">El número de capítulo es el <strong>número global de la serie</strong>, no el número dentro de su temporada.</p>`:''}
     <div class="final-ficha-actions"><button id="fav" type="button" class="fav-btn" aria-pressed="${isFavorite(KIND,item.slug)}">${isFavorite(KIND,item.slug)?'♥ Quitar de favoritos':'♡ Guardar en favoritos'}</button>
      <button id="copy-link" type="button" class="final-share">Compartir ficha</button></div>
     <p id="share-feedback" class="final-feedback" role="status"></p>
     ${originalFields.length?`<details class="original-language"><summary>Leer texto original de la fuente (inglés)</summary><p class="final-original-note">Este texto no se ha traducido ni reescrito. Se conserva para verificar su contenido.</p><dl>${originalFields.map(([k,v])=>`<dt>${escapeHtml(k)}</dt><dd lang="en">${escapeHtml(v)}</dd>`).join('')}</dl></details>`:''}
     ${validSourceUrl?`<p class="final-provenance">Fuente original: <a href="${escapeAttr(validSourceUrl)}" target="_blank" rel="noopener noreferrer">The Simpsons API · ${escapeHtml(kindLabel)} ${item.externalId} ↗</a></p>`:'<p class="final-provenance">Registro editorial independiente. No debe confundirse con un registro externo.</p>'}
     ${Number.isInteger(item.firstAppearanceEpId)&&item.firstAppearanceEpId>0&&KIND!=='episode'?'<div class="final-related" id="first-appearance"><strong>Primera aparición referenciada por la API</strong><span>Buscando el episodio correspondiente…</span></div>':''}
     ${KIND==='episode'?'<div class="watch-official"><h2>Ver el episodio</h2><p>Esta API es una fuente de metadatos: no distribuye capítulos completos.</p><a href="https://www.disneyplus.com/" target="_blank" rel="noopener noreferrer">Consultar disponibilidad oficial en Disney+ ↗</a></div>':''}
   </div></section>`;
 root.querySelector('#fav')?.addEventListener('click',ev=>{
  const current=toggleFavorite(KIND,item.slug);ev.currentTarget.setAttribute('aria-pressed',String(current));
  ev.currentTarget.textContent=current?'♥ Quitar de favoritos':'♡ Guardar en favoritos';
 });
 root.querySelector('#copy-link')?.addEventListener('click',async()=>{
  const feedback=root.querySelector('#share-feedback');
  try {await navigator.clipboard.writeText(location.href);feedback.textContent='Enlace copiado.';}
  catch {feedback.textContent='Copia la dirección de esta página desde tu navegador.';}
 });
 try{recordResearch(localStorage,{kind:KIND,slug:item.slug,name:item.name});}catch{}
 if(KIND!=='episode'&&Number.isInteger(item.firstAppearanceEpId)&&item.firstAppearanceEpId>0)void linkFirstAppearance(item.firstAppearanceEpId);
}
async function linkFirstAppearance(externalId){
 const target=root.querySelector('#first-appearance');if(!target)return;
 try{
  const episode=await fetchJson(`/episodes/external/${externalId}`);
  if(!/^[a-z0-9]+(-[a-z0-9]+)*$/.test(episode.slug))throw Error('Sin enlace seguro');
  target.innerHTML=`<strong>Primera aparición mencionada en la API</strong><a href="/episodio.html?slug=${encodeURIComponent(episode.slug)}">${escapeHtml(episode.name)} · ${escapeHtml(episodeLabel(episode))} ↗</a>`;
 }catch{target.innerHTML='<strong>Primera aparición</strong><span>La API indica una referencia, pero no hay episodio enlazable en el catálogo local.</span>';}
}
