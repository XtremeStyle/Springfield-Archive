/** Caso 742: juego narrativo original, con pruebas exclusivamente de nuestra API Java.
 * No hay datos falsos, animación automática invasiva ni ranking susceptible de trampas.
 */
import {fetchJson} from './api.js';
import {mountChrome,escapeHtml,escapeAttr,imageForItem} from './components.js';
import {buildMission,validateCode,evaluateChoice,orderedEvidence,verifyEvidenceOrder,makeAuditPacket,isIntruder,CIPHER_OFFSETS} from './noir-core.js';
import {emitSound,setAmbient} from './sound.js';
import {hintFromEvidence} from './noir-rewards.js';
import {inspectionCost,caseGrade,applyIntegrityCost,investigationFailed} from './noir-mechanics.js';

mountChrome('game');
const stage=document.querySelector('#noir-stage');
const progress=document.querySelector('#noir-progress');
const status=document.querySelector('#noir-status');
const phaseTrack=document.querySelector('#noir-phase-track');
const PHASE_LABELS=['Persona','Lugar','Capítulo','Auditoría','Orden','Cifrado'];
const SAVE_KEY='springfield:caso742:historial:v1';
const FREE_INSPECTIONS=2;
const PHASES=6;
const SCENES=[
 {no:'ACTO I',place:'La llamada',headline:'Una firma que nadie reconoce',story:'El Ayuntamiento encontró una ficha de empleado en el cajón de las rosquillas. Identifica al personaje usando el oficio y los datos que quedaron en el expediente.',icon:'◉'},
 {no:'ACTO II',place:'La ruta',headline:'Un destino tachado',story:'Una dirección ha desaparecido del mapa de Springfield. El inventario conserva la función del lugar: úsala para reconstruir la ruta.',icon:'⌖'},
 {no:'ACTO III',place:'La grabación',headline:'Una cinta sin etiqueta',story:'La última prueba es un episodio. El sistema guardó la temporada y el número global; encuentra el capítulo correcto antes de cerrar el informe.',icon:'▣'},
];
const originalJokes={
 correct:['El funcionario estampa el papel. Es la primera decisión acertada de su turno.','La ruta vuelve al mapa. El GPS del alcalde sigue diciendo «recalculando».','La cinta se recuperó. El archivo exige dos copias y una tercera para perderla.'],
 wrong:['Ese nombre pertenece a otra ficha. El archivista ya estaba preparando su renuncia.','La señal conduce a otra dirección. Habrá que devolver el coche del municipio.','La cinta no coincide. Un técnico culpa a la gravedad, que no trabaja aquí.'],
};
let mission=null,step=0,health=100,choice=null,clueOpen=false,bonusUsed=false,locked=false,discovered=[];
let freeHint=false;
let inspected=new Set();
let inspections=0;
let auditPacket=[],intruderChoice=null,evidenceSequence=[];
function earnedHint(){
 try{return hintFromEvidence(localStorage);}catch{return false;}
}
function refreshHintReward(){
 if(bonusUsed)return;
 freeHint=earnedHint();
 const control=stage.querySelector('#noir-hint');
 if(control)control.textContent=freeHint?'Pista de campo (gratis)':'Pedir una pista (−10)';
}
document.addEventListener('springfield:donut-found',refreshHintReward);
window.addEventListener('focus',refreshHintReward);
const pad=n=>String(n).padStart(2,'0');
function lastScore(){
 try{const values=JSON.parse(localStorage.getItem(SAVE_KEY)||'[]');
 return Array.isArray(values)&&values.length&&Number.isFinite(values.at(-1)?.health)?values.at(-1).health:null;}catch{return null;}
}
const documentId=c=>String(c.id).padStart(4,'0');
function setStatus(){
 const phase=mission?`${Math.min(step+1,PHASES)} / ${PHASES}`:`0 / ${PHASES}`;
 progress.textContent=phase;
 status.textContent=mission?`Integridad ${health}% · ${discovered.length} expedientes recuperados`:'Esperando señal del archivo';
 if(phaseTrack)phaseTrack.innerHTML=PHASE_LABELS.map((name,i)=>`<span class="noir-phase ${mission?(i<step?'is-done':i===step?'is-current':'') : ''}" ${mission&&i===step?'aria-current="step"':''}><b>${i<step&&mission?'✓':i+1}</b><small>${name}</small></span>`).join('');
}
const escape=escapeHtml;
const icon=(key)=>({character:'♙',location:'⌖',episode:'▣'}[key]||'◈');
function play(cue){emitSound(cue);}
function Intro(){
 setAmbient(false);mission=null;step=0;bonusUsed=false;health=100;discovered=[];freeHint=earnedHint();setStatus();
 stage.innerHTML=`<div class="noir-intro noir-appear">
 <div class="noir-intro-story"><div class="noir-kicker">AYUNTAMIENTO DE SPRINGFIELD · TRANSMISIÓN 742</div>
 <h2>El caso del<br><em>archivo perdido.</em></h2>
 <p>Alguien mezcló tres documentos oficiales con el inventario de rosquillas. El Departamento de Archivos necesita un investigador... y el último se fue a almorzar.</p>
 <div class="noir-dialogue"><span class="noir-avatar">S</span><div><strong>Despacho municipal</strong><p>«Recupera tres fichas auténticas, descubre qué expediente fue infiltrado en el lote y restaura la secuencia antes de descifrar el archivo.»</p></div></div>
 <p class="noir-source-note">La trama es una ficción cómica original. Los documentos y las respuestas se construyen con datos importados de <strong>The Simpsons API</strong> y servidos por Java.</p>
 <div class="noir-rules" aria-label="Reglas del juego"><span><b>01</b> Lee las pruebas</span><span><b>02</b> Investiga sospechosos</span><span><b>03</b> Detecta el intruso</span><span><b>04</b> Descifra el protocolo 742</span></div>
 <button id="noir-begin" class="noir-main-button" type="button">Aceptar el caso <span aria-hidden="true">→</span></button>
 <p class="noir-footnote">La misión tiene seis fases y decisiones con consecuencias. Sin temporizador ni puntuaciones públicas. ${freeHint?'✓ Pista de campo gratis desbloqueada en la wiki.':'Explora tres fichas de dos categorías o encuentra la dona del encabezado para conseguir una pista sin penalización.'}</p>
 ${lastScore()!==null?`<p class="noir-last-run">Último expediente: ${lastScore()}% de integridad. ¿Puedes superarlo?</p>`:""}
 </div><div class="noir-intro-art" aria-hidden="true"><div class="noir-city"><i></i><i></i><i></i><i></i><i></i><i></i></div><div class="noir-folder"><div>SPRINGFIELD / ARCHIVOS</div><strong>742</strong><span>NO ABRIR<br>SIN CAFÉ</span></div><div class="noir-police-line">DEPARTAMENTO DE INCIDENTES MENORES</div></div>
 </div>`;
 document.querySelector('#noir-begin').addEventListener('click',()=>void loadMission());
}
async function loadMission(){
 stage.innerHTML='<div class="noir-wait" role="status"><span class="noir-spinner"></span><h2>Descifrando la señal municipal…</h2><p>Consultando personajes, lugares y episodios de nuestra API Java.</p></div>';
 play('paper');
 try{
  const kinds=[['characters','character'],['locations','location'],['episodes','episode']];
  const first=await Promise.all(kinds.map(([endpoint])=>fetchJson('/'+endpoint,{size:50,page:0,...(endpoint==='episodes'?{}:{source:'curated'})})));
  if(first.some(r=>!Array.isArray(r.content)||!Number.isInteger(r.totalPages)))throw new Error('La API Java no devolvió el contrato esperado.');
  const extra=await Promise.all(kinds.map(async([endpoint],i)=>{
   const n=first[i].totalPages;
   if(n<=1)return [];
   const page=1+Math.floor(Math.random()*Math.min(n-1,11));
   try{const resp=await fetchJson('/'+endpoint,{size:50,page,...(endpoint==='episodes'?{}:{source:'curated'})});return Array.isArray(resp.content)?resp.content:[];}
   catch{return [];}
  }));
  const pools=Object.fromEntries(kinds.map(([endpoint],i)=>[endpoint,[...first[i].content,...extra[i]]]));
  mission=buildMission(pools);
  if(!mission){throw new Error('Faltan datos completos para elaborar tres pruebas deducibles. Ejecuta la sincronización de personajes, lugares y episodios.');}
  step=0;health=100;choice=null;bonusUsed=false;locked=false;discovered=[];freeHint=earnedHint();auditPacket=[];intruderChoice=null;evidenceSequence=[];
  setAmbient(true);play('start');renderCase();
 }catch(e){setAmbient(false);stage.innerHTML=`<section class="noir-error" role="alert"><h2>No hay expediente jugable todavía</h2><p>${escape(e.message)}</p><p>El juego usa exclusivamente registros reales de Java/PostgreSQL. No sustituye la API por candidatos inventados.</p><button id="noir-retry" class="noir-main-button" type="button">Intentar otra vez ↺</button></section>`;
 document.getElementById('noir-retry')?.addEventListener('click',()=>void loadMission());}
}
function renderCase(){
 if(!mission||step>=3){if(step===3)renderAudit();else if(step===4)renderSorting();else renderVault();return;}
 const c=mission[step],scene=SCENES[step];
 choice=null;clueOpen=false;locked=false;inspected=new Set();inspections=0;setStatus();
 stage.innerHTML=`<section class="noir-case noir-appear" data-act="${step+1}">
  <div class="noir-case-head"><div><span class="noir-kicker">${escape(scene.no)} · ${escape(scene.place)}</span><h2 tabindex="-1">${escape(scene.headline)}</h2><p>${escape(scene.story)}</p></div><div class="noir-case-emblem" aria-hidden="true">${scene.icon}</div></div>
  <div class="noir-evidence-strip" aria-label="Tablero de pruebas">${[0,1,2].map((i)=>`<div class="noir-pin ${i<discovered.length?'collected':''}" aria-label="${i<discovered.length?'Prueba '+(i+1)+' recuperada':'Prueba '+(i+1)+' pendiente'}"><span>${i<discovered.length?'✓':i+1}</span><small>${['Persona','Lugar','Capítulo'][i]}</small></div>`).join('')}</div>
  <div class="noir-desk"><div class="noir-evidence"><span class="noir-kicker">DOCUMENTO Nº ${pad(step+1)} · SOBRE CONFIDENCIAL</span>
   <button class="noir-envelope" id="noir-envelope" type="button" aria-expanded="false" aria-controls="noir-clues"><span class="noir-envelope-flap"></span><span class="noir-envelope-stamp">${escape(c.type)}</span><strong>ABRIR PRUEBA</strong><small>La etiqueta del sobre se perdió. El contenido no.</small></button>
   <div id="noir-clues" class="noir-clues" hidden><h3>Datos recuperados</h3><dl>${c.hints.map(h=>`<dt>${escape(h.name)}</dt><dd>${escape(h.value)}</dd>`).join('')}</dl><p>Elige una ficha compatible con <strong>todos</strong> los datos. Puedes inspeccionarla antes de firmar.</p></div>
   <div class="noir-resources"><span class="noir-kicker">LO QUE LLEVAS</span><p>${discovered.length?`${discovered.length} fichas auténticas recuperadas. Sus identificadores se guardan para la caja fuerte.`:'Ninguna ficha recuperada todavía.'}</p></div>
  </div><div class="noir-suspects"><div class="noir-suspects-head"><div><span class="noir-kicker">MESA DE IDENTIFICACIÓN</span><h3>¿Qué ficha corresponde?</h3></div><span>0${step+1}/03</span></div>
    <div class="noir-options">${c.options.map((o,i)=>`<button type="button" class="noir-option" data-answer="${escapeAttr(o.slug)}" aria-pressed="false" ${!clueOpen?'disabled':''}><span class="noir-option-image"><img src="${escapeAttr(imageForItem({externalId:o.externalId,portraitPath:o.mediaPath,imagePath:o.mediaPath},o.kind))}" alt="" loading="lazy"></span><strong>${escape(o.name)}</strong><small>Ficha reservada · candidato ${i+1}</small></button>`).join('')}</div>
    <div class="noir-inspection-meter" id="noir-meter" role="status">Dos inspecciones gratuitas por acto; las siguientes cuestan 6 puntos de integridad. También puedes consultar la wiki.</div>
    <div id="noir-inspection" class="noir-inspection" role="status">Abre el sobre para empezar.</div>
    <div class="noir-actions"><button id="noir-inspect" class="noir-quiet-button" type="button" disabled>Examinar ficha elegida</button><button id="noir-verify" class="noir-main-button" type="button" disabled>Firmar dictamen →</button><button id="noir-hint" class="noir-quiet-button" type="button" ${!clueOpen||bonusUsed?'disabled':''}>${bonusUsed?'Pista ya utilizada':(freeHint?'Pista de campo (gratis)':'Pedir una pista (−10)')}</button></div>
  </div></div><p class="noir-shortcuts">Teclado: 1–4 elegir candidato · I examinar · V verificar · H pedir pista.</p></section>`;
 const envelope=stage.querySelector('#noir-envelope');
 envelope.addEventListener('click',()=>{
  if(clueOpen)return;
  clueOpen=true;envelope.classList.add('is-open');envelope.setAttribute('aria-expanded','true');
  stage.querySelector('#noir-clues').hidden=false;
  stage.querySelectorAll('[data-answer]').forEach(b=>b.disabled=false);
  const hint=stage.querySelector('#noir-hint');
  hint.disabled=bonusUsed;
  if(bonusUsed)hint.textContent='Pista ya utilizada';
  stage.querySelector('#noir-inspection').textContent='Compara los datos del documento con una ficha antes de emitir tu veredicto.';
  play('paper');
 });
 function showSelected(){
  const panel=stage.querySelector('#noir-inspection');
  const button=stage.querySelector('#noir-inspect');
  const option=c.options.find(o=>o.slug===choice);
  if(!option){panel.textContent='Elige un candidato para continuar.';button.disabled=true;return;}
  const revealed=inspected.has(option.slug);
  panel.innerHTML=`<strong>${escape(option.name)}</strong><p>${revealed?`Datos de la ficha: ${escape(option.meta)}`:'Datos reservados. Examina la ficha o consúltala en la wiki antes de firmar.'}</p><a href="${escapeAttr(option.href)}" target="_blank" rel="noopener noreferrer">Abrir ficha y fuente de la API ↗</a>`;
  button.disabled=locked||!clueOpen||revealed;
  button.textContent=revealed?'Ficha examinada':`Examinar ficha ${inspectionCost(inspections,FREE_INSPECTIONS)===0?'(gratis)':'(−6 puntos)'}`;
 }
 stage.querySelectorAll('[data-answer]').forEach(button=>button.addEventListener('click',()=>{
  if(locked||!clueOpen)return;
  choice=button.dataset.answer;
  stage.querySelectorAll('[data-answer]').forEach(el=>el.setAttribute('aria-pressed',String(el===button)));
  showSelected();
  stage.querySelector('#noir-verify').disabled=false;
  play('click');
 }));
 stage.querySelector('#noir-inspect').addEventListener('click',()=>{
  if(locked||!choice||inspected.has(choice))return;
  const cost=inspectionCost(inspections,FREE_INSPECTIONS);
  health=applyIntegrityCost(health,cost);
  if(investigationFailed(health)){renderFailure('Se agotó el presupuesto de inspección. Burns confiscó las pruebas por exceso de papeleo.');return;}
  inspected.add(choice);inspections++;
  const selected=stage.querySelectorAll('[data-answer]');
  selected.forEach(el=>{if(el.dataset.answer===choice)el.classList.add('is-inspected');});
  const free=Math.max(0,FREE_INSPECTIONS-inspections);
  stage.querySelector('#noir-meter').textContent=`Inspecciones: ${inspections} realizadas · ${free} gratuitas disponibles${cost?' · coste aplicado: 6 puntos':''}.`;
  showSelected();setStatus();play('paper');
 });
 stage.querySelector('#noir-verify').addEventListener('click',()=>verify());
 stage.querySelector('#noir-hint').addEventListener('click',()=>{
  if(locked||bonusUsed)return;
  freeHint=freeHint||earnedHint();
  bonusUsed=true;if(!freeHint)health=applyIntegrityCost(health,10);if(investigationFailed(health)){renderFailure('No quedan recursos para continuar con las pistas. El ayuntamiento suspendió la investigación.');return;}setStatus();play('hint');stage.querySelector('#noir-hint').disabled=true;stage.querySelector('#noir-hint').textContent='Pista ya utilizada';
  // Una pista no regala la respuesta: elimina exactamente UN distractor.
  const falseChoice=c.options.find(o=>o.slug!==c.answer&&!stage.querySelector(`[data-answer="${o.slug}"]`)?.disabled);
  const target=stage.querySelector(`[data-answer="${falseChoice?.slug||''}"]`);
  if(target){target.disabled=true;target.classList.add('ruled-out');target.setAttribute('aria-pressed','false');if(choice===falseChoice.slug){choice=null;stage.querySelector('#noir-verify').disabled=true;stage.querySelector('#noir-inspect').disabled=true;}}
  stage.querySelector('#noir-inspection').textContent=`Una ficha quedó descartada ${freeHint?'gracias a tu exploración de la wiki, sin penalización':'con una penalización de diez puntos'}. Burns llamaría a esto «reducción de personal».`;
 });
 stage.querySelector('h2')?.focus({preventScroll:true});
}
function verify(){
 if(!mission||locked||!choice)return;
 const c=mission[step];const correct=evaluateChoice(c,choice);
 if(!correct){
  health=applyIntegrityCost(health,18);play('fail');if(investigationFailed(health)){renderFailure('Las pruebas se contradicen demasiado. El archivo quedó intervenido y la investigación fue suspendida.');return;}setStatus();
  const feedback=stage.querySelector('#noir-inspection');
  feedback.innerHTML=`<strong>No coincide.</strong> ${escape(originalJokes.wrong[step])} <span>Intenta con otra ficha.</span>`;
  const selectedButton=stage.querySelector(`[data-answer="${choice}"]`);if(selectedButton){selectedButton.disabled=true;selectedButton.classList.add('ruled-out');}
  choice=null;stage.querySelector('#noir-verify').disabled=true;stage.querySelector('#noir-inspect').disabled=true;
  return;
 }
 locked=true;discovered.push(c);play('stamp');setStatus();
 stage.querySelectorAll('[data-answer]').forEach(el=>el.disabled=true);
 stage.querySelector('#noir-hint').disabled=true;stage.querySelector('#noir-verify').disabled=true;stage.querySelector('#noir-inspect').disabled=true;
 const feedback=stage.querySelector('#noir-inspection');
 feedback.classList.add('is-correct');
 feedback.innerHTML=`<strong>✓ Verificación correcta.</strong><p>${escape(originalJokes.correct[step])}</p><p>Prueba archivada: <strong>${escape(c.name)}</strong> · ID del proveedor <strong>#${documentId(c)}</strong> · último dígito <strong>${c.codeDigit}</strong>.</p><a href="${escapeAttr(c.href)}" target="_blank" rel="noopener noreferrer">Consultar la ficha real ↗</a><button id="noir-next" class="noir-main-button" type="button">${step===2?'Auditar el lote':'Siguiente escenario'} →</button>`;
 feedback.querySelector('#noir-next').addEventListener('click',()=>{step++;play('paper');renderCase();});
}
/** Consecuencia verificable: sin integridad no se puede ganar por fuerza bruta. */
function renderFailure(reason){
 locked=true;setAmbient(false);play('alert');storeResult(false);
 const found=discovered.length;setStatus();
 status.textContent='Expediente cerrado · integridad agotada';
 stage.innerHTML=`<section class="noir-ending noir-appear" aria-labelledby="noir-failed-heading">
 <div class="noir-ending-stamp noir-stamp-failed" aria-hidden="true">CASO<br>SUSPENDIDO</div>
 <span class="noir-kicker">ACTA MUNICIPAL · RESULTADO INSUFICIENTE</span>
 <h2 id="noir-failed-heading" tabindex="-1">La trituradora ganó esta ronda.</h2>
 <p>${escape(reason)}</p>
 <div class="noir-ending-score"><div><strong>0%</strong><span>Integridad restante</span></div><div><strong>${found}/3</strong><span>Fichas recuperadas</span></div></div>
 <p>Una investigación también requiere saber cuándo detenerse. Tus próximas decisiones pueden conservar más pruebas.</p>
 <div class="noir-ending-actions"><button id="noir-restart" type="button" class="noir-main-button">Reabrir el caso ↺</button><a class="noir-quiet-button" href="/personajes.html">Consultar la wiki ↗</a></div>
 </section>`;
 stage.querySelector('#noir-restart').addEventListener('click',Intro);
 stage.querySelector('h2')?.focus({preventScroll:true});
}
/** Fase IV: una ficha real se coló entre las tres que el jugador recuperó. */
function renderAudit(){
 if(!mission)return;
 step=3;locked=false;intruderChoice=null;setStatus();
 if(!auditPacket.length)auditPacket=makeAuditPacket(mission)||[];
 if(auditPacket.length!==4){renderFailure('El sistema no recibió suficientes evidencias para auditar.');return;}
 stage.innerHTML=`<section class="noir-forensic noir-appear"><span class="noir-kicker">FASE IV / CONTROL DE DOCUMENTOS</span><h2 tabindex="-1">Un expediente se coló en el lote.</h2>
 <p>El sistema recuperó cuatro documentos de nuestra API, pero tú <strong>solo verificaste tres</strong>. Uno es auténtico como dato de la serie, pero NO pertenece a esta investigación. ¿Cuál hay que retirar antes de enviar las pruebas al archivo?</p>
 <div class="noir-recovered"><strong>Tus actas verificadas:</strong>${mission.map(c=>`<span>${escape(c.name)}</span>`).join('')}</div>
 <div class="noir-audit-grid">${auditPacket.map((r,i)=>`<button type="button" class="noir-audit-file" data-audit="${escapeAttr(r.slug)}" aria-pressed="false"><small>FICHA ${pad(i+1)} / ${escape(r.kind==='character'?'PERSONA':r.kind==='location'?'LUGAR':'CAPÍTULO')}</small><strong>${escape(r.name)}</strong><span>Registro externo #${documentId(r)}</span></button>`).join('')}</div>
 <div class="noir-forensic-actions"><button id="noir-audit-verify" class="noir-main-button" type="button" disabled>Retirar expediente intruso →</button><p id="noir-audit-feedback" role="status">Selecciona el documento que no figuraba entre tus tres actas.</p></div>
 </section>`;
 stage.querySelectorAll('[data-audit]').forEach(btn=>btn.addEventListener('click',()=>{
  if(locked||btn.disabled)return;intruderChoice=btn.dataset.audit;
  stage.querySelectorAll('[data-audit]').forEach(other=>other.setAttribute('aria-pressed',String(other===btn)));
  stage.querySelector('#noir-audit-verify').disabled=false;play('click');
 }));
 stage.querySelector('#noir-audit-verify').addEventListener('click',()=>{
  if(locked||!intruderChoice)return;
  if(!isIntruder(mission,intruderChoice)){
    health=applyIntegrityCost(health,14);play('fail');
    if(investigationFailed(health)){renderFailure('La auditoría identificó mal el lote y confiscó las pruebas.');return;}
    stage.querySelector('#noir-audit-feedback').textContent='Ese expediente SÍ fue verificado. Pierdes 14 de integridad; revisa tus tres actas antes de retirar otro.';
    stage.querySelector(`[data-audit="${intruderChoice}"]`).disabled=true;intruderChoice=null;stage.querySelector('#noir-audit-verify').disabled=true;setStatus();return;
  }
  locked=true;play('stamp');setStatus();
  stage.querySelectorAll('[data-audit]').forEach(b=>b.disabled=true);
  stage.querySelector('#noir-audit-feedback').innerHTML='<strong>✓ Lote limpio.</strong> El cuarto documento no estaba entre tus pruebas. El alcalde propone darle un cargo público.';
  stage.querySelector('#noir-audit-verify').outerHTML='<button id="noir-next" class="noir-main-button" type="button">Ordenar las actas verificadas →</button>';
  stage.querySelector('#noir-next').addEventListener('click',()=>{step=4;play('paper');renderSorting();});
 });
 stage.querySelector('h2')?.focus({preventScroll:true});
}
/** Fase V: mover actas con botones accesibles en vez de arrastrar y soltar. */
function renderSorting(){
 if(!mission)return;
 step=4;locked=false;setStatus();
 if(!evidenceSequence.length){
  const correct=orderedEvidence(mission);
  // Rotación deliberada: nadie completa la etapa por azar al abrirla.
  evidenceSequence=[correct[1],correct[2],correct[0]];
 }
 stage.innerHTML=`<section class="noir-forensic noir-appear"><span class="noir-kicker">FASE V / MESA DE ORDENACIÓN</span><h2 tabindex="-1">El protocolo de transferencia.</h2>
 <p>El administrador archivó las actas en cualquier orden. Debes colocarlas según su <strong>ID externo, de menor a mayor</strong>. Si dos IDs coinciden, da prioridad a Persona → Lugar → Capítulo. El orden resultante será el del cifrado final; no representa relaciones entre esos recursos.</p>
 <div class="noir-order-list" id="noir-order-list" aria-label="Orden actual de actas"></div>
 <div class="noir-forensic-actions"><button id="noir-sort-verify" class="noir-main-button" type="button">Comprobar secuencia →</button><p id="noir-sort-feedback" role="status">Usa ↑ y ↓ para cambiar de posición cada documento.</p></div></section>`;
 const list=stage.querySelector('#noir-order-list');
 function draw(){
  list.innerHTML=evidenceSequence.map((c,i)=>`<article class="noir-order-file"><span class="noir-order-position">0${i+1}</span><div class="noir-order-meta"><strong>${escape(c.name)}</strong><small>${escape(c.type)} · ID externo ${documentId(c)}</small></div><div class="noir-order-buttons"><button type="button" data-move="up" data-index="${i}" aria-label="Subir ${escapeAttr(c.name)}" ${i===0?'disabled':''}>↑</button><button type="button" data-move="down" data-index="${i}" aria-label="Bajar ${escapeAttr(c.name)}" ${i===2?'disabled':''}>↓</button></div></article>`).join('');
 }
 list.addEventListener('click',e=>{
  const button=e.target.closest('button[data-move]');if(!button||locked)return;
  const i=Number(button.dataset.index),j=i+(button.dataset.move==='up'?-1:1);
  if(i<0||i>=3||j<0||j>=3)return;
  [evidenceSequence[i],evidenceSequence[j]]=[evidenceSequence[j],evidenceSequence[i]];
  draw();list.querySelector(`[data-index="${j}"][data-move="${button.dataset.move}"]`)?.focus();play('paper');
 });
 stage.querySelector('#noir-sort-verify').addEventListener('click',()=>{
  if(locked)return;
  if(!verifyEvidenceOrder(mission,evidenceSequence.map(c=>c.answer))){
    health=applyIntegrityCost(health,9);play('fail');setStatus();
    if(investigationFailed(health)){renderFailure('El archivo rechazó demasiadas secuencias incorrectas.');return;}
    stage.querySelector('#noir-sort-feedback').textContent='Los IDs no están ordenados de menor a mayor. −9 de integridad. Comprueba los números y prueba de nuevo.';
    return;
  }
  locked=true;play('success');setStatus();
  list.querySelectorAll('button').forEach(b=>b.disabled=true);
  stage.querySelector('#noir-sort-feedback').innerHTML='<strong>✓ Secuencia recuperada.</strong> El sistema ha dejado de clasificar por orden de llegada al almuerzo.';
  stage.querySelector('#noir-sort-verify').outerHTML='<button id="noir-next" class="noir-main-button" type="button">Descifrar la caja fuerte →</button>';
  stage.querySelector('#noir-next').addEventListener('click',()=>{step=5;play('paper');renderVault();});
 });
 draw();stage.querySelector('h2')?.focus({preventScroll:true});
}
/** Fase VI: código de tres dígitos derivado de IDs reales y sello 742. */
function renderVault(){
 if(!mission)return;
 step=5;setStatus();play('start');
 const ordered=orderedEvidence(mission);
 stage.innerHTML=`<section class="noir-vault noir-appear"><div class="noir-vault-head"><span class="noir-kicker">FASE VI / ALMACÉN MUNICIPAL</span><h2 tabindex="-1">La caja fuerte de Burns</h2><p>La clave no está escrita en ninguna ficha. Usa el orden que recuperaste y el cifrado del expediente <strong>742</strong>. Para cada acta, <strong>suma los dígitos de su ID</strong>, agrega el número del sello que corresponda (7, 4 o 2) y conserva <strong>solo la última cifra</strong>.</p>
 <p class="noir-cipher-example">Ejemplo: ID 26 y sello 7 → 2 + 6 + 7 = 15 → dígito 5. Las cifras se calculan localmente con datos auténticos de la API.</p></div>
 <div class="noir-receipts">${ordered.map((c,i)=>`<article class="noir-receipt"><span>POSICIÓN ${i+1} · SELLO +${CIPHER_OFFSETS[i]}</span><strong>${escape(c.name)}</strong><small>ID externo #${documentId(c)}</small><a href="${escapeAttr(c.href)}" target="_blank" rel="noopener noreferrer">Revisar ficha ↗</a></article>`).join('')}</div>
 <form id="noir-vault-form" class="noir-vault-console"><span id="noir-code-label" class="noir-vault-label">Tu código de tres dígitos</span><div class="noir-dials" role="group" aria-labelledby="noir-code-label">${[0,1,2].map(i=>`<select class="noir-dial" aria-label="Dígito ${i+1}" data-digit="${i}">${Array.from({length:10},(_,n)=>`<option value="${n}">${n}</option>`).join('')}</select>`).join('')}</div><button class="noir-main-button" type="submit">Abrir expediente clasificado ↗</button><p id="noir-vault-feedback" role="status"></p></form>
 </section>`;
 stage.querySelector('#noir-vault-form').addEventListener('submit',event=>{
  event.preventDefault();const value=[...stage.querySelectorAll('[data-digit]')].map(el=>el.value).join('');
  if(validateCode(mission,value)){play('vault');renderEnding();}
  else{health=applyIntegrityCost(health,12);play('fail');if(investigationFailed(health)){renderFailure('La caja fuerte cerró el protocolo municipal: demasiadas claves incorrectas.');return;}setStatus();stage.querySelector('#noir-vault-feedback').textContent='Clave incorrecta (−12). Revisa el orden ascendente, la suma de cada ID y el sello 742.';}
 });
 stage.querySelector('h2')?.focus({preventScroll:true});
}
function storeResult(completed=true){
 try{
  const old=JSON.parse(localStorage.getItem(SAVE_KEY)||'[]');const safe=Array.isArray(old)?old.slice(-9):[];
  safe.push({date:new Date().toISOString(),health,completed,ids:mission.map(c=>c.id)});
  localStorage.setItem(SAVE_KEY,JSON.stringify(safe));
 }catch{}
}
function renderEnding(){
 setAmbient(false);storeResult();step=PHASES;setStatus();play('stamp');
 stage.innerHTML=`<section class="noir-ending noir-appear"><div class="noir-ending-stamp" aria-hidden="true">ARCHIVO<br>RECUPERADO</div><span class="noir-kicker">CASO 742 / INFORME MUNICIPAL</span><h2 tabindex="-1">Informe entregado.<br><em>La trituradora pierde.</em></h2><p>Recuperaste tres fichas reales, retiraste un documento intruso, reconstruiste el orden de las actas y descifraste el protocolo 742. El ayuntamiento anunció una investigación interna sobre el presupuesto de las grapas.</p>
 <div class="noir-verdict"><strong>${escape(caseGrade(health).title)}</strong><p>${escape(caseGrade(health).message)}</p></div>
 <div class="noir-ending-score"><div><strong>${health}%</strong><span>Integridad conservada</span></div><div><strong>6/6</strong><span>Fases completadas</span></div></div>
 <div class="noir-ending-links">${mission.map(c=>`<a href="${escapeAttr(c.href)}" target="_blank" rel="noopener noreferrer">${escape(icon(c.kind))} ${escape(c.name)} ↗</a>`).join('')}</div>
 <p>Esta misión se generó a partir de datos guardados en PostgreSQL y servidos por Java. La trama es una ficción propia, no una relación narrativa declarada por la API.</p>
 <div class="noir-ending-actions"><button id="noir-download" type="button" class="noir-main-button">Descargar informe .txt</button><button id="noir-replay" type="button" class="noir-quiet-button">Jugar otro caso ↺</button></div></section>`;
 stage.querySelector('#noir-download').addEventListener('click',()=>{
  const lines=['SPRINGFIELD ARCHIVE — CASO 742','Informe de investigación (ficción interactiva)',`Integridad: ${health}%`,...mission.map((c,i)=>`${i+1}. ${c.type}: ${c.name} · ID API ${c.id} · ${location.origin+c.href}`),'Datos de la API pública almacenados en PostgreSQL por el backend Java.'];
  const url=URL.createObjectURL(new Blob([lines.join('\n')],{type:'text/plain;charset=utf-8'}));
  const a=document.createElement('a');a.href=url;a.download='SpringfieldArchive-Caso742.txt';a.click();
  setTimeout(()=>URL.revokeObjectURL(url),1000);
 });
 stage.querySelector('#noir-replay').addEventListener('click',Intro);
 stage.querySelector('h2')?.focus({preventScroll:true});
}
// Atajos voluntarios: no interfieren con inputs, selectores ni con la caja fuerte.
document.addEventListener('keydown',event=>{
 if(event.altKey||event.ctrlKey||event.metaKey||event.repeat||/^(INPUT|SELECT|TEXTAREA)$/.test(event.target?.tagName||''))return;
 if(!mission||step>=3||!clueOpen||locked)return;
 const key=event.key.toLowerCase();
 if(/^[1-4]$/.test(key)){const target=[...stage.querySelectorAll('.noir-option')][Number(key)-1];target?.click();return;}
 if(key==='i')stage.querySelector('#noir-inspect:not(:disabled)')?.click();
 if(key==='v')stage.querySelector('#noir-verify:not(:disabled)')?.click();
 if(key==='h')stage.querySelector('#noir-hint:not(:disabled)')?.click();
});
Intro();
