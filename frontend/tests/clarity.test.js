import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const read=path=>readFileSync(new URL(path,import.meta.url),'utf8');
test('sin enlaces a un cuaderno antiguo inexistente',()=>{
 for(const filename of ['index.html','personajes.html','lugares.html']){
  const html=read('../'+filename);
  assert.ok(!html.includes('/game.html#cuaderno'),filename);
 }
});
test('las tarjetas del catálogo nunca se ocultan esperando animaciones',()=>{
 const motion=read('../js/motion.js');
 const targets=motion.match(/const TARGETS\s*=\s*'([^']+)'/);
 assert.ok(targets);
 assert.ok(!targets[1].split(',').map(x=>x.trim()).includes('.card'));
});
test('la portada no repite tarjetas de curiosidades sin una seleccion editorial real',()=>{
 const home=read('../js/home.js');
 const html=read('../index.html');
 assert.ok(!home.includes('renderCuriosities'));
 assert.ok(!html.includes('id="curiosities"'));
});
