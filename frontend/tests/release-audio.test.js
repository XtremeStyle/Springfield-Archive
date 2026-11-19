import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,existsSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
const audioDir=fileURLToPath(new URL('../assets/audio/',import.meta.url));
const sound=readFileSync(new URL('../js/sound.js',import.meta.url),'utf8');
test('audio opt-in se sirve exclusivamente en archivos WAV locales',()=>{
  for(const cue of ['can-open','vault','alert','archive-ambience']){
    const file=`${audioDir}/${cue}.wav`;
    assert.ok(existsSync(file),`Debe existir ${cue}`);
    const data=readFileSync(file);
    assert.equal(data.toString('ascii',0,4),'RIFF');
    assert.equal(data.toString('ascii',8,12),'WAVE');
    assert.ok(data.byteLength>512);
  }
  assert.doesNotMatch(sound,/new Audio\(['"]https?:/);
  assert.match(sound,/enabled/);
});
