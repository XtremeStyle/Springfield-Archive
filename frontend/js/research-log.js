/**
 * Cuaderno de campo: registros realmente abiertos y verificados por la API propia.
 * Local, privado y recreativo. No sirve para demostrar autenticidad ante un servidor.
 */
export const RESEARCH_KEY='springfield-archive:verified-research:v1';
const MAX=60;
const SAFE_SLUG=/^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const KINDS=new Set(['character','location','episode']);
const PAGES={character:'personaje',location:'lugar',episode:'episodio'};
function valid(row){
  return row&&KINDS.has(row.kind)&&typeof row.slug==='string'&&row.slug.length<=(row.kind==='episode'?120:80)
    &&SAFE_SLUG.test(row.slug)&&typeof row.name==='string'&&row.name.trim().length>0&&row.name.length<=160;
}
export function readResearch(storage){
  try{
    const parsed=JSON.parse(storage?.getItem(RESEARCH_KEY)||'[]');
    const seen=new Set();
    return (Array.isArray(parsed)?parsed:[]).filter(row=>{
      if(!valid(row))return false;
      const key=row.kind+':'+row.slug;
      if(seen.has(key))return false;
      seen.add(key);return true;
    }).slice(0,MAX).map(row=>({kind:row.kind,slug:row.slug,name:row.name.trim()}));
  }catch{return [];}
}
export function recordResearch(storage,entry){
  if(!valid(entry))return {saved:false,added:false,entries:readResearch(storage)};
  const previous=readResearch(storage);
  const existed=previous.some(r=>r.kind===entry.kind&&r.slug===entry.slug);
  const next=[{kind:entry.kind,slug:entry.slug,name:entry.name.trim()},
    ...previous.filter(row=>!(row.kind===entry.kind&&row.slug===entry.slug))].slice(0,MAX);
  try{
    storage?.setItem(RESEARCH_KEY,JSON.stringify(next));
    return {saved:storage?.getItem(RESEARCH_KEY)===JSON.stringify(next),added:!existed,entries:next};
  }catch{return {saved:false,added:!existed,entries:next};}
}
export function researchBonus(entries){
  const safe=Array.isArray(entries)?entries.filter(valid):[];
  const kinds=new Set(safe.map(r=>r.kind));
  return safe.length>=3&&kinds.size>=2 ? 1 : 0;
}
export function researchHref(entry){
  return valid(entry)?`/${PAGES[entry.kind]}.html?slug=${encodeURIComponent(entry.slug)}`:null;
}
