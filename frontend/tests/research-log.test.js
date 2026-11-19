import test from 'node:test';
import assert from 'node:assert/strict';
import {readResearch,recordResearch,researchBonus,researchHref,RESEARCH_KEY} from '../js/research-log.js';
const memory=()=>{const m=new Map();return {getItem:k=>m.get(k)??null,setItem:(k,v)=>m.set(k,v)};};
test('guarda fichas verificadas y evita registros repetidos',()=>{
  const storage=memory();
  const item={kind:'character',slug:'homer-simpson',name:'Homer Simpson'};
  assert.equal(recordResearch(storage,item).added,true);
  assert.equal(recordResearch(storage,item).added,false);
  assert.equal(readResearch(storage).length,1);
  recordResearch(storage,{kind:'location',slug:'moes-tavern',name:'Moe'});
  recordResearch(storage,{kind:'episode',slug:'episode-1',name:'Episode 1'});
  assert.equal(researchBonus(readResearch(storage)),1);
  assert.equal(researchHref(item),'/personaje.html?slug=homer-simpson');
});
test('registros manipulados no generan beneficios ni rutas inseguras',()=>{
  const storage=memory();
  storage.setItem(RESEARCH_KEY,JSON.stringify([{kind:'character',slug:'../admin',name:'Attack'}]));
  assert.deepEqual(readResearch(storage),[]);
  assert.equal(recordResearch(storage,{kind:'unknown',slug:'homer',name:'x'}).saved,false);
  assert.equal(researchHref({kind:'location',slug:'javascript:x',name:'x'}),null);
  assert.equal(researchBonus([{kind:'character',slug:'homer',name:'H'},{kind:'character',slug:'bart',name:'B'},{kind:'character',slug:'lisa',name:'L'}]),0);
});
test('almacenamiento deshabilitado no afirma persistencia',()=>{
  const storage={getItem:()=>{throw new Error('denied')},setItem:()=>{throw new Error('denied')}};
  assert.equal(recordResearch(storage,{kind:'location',slug:'kwik-e-mart',name:'Kwik-E-Mart'}).saved,false);
  assert.deepEqual(readResearch(storage),[]);
});
