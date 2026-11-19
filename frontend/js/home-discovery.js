/** Selección de expedientes reales: evita repetir fichas durante una visita. */
export function nextDiscovery(records, visited, random=Math.random){
  const safe=Array.isArray(records)?records.filter(r=>r && typeof r.slug==='string' && /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(r.slug) && typeof r.name==='string' && r.name.trim()):[];
  const fresh=safe.filter(r=>!visited.has(r.slug));
  if(!fresh.length)return null;
  const n=Number(random());
  const index=Math.min(fresh.length-1,Math.max(0,Math.floor((Number.isFinite(n)?n:0)*fresh.length)));
  return fresh[index];
}
export function randomPage(totalPages,random=Math.random){
  if(!Number.isSafeInteger(totalPages)||totalPages<2)return 0;
  const r=Number(random());
  return Math.max(0,Math.min(totalPages-1,Math.floor((Number.isFinite(r)?r:0)*totalPages)));
}
