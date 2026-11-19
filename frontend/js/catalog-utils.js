/** Pure functions for catalog navigation, independent of the DOM. */
export const MAX_PAGE = 100;
export const PAGE_SIZE = 24;
export const DEFAULTS = Object.freeze({
  character: Object.freeze({source:'curated',scope:'name',order:'relevance'}),
  episode: Object.freeze({scope:'title',order:'chronological',season:''}),
  location: Object.freeze({source:'curated',scope:'name',order:'relevance',type:'all'}),
});
const CHARACTER_ALIASES = new Map([
  ['homero','Homer'], ['homero simpson','Homer Simpson'],
  ['señor burns','Burns'], ['sr burns','Burns'],
  ['milhouse van houten','Milhouse'], ['flanders','Flanders'],
  ['aplicador de seguridad','Safety Inspector'],
]);
const LOCATION_ALIASES = new Map([
  ['taberna de moe','Moe'], ['bar de moe','Moe'],
  ['casa de los simpson','Simpson'], ['escuela primaria','Springfield Elementary'],
  ['planta nuclear','Nuclear Power Plant'], ['central nuclear','Nuclear Power Plant'],
]);
const ALLOWED = {
  source:['curated','api','all','editorial'], scope:['name','title','all'],
  order:['relevance','az','za','recent','chronological','title'],
  type:['all','home','business','landmark','school','park','plant'],
};
export function safeOption(key, value, fallback) {
  return ALLOWED[key]?.includes(value) ? value : fallback;
}
export function safePage(value) {
  if (value == null || !/^\d+$/.test(String(value))) return 0;
  const n=Number(value);
  return Number.isSafeInteger(n) && n>=0 && n<=MAX_PAGE ? n : 0;
}
export function safeSeason(value) {
  if(value == null || value === '')return '';
  if(!/^\d+$/.test(String(value)))return '';
  const n=Number(value);
  return Number.isSafeInteger(n) && n >= 1 && n<=100 ? String(n) : '';
}
export function catalogState(kind, params) {
  const defaults = DEFAULTS[kind] || {};
  return {
    q:(params.get('q')||'').trim().slice(0,120),
    page:safePage(params.get('page')),
    ...(kind==='character'?{
      source:safeOption('source',params.get('source'),defaults.source),
      scope:['name','all'].includes(params.get('scope')) ? params.get('scope') : defaults.scope,
      order:['relevance','az','za','recent'].includes(params.get('order')) ? params.get('order') : defaults.order,
    }:{}),
    ...(kind==='location'?{
      source:safeOption('source',params.get('source'),defaults.source),
      scope:['name','all'].includes(params.get('scope'))?params.get('scope'):defaults.scope,
      order:['relevance','az','za','recent'].includes(params.get('order'))?params.get('order'):defaults.order,
      type:safeOption('type',params.get('type'),defaults.type),
    }:{}),
    ...(kind==='episode'?{
      season:safeSeason(params.get('season')),
      scope:['title','all'].includes(params.get('scope')) ? params.get('scope') : defaults.scope,
      order:['chronological','recent','title'].includes(params.get('order')) ? params.get('order') : defaults.order,
    }:{}),
  };
}
export function backendSearch(q, kind) {
  const text=typeof q==='string'?q.trim():'';
  if(kind!=='character'&&kind!=='location') return {query:text,alias:null};
  const alias=(kind==='location'?LOCATION_ALIASES:CHARACTER_ALIASES).get(text.toLocaleLowerCase('es'));
  return alias ? {query:alias,alias:`${text} → ${alias}`} : {query:text,alias:null};
}
export function episodeLabel(episode) {
  const season=Number.isInteger(episode?.season) && episode.season>0 ? `T${episode.season}`:'Temporada no disponible';
  // The Simpsons API: episode_number is overall series number (S2 premiere = #14).
  const global=Number.isInteger(episode?.episodeNumber) && episode.episodeNumber>0
    ? `Capítulo #${episode.episodeNumber} de la serie`: 'Número global no disponible';
  return `${season} · ${global}`;
}
export function readableDate(value) {
  if(typeof value!=='string'||!/^(\d{4})-(\d{2})-(\d{2})$/.test(value)) return '';
  const [,y,m,d]=value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  const date = new Date(`${y}-${m}-${d}T12:00:00Z`);
  if(Number.isNaN(date.getTime())||date.getUTCFullYear()!==Number(y)
    ||date.getUTCMonth()+1!==Number(m)||date.getUTCDate()!==Number(d)) return '';
  return new Intl.DateTimeFormat('es-CO',{day:'numeric',month:'short',year:'numeric',timeZone:'UTC'}).format(date);
}
export function excerpt(value, max=126) {
  if(typeof value!=='string') return '';
  const trimmed=value.replace(/\s+/g,' ').trim();
  if(trimmed.length<=max) return trimmed;
  const end=trimmed.lastIndexOf(' ',max);
  return trimmed.slice(0,end>max/2?end:max).trimEnd()+'…';
}
export function resultWindow(response){
  const total=Number(response?.totalElements)||0;
  const page=Number(response?.page)||0;
  const size=Number(response?.size)||PAGE_SIZE;
  const shown=Array.isArray(response?.content)?response.content.length:0;
  return {start:shown? page*size+1:0,end:shown?page*size+shown:0,total};
}
