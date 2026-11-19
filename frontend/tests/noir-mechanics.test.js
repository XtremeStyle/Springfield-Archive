import test from 'node:test';
import assert from 'node:assert/strict';
import {inspectionCost,caseGrade,applyIntegrityCost,investigationFailed} from '../js/noir-mechanics.js';
test('dos primeras inspecciones son gratuitas; siguientes tienen coste uniforme',()=>{
  assert.equal(inspectionCost(0),0);assert.equal(inspectionCost(1),0);
  assert.equal(inspectionCost(2),6);assert.equal(inspectionCost(3),6);
});
test('una cantidad corrupta no produce costes negativos ni resultados no finitos',()=>{
  assert.equal(inspectionCost(-1),0);assert.equal(inspectionCost(NaN),0);
});
test('informes finales expresan consecuencias de la integridad alcanzada',()=>{
  assert.match(caseGrade(100).title,/Inspector/);
  assert.match(caseGrade(75).title,/reservas/);
  assert.match(caseGrade(12).title,/pelos/);
  assert.match(caseGrade(NaN).title,/sin validar/);
});

test('los fallos del juego agotan la integridad y detienen una partida inviable',()=>{
 assert.equal(applyIntegrityCost(12,18),0);
 assert.equal(applyIntegrityCost(100,6),94);
 assert.equal(applyIntegrityCost(50,0),50);
 assert.equal(applyIntegrityCost(180,6),94);
 assert.equal(applyIntegrityCost(0,-5),0);
 assert.equal(applyIntegrityCost(NaN,10),0);
 assert.equal(investigationFailed(0),true);
 assert.equal(investigationFailed(-1),true);
 assert.equal(investigationFailed(50),false);
});
