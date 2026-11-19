import test from 'node:test';
import assert from 'node:assert/strict';
import {cueNames,cueNotes,playSound,soundEnabled} from '../js/sound.js';
test('efectos originales no requieren audio externo ni se reproducen automaticamente',()=>{
  assert.ok(cueNames().includes('success'));
  assert.ok(cueNames().includes('donut'));
  assert.equal(soundEnabled(),false);
  assert.equal(playSound('success'),false);
  assert.deepEqual(cueNotes('__invalid__'),[]);
  assert.ok(cueNotes('stamp').every(n=>n[0]>0&&n[2]>0&&n[2]<.3));
});
