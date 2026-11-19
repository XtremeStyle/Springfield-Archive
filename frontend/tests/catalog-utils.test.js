import test from 'node:test';
import assert from 'node:assert/strict';
import {safePage,safeSeason,catalogState,backendSearch,episodeLabel,readableDate,excerpt,resultWindow} from '../js/catalog-utils.js';

test('búsqueda por alias mantiene el nombre real del proveedor',()=>{
  assert.deepEqual(backendSearch('Homero','character'),{query:'Homer',alias:'Homero → Homer'});
  assert.deepEqual(backendSearch('Bart','character'),{query:'Bart',alias:null});
  assert.deepEqual(backendSearch('Homero','episode'),{query:'Homero',alias:null});
});
test('parametros de URL inválidos no se reflejan en consultas ni ordenaciones',()=>{
  const s=catalogState('character',new URLSearchParams('page=999&source=bogus&scope=drop&order=NO&q=Homer'));
  assert.equal(s.page,0);assert.equal(s.source,'curated');assert.equal(s.scope,'name');assert.equal(s.order,'relevance');
  assert.equal(s.q,'Homer');assert.equal(safePage('-1'),0);assert.equal(safePage('101'),0);
  assert.equal(safeSeason('1'), '1');assert.equal(safeSeason('999'),'');
});
test('episodios identificados por número GLOBAL de serie, nunca ordinal de temporada',()=>{
  const e={season:2,episodeNumber:14};
  assert.match(episodeLabel(e),/T2/);
  assert.match(episodeLabel(e),/Capítulo #14 de la serie/);
  assert.ok(!episodeLabel(e).includes('E14'));
});
test('fecha y resumen evitan datos inválidos y texto excesivo',()=>{
  assert.equal(readableDate('1990-02-30'),'');
  assert.match(readableDate('1990-10-11'),/1990/);
  assert.equal(readableDate(''),'');
  assert.ok(excerpt('Palabra '.repeat(100)).length<=130);
  assert.deepEqual(resultWindow({page:1,size:24,content:[1,2,3],totalElements:50}),{start:25,end:27,total:50});
});

test('lugares ofrece vista consolidada, filtros reales y aliases explícitos',()=>{
 const bad=catalogState('location',new URLSearchParams('source=malicioso&type=drop&order=sql&scope=full'));
 assert.equal(bad.source,'curated');assert.equal(bad.type,'all');assert.equal(bad.order,'relevance');assert.equal(bad.scope,'name');
 const good=catalogState('location',new URLSearchParams('source=all&type=school&order=za&scope=all&page=3'));
 assert.deepEqual([good.source,good.type,good.order,good.scope,good.page],['all','school','za','all',3]);
 assert.equal(backendSearch('Taberna de Moe','location').query,'Moe');
 assert.equal(backendSearch('Escuela primaria','location').query,'Springfield Elementary');
});
