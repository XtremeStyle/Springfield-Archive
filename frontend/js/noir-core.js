/** Springfield Files — reglas puras del juego.
 * Las pistas y el código provienen SOLAMENTE de registros de nuestra API Java.
 * No se inventan relaciones entre personaje, lugar y episodio: la trama es ficción.
 */
import {ocupacionEs,estadoEs,usoEs} from './spanish-content.js';
import {readableDate} from './catalog-utils.js';
const SLUG=/^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const ROUTES={character:'personaje',location:'lugar',episode:'episodio'};
const TYPES={character:'PERSONA',location:'LUGAR',episode:'EPISODIO'};
export const CASE_SEQUENCE=Object.freeze(['character','location','episode']);
const normal=name=>String(name||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();
const valid=(v,kind)=>v&&typeof v.name==='string'&&v.name.trim()&&typeof v.slug==='string'&&v.slug.length<=(kind==='episode'?120:80)&&SLUG.test(v.slug)&&Number.isSafeInteger(v.externalId)&&v.externalId>0;
const signature=(r,kind)=>kind==='character'?`${ocupacionEs(r.occupation)||''}|${estadoEs(r.status)||''}`:
 kind==='location'?`${usoEs(r.use)||''}|${r.town||''}`:`${r.season||''}|${r.episodeNumber||''}`;
export function distinctRecords(rows,kind){
 const seenId=new Set(),seenName=new Set();
 return (Array.isArray(rows)?rows:[]).filter(r=>{
  if(!valid(r,kind))return false;
  const key=normal(r.name);if(!key||seenId.has(r.externalId)||seenName.has(key))return false;
  seenId.add(r.externalId);seenName.add(key);return true;
 });
}
export function shuffle(records,rng=Math.random){
 const a=[...records];for(let i=a.length-1;i>0;i--){
  const r=Number(rng());const n=Number.isFinite(r)?Math.max(0,Math.min(i,Math.floor(r*(i+1)))):0;
  [a[i],a[n]]=[a[n],a[i]];
 }return a;
}
function usable(r,kind){
 if(kind==='character')return Boolean(ocupacionEs(r.occupation)&&estadoEs(r.status));
 if(kind==='location')return Boolean(usoEs(r.use));
 return Number.isInteger(r.season)&&r.season>0&&Number.isInteger(r.episodeNumber)&&r.episodeNumber>0;
}
function hints(item,kind){
 if(kind==='character')return [
  {name:'Oficio registrado',value:ocupacionEs(item.occupation)},
  {name:'Estado',value:estadoEs(item.status)},
  ...(Number.isInteger(item.age)&&item.age>=0?[{name:'Edad publicada',value:`${item.age} años`}]:[]),
 ];
 if(kind==='location')return [
  {name:'Uso documentado',value:usoEs(item.use)},
  ...(typeof item.town==='string'&&item.town.trim()?[{name:'Localidad',value:item.town.trim()}]:[]),
 ];
 return [
  {name:'Temporada',value:String(item.season)},
  {name:'Número global en la serie',value:String(item.episodeNumber)},
  ...(readableDate(item.airdate)?[{name:'Fecha de emisión',value:readableDate(item.airdate)}]:[]),
 ];
}
function candidateMeta(item,kind){
 if(kind==='character')return [ocupacionEs(item.occupation),estadoEs(item.status),Number.isInteger(item.age)&&item.age>=0?`${item.age} años`:null].filter(Boolean).join(' · ');
 if(kind==='location')return [usoEs(item.use),item.town].filter(Boolean).join(' · ');
 return [`Temp. ${item.season??'?'}`,Number.isInteger(item.episodeNumber)?`Capítulo #${item.episodeNumber}`:'',readableDate(item.airdate)].filter(Boolean).join(' · ');
}
function caseFor(pool,kind,rng){
 const usableRecords=distinctRecords(pool,kind).filter(r=>usable(r,kind));
 const good=shuffle(usableRecords,rng);
 for(const target of good){
  // Todos los candidatos deben tener datos suficientes para una inspeccion comparable.
  const other=shuffle(usableRecords.filter(r=>r.externalId!==target.externalId && signature(r,kind)!==signature(target,kind)),rng);
  if(other.length<3)continue;
  const options=shuffle([target,...other.slice(0,3)],rng).map(r=>({
   slug:r.slug,name:r.name,externalId:r.externalId,kind,
   mediaPath:kind==='character'?r.portraitPath:r.imagePath,
   meta:candidateMeta(r,kind),
   href:`/${ROUTES[kind]}.html?slug=${encodeURIComponent(r.slug)}`,
  }));
  return {
   kind,type:TYPES[kind],id:target.externalId,answer:target.slug,
   name:target.name,hints:hints(target,kind),options,
   href:`/${ROUTES[kind]}.html?slug=${encodeURIComponent(target.slug)}`,
   codeDigit:target.externalId%10,
  };
 }
 return null;
}
export function buildMission(pools,rng=Math.random){
 const byKind={character:pools?.characters,location:pools?.locations,episode:pools?.episodes};
 const result=CASE_SEQUENCE.map(kind=>caseFor(byKind[kind],kind,rng));
 return result.every(Boolean)?result:null;
}
/** Orden único de actas: ID externo ascendente y categoría como desempate. */
export function orderedEvidence(cases){
 if(!Array.isArray(cases)||cases.length!==CASE_SEQUENCE.length||cases.some((c,i)=>c?.kind!==CASE_SEQUENCE[i]||!Number.isSafeInteger(c.id)||c.id<1))return null;
 return [...cases].sort((a,b)=>a.id-b.id||CASE_SEQUENCE.indexOf(a.kind)-CASE_SEQUENCE.indexOf(b.kind));
}
export function verifyEvidenceOrder(cases,slugs){
 const expected=orderedEvidence(cases);
 return Boolean(expected&&Array.isArray(slugs)&&slugs.length===expected.length&&expected.every((c,i)=>c.answer===slugs[i]));
}
/** Todos son registros auténticos; uno es ajeno a la investigación y está marcado solo en memoria. */
export function makeAuditPacket(cases,rng=Math.random){
 if(!orderedEvidence(cases))return null;
 const pool=[];
 for(const c of cases){
  for(const o of Array.isArray(c.options)?c.options:[]){
   if(o.slug!==c.answer && Number.isSafeInteger(o.externalId) && o.externalId>0) pool.push(o);
  }
 }
 if(!pool.length)return null;
 const index=Math.min(pool.length-1, Math.max(0, Math.floor((typeof rng==='function'?rng():Math.random())*pool.length)));
 const extra=pool[index];
 const receipts=cases.map(c=>({name:c.name,slug:c.answer,kind:c.kind,id:c.id,href:c.href}));
 receipts.push({name:extra.name,slug:extra.slug,kind:extra.kind,id:extra.externalId,href:extra.href});
 return shuffle(receipts,rng);
}
export function isIntruder(cases,slug){
 // El intruso es cualquier distractor auténtico que no es respuesta; la UI solo
 // presenta uno, así que la comprobación por opciones es equivalente y no depende del orden.
 if(typeof slug!=='string'||cases.some(c=>c.answer===slug))return false;
 return cases.some(c=>Array.isArray(c.options)&&c.options.some(o=>o.slug===slug&&Number.isSafeInteger(o.externalId)&&o.externalId>0));
}
/** Cifrado 742: sumar cada dígito del ID y agregar 7, 4 o 2; conservar unidades. */
export const CIPHER_OFFSETS=Object.freeze([7,4,2]);
export function cipherDigit(id,offset){
 if(!Number.isSafeInteger(id)||id<1||!Number.isInteger(offset)||offset<0||offset>9)return null;
 return (String(id).split('').reduce((sum,n)=>sum+Number(n),0)+offset)%10;
}
export function missionCode(cases){
 const ordered=orderedEvidence(cases);
 return ordered?ordered.map((c,i)=>String(cipherDigit(c.id,CIPHER_OFFSETS[i]))).join(''):null;
}
export function validateCode(cases,value){return /^\d{3}$/.test(String(value))&&missionCode(cases)===String(value);}
export function evaluateChoice(caseItem,slug){return Boolean(caseItem?.options?.some(o=>o.slug===slug)&&caseItem.answer===slug);}
