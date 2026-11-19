import test from 'node:test';
import assert from 'node:assert/strict';
import {hintFromEvidence,DONUT_KEY} from '../js/noir-rewards.js';
import {recordResearch} from '../js/research-log.js';
const storage=()=>{
 const map=new Map();return {getItem:k=>map.get(k)||null,setItem:(k,v)=>map.set(k,v)};
};
test('sin exploración no hay pista gratis',()=>assert.equal(hintFromEvidence(storage()),false));
test('la dona del encabezado concede un uso gratuito',()=>{
 const s=storage();s.setItem(DONUT_KEY,'yes');assert.equal(hintFromEvidence(s),true);
});
test('abrir tres fichas reales de al menos dos categorías habilita una pista',()=>{
 const s=storage();
 recordResearch(s,{kind:'character',slug:'homer-simpson',name:'Homer Simpson'});
 recordResearch(s,{kind:'character',slug:'marge-simpson',name:'Marge Simpson'});
 assert.equal(hintFromEvidence(s),false);
 recordResearch(s,{kind:'location',slug:'simpson-house',name:'Simpson House'});
 assert.equal(hintFromEvidence(s),true);
});
test('almacenamiento no disponible no bloquea el juego',()=>{
 const s={getItem:()=>{throw new Error('denegado')}};assert.equal(hintFromEvidence(s),false);
});
