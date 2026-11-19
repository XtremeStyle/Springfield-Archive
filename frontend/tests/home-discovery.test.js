import test from 'node:test';
import assert from 'node:assert/strict';
import {nextDiscovery,randomPage} from '../js/home-discovery.js';
const docs=[{slug:'a',name:'A'},{slug:'b',name:'B'},{slug:'c',name:'C'}];
test('exploración no repite fichas visitadas ni devuelve datos inválidos',()=>{
 assert.equal(nextDiscovery(docs,new Set(['a','b']),()=>0).slug,'c');
 assert.equal(nextDiscovery(docs,new Set(['a','b','c'])),null);
 assert.equal(nextDiscovery([{slug:'../bad',name:'X'}, ...docs],new Set(),()=>0).slug,'a');
});
test('página aleatoria tiene límites y tolera fuentes malformadas',()=>{
 assert.equal(randomPage(100,()=>.995),99);
 assert.equal(randomPage(100,()=>0),0);
 assert.equal(randomPage(0,()=>1),0);
 assert.equal(randomPage(5,()=>Number.NaN),0);
});
