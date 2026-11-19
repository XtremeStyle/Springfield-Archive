import test from 'node:test';
import assert from 'node:assert/strict';

test('primera activacion del audio conserva volumen audible y respeta volumen 0 guardado',async()=>{
  const priorDocument=globalThis.document;
  const priorStorage=globalThis.localStorage;
  const values=new Map();
  const fakeButton={attrs:{},setAttribute(k,v){this.attrs[k]=v;},addEventListener(){},set textContent(v){this.label=v;}};
  globalThis.localStorage={getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v)};
  globalThis.document={querySelectorAll:()=>[],addEventListener:()=>{}};
  const root={querySelector:()=>fakeButton};
  try {
    const mod=await import('../js/sound.js?new-install-vol');
    mod.initSound(root);
    assert.equal(mod.soundVolume(),.7,'sin preferencia guardada no debe iniciarse a volumen cero');
    assert.equal(fakeButton.attrs['aria-pressed'],'false');
    assert.equal(mod.setSoundVolume(0),0,'cero voluntario es válido');
    const mod2=await import('../js/sound.js?stored-mute');
    mod2.initSound(root);
    assert.equal(mod2.soundVolume(),0,'el volumen anterior sigue guardado hasta una nueva activación');
  } finally {
    if(priorDocument===undefined)delete globalThis.document;else globalThis.document=priorDocument;
    if(priorStorage===undefined)delete globalThis.localStorage;else globalThis.localStorage=priorStorage;
  }
});
