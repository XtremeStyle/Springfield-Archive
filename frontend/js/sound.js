/**
 * Springfield Foley: original PCM sounds created in scripts/generate-original-sfx.py.
 * No voices, episode music or automatic audio. All audio is packaged locally; playback is opt-in.
 * Audio only starts after a user gesture; all controls remain usable when muted.
 */
const PREF='springfield-sound:enabled:v1';
const VOLUME_PREF='springfield-sound:volume:v1';
const FILES=Object.freeze({
  open:'can-open', close:'can-close', success:'success', fail:'fail', hint:'hint',
  stamp:'stamp', donut:'donut', click:'click', start:'start', paper:'paper',
  reveal:'paper', vault:'vault', alert:'alert',
});
// Legacy note metadata kept for tests and migration, no longer used for playback.
const LEGACY=Object.freeze({stamp:[[420,0,.075,'sine']],success:[[520,0,.09,'sine']]});
let initialized=false,enabled=false,volume=.7,ambientWanted=false,ambient=null;
const pool=new Map();
let lastCue='',lastAt=0;

export const cueNames=()=>Object.keys(FILES);
export const cueNotes=(cue)=>LEGACY[cue]||[];
export const soundEnabled=()=>enabled;
export const soundVolume=()=>volume;
const safeStore=(key,value)=>{try{localStorage.setItem(key,String(value));}catch{}};
const safeRead=(key)=>{try{return localStorage.getItem(key);}catch{return null;}};
export function setSoundVolume(value){
  const n=Number(value);if(!Number.isFinite(n))return volume;
  volume=Math.max(0,Math.min(1,n));safeStore(VOLUME_PREF,volume);
  if(ambient)ambient.volume=volume*.16;
  if(typeof document!=='undefined')document.querySelectorAll('[data-sound-volume]').forEach(el=>el.value=String(volume));
  return volume;
}
function clip(cue){
  if(!Object.hasOwn(FILES,cue)||typeof Audio==='undefined')return null;
  if(!pool.has(cue)){
    const file=new Audio(`/assets/audio/${FILES[cue]}.wav?v=20261118-final2`);
    file.preload='auto';pool.set(cue,file);
  }
  return pool.get(cue);
}
export function playSound(cue){
  if(!enabled||volume<=0||typeof window==='undefined'||!Object.hasOwn(FILES,cue))return false;
  const now=Date.now();if(cue===lastCue&&now-lastAt<70)return false;
  lastCue=cue;lastAt=now;
  try{
    const original=clip(cue);if(!original)return false;
    // Clone permits overlapping cues (e.g. metallic pop and stamp) without resetting a previous effect.
    const audio=original.cloneNode();audio.volume=Math.min(1,volume*.84);
    // Los sonidos son secundarios: si el navegador no puede reproducirlos,
    // la interfaz permanece completamente operativa sin hacer solicitudes externas.
    void audio.play().catch(()=>{});
    return true;
  }catch{return false;}
}
function updateAmbient(){
  if(typeof document==='undefined')return;
  const shouldPlay=ambientWanted&&enabled&&volume>0&&!document.hidden;
  if(!shouldPlay){ambient?.pause();return;}
  try{
    if(!ambient){ambient=new Audio('/assets/audio/archive-ambience.wav?v=20261118-final2');ambient.loop=true;ambient.preload='auto';}
    ambient.volume=volume*.16;
    void ambient.play().catch(()=>{});
  }catch{/* Ambient sound is optional and never blocks gameplay. */}
}
export function setAmbient(wanted){ambientWanted=Boolean(wanted);updateAmbient();}
export function initSound(root=document){
  if(initialized||typeof document==='undefined')return;
  initialized=true;
  enabled=safeRead(PREF)==='yes';
  const rawVolume=safeRead(VOLUME_PREF);
  const saved=rawVolume===null?NaN:Number(rawVolume);
  volume=Number.isFinite(saved)&&saved>=0&&saved<=1?saved:.7;
  const button=root.querySelector('#sound-toggle');
  const update=()=>{
    if(!button)return;
    button.setAttribute('aria-pressed',String(enabled));
    button.setAttribute('aria-label',enabled?'Silenciar sonidos originales de Springfield':'Activar sonidos originales de Springfield');
    button.textContent=enabled?'♪ Sonido: sí':'♪ Sonido: no';
    button.title=enabled?'Silenciar sonidos':'Activar efectos de lata, papel e investigación';
  };
  button?.addEventListener('click',()=>{
    enabled=!enabled;if(enabled&&volume<=0)setSoundVolume(.68);safeStore(PREF,enabled?'yes':'no');update();
    if(enabled){playSound('click');updateAmbient();}else{ambient?.pause();}
  });
  document.addEventListener('springfield:sfx',event=>playSound(event.detail?.cue||''));
  document.addEventListener('visibilitychange',updateAmbient);
  document.querySelectorAll('[data-sound-volume]').forEach(el=>{
    el.value=String(volume);
    el.addEventListener('input',()=>setSoundVolume(el.value));
  });
  update();
}
export function emitSound(cue){
  if(typeof document!=='undefined')document.dispatchEvent(new CustomEvent('springfield:sfx',{detail:{cue}}));
}
