import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
const names=['can-open','can-close','paper','stamp','success','fail','click','hint','donut','start','archive-ambience'];
test('los efectos Foley originales existen como WAV válidos en el proyecto, sin red',()=>{
 for(const name of names){
  const data=readFileSync(new URL(`../assets/audio/${name}.wav`,import.meta.url));
  assert.equal(data.subarray(0,4).toString(),'RIFF');
  assert.equal(data.subarray(8,12).toString(),'WAVE');
  assert.ok(data.length>1000 && data.length<600_000, `${name} tiene tamaño razonable`);
 }
});

test('el efecto Duff es estéreo, corto y semejante a una apertura de lata normal',()=>{
 const buffer=readFileSync(new URL('../assets/audio/can-open.wav',import.meta.url));
 const channels=buffer.readUInt16LE(22);
 const sampleRate=buffer.readUInt32LE(24);
 const bytesPerSecond=buffer.readUInt32LE(28);
 assert.equal(channels,2);
 assert.equal(sampleRate,44100);
 assert.ok((buffer.length-44)/bytesPerSecond>=.7 && (buffer.length-44)/bytesPerSecond<=1.2);
});
