import test from 'node:test';
import assert from 'node:assert/strict';
import {descriptionEs,estadoEs,generoEs,ocupacionEs,tipoEs,shortMetaEs,originalText,verifiedFacts} from '../js/spanish-content.js';

test('las etiquetas disponibles del proveedor se traducen de forma controlada',()=>{
 assert.equal(estadoEs('Alive'),'Vivo');
 assert.equal(estadoEs('Deceased'),'Fallecido');
 assert.equal(generoEs('Male'),'Masculino');
 assert.equal(tipoEs('school'),'Centro educativo');
 assert.equal(ocupacionEs('Safety Inspector'),'Inspector de seguridad');
 assert.equal(shortMetaEs({occupation:'Safety Inspector',status:'Alive'},'character'),'Inspector de seguridad · Vivo');
});
test('no inventa reseñas ni muestra inglés como traducción',()=>{
 const data={name:'Personaje Ejemplo',externalId:999,occupation:'Custom English Profession',status:'Alive',description:'A long description in English.'};
 const info=descriptionEs(data,'character');
 assert.equal(info.text,null);
 assert.equal(originalText(data,'character'),data.description);
 assert.deepEqual(verifiedFacts(data,'character'),[['Estado','Vivo']]);
});
test('el contexto español de personajes conocidos se etiqueta como editorial, nunca como traducción',()=>{
 const data={name:'Homer Simpson',externalId:1,description:'Original upstream text in English.'};
 const info=descriptionEs(data,'character');
 assert.match(info.source,/editorial/);
 assert.match(info.text,/Marge/);
 assert.equal(originalText(data,'character'),data.description);
});
test('el uso en inglés desconocido no se convierte en un tipo supuesto',()=>{
 const data={name:'Sitio sin clasificar',externalId:123,town:'Springfield',use:'Unknown English Value'};
 assert.equal(descriptionEs(data,'location').text,null);
 assert.deepEqual(verifiedFacts(data,'location'),[['Localidad','Springfield']]);
});
