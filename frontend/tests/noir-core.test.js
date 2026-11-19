import test from 'node:test';
import assert from 'node:assert/strict';
import {buildMission,missionCode,validateCode,evaluateChoice,distinctRecords,orderedEvidence,verifyEvidenceOrder,makeAuditPacket,isIntruder,cipherDigit} from '../js/noir-core.js';
const chars=[{name:'Homer Simpson',slug:'homer-simpson',externalId:1,occupation:'Safety Inspector',status:'Alive'},
{name:'Marge Simpson',slug:'marge-simpson',externalId:2,occupation:'Unemployed',status:'Alive'},
{name:'Bart Simpson',slug:'bart-simpson',externalId:3,occupation:'Student',status:'Alive'},
{name:'Krusty the Clown',slug:'krusty-the-clown',externalId:4,occupation:'Clown',status:'Alive'},
{name:'Chief Wiggum',slug:'chief-wiggum',externalId:5,occupation:'Police Chief',status:'Alive'}];
const loc=[{name:'Moe Tavern',slug:'moe-tavern',externalId:11,use:'Tavern',town:'Springfield'},
{name:'Central',slug:'central',externalId:12,use:'Power Plant',town:'Springfield'},
{name:'School',slug:'school',externalId:13,use:'School',town:'Springfield'},
{name:'Hospital',slug:'hospital',externalId:14,use:'Hospital',town:'Springfield'}];
const eps=Array.from({length:4},(_,i)=>({name:'Episode '+(i+1),slug:'episode-'+(i+1),externalId:21+i,season:1,episodeNumber:i+1}));
const pools={characters:chars,locations:loc,episodes:eps};
test('no conserva duplicados editoriales, homónimos o IDs repetidos',()=>{
 assert.equal(distinctRecords([...chars,chars[0],{...chars[0],externalId:900,slug:'homer-copy'}, {...chars[0],externalId:null,slug:'editorial'}],'character').length,5);
});
test('misión usa tres recursos externos distintos y pistas reales en español',()=>{
 const cases=buildMission(pools,()=>0.37);
 assert.equal(cases.length,3);
 assert.deepEqual(cases.map(c=>c.kind),['character','location','episode']);
 cases.forEach(c=>{
  assert.equal(c.options.length,4);
  assert.equal(new Set(c.options.map(o=>o.name)).size,4);
  assert.ok(c.hints.every(h=>h.name&&h.value));
  assert.ok(c.options.some(o=>evaluateChoice(c,o.slug)));
  assert.match(c.href,/^\/(personaje|lugar|episodio)\.html\?slug=/);
 });
});
test('código de caja fuerte sale de IDs de la API y detecta respuestas erróneas',()=>{
 const cases=buildMission(pools,()=>.1);
 const expected=orderedEvidence(cases).map((c,i)=>cipherDigit(c.id,[7,4,2][i])).join('');
 assert.equal(missionCode(cases),expected);
 assert.equal(validateCode(cases,expected),true);
 assert.equal(validateCode(cases,'999'),expected==='999');
 assert.equal(validateCode(cases,'abc'),false);
 assert.equal(missionCode([cases[0]]),null);
});
test('no inventa pistas con registros incompletos',()=>{
 const unavailable=buildMission({characters:[],locations:loc,episodes:eps});
 assert.equal(unavailable,null);
});

test('auditoría incluye exactamente un expediente auténtico ajeno al caso',()=>{
 const cases=buildMission(pools,()=>.35);
 const packet=makeAuditPacket(cases,()=>.2);
 assert.equal(packet.length,4);
 assert.equal(new Set(packet.map(p=>p.kind+':'+p.slug)).size,4);
 const extras=packet.filter(p=>!cases.some(c=>c.answer===p.slug));
 assert.equal(extras.length,1);
 assert.equal(isIntruder(cases,extras[0].slug),true);
 assert.equal(isIntruder(cases,cases[0].answer),false);
});
test('ordenación comprueba los tres identificadores externos y rechaza alteraciones',()=>{
 const cases=buildMission(pools,()=>.26);
 const ordered=orderedEvidence(cases);
 assert.equal(verifyEvidenceOrder(cases,ordered.map(c=>c.answer)),true);
 assert.equal(verifyEvidenceOrder(cases,ordered.map(c=>c.answer).reverse()),false);
 assert.equal(verifyEvidenceOrder(cases,['a','b']),false);
});
test('cifrado 742 incorpora suma de dígitos y el sello en cada posición',()=>{
 assert.equal(cipherDigit(26,7),5);
 assert.equal(cipherDigit(0,7),null);
 assert.equal(cipherDigit(9,-1),null);
 assert.equal(cipherDigit(157,4),7);
});
