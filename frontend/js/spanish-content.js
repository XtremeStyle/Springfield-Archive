/**
 * Presentación honesta en español. El proveedor publica muchos campos en inglés
 * y NO ofrece traducciones oficiales: no inventamos una biografía con frases
 * genéricas ni mezclamos las reseñas editoriales V2 con los registros de la API.
 */
import {verifiedBioEs} from './verified-bios-es.js';
import {editorialEs} from './editorial-es.js';
const statuses = new Map([['alive','Vivo'],['deceased','Fallecido'],['dead','Fallecido'],['unknown','Desconocido'],['unconfirmed','Sin confirmar']]);
const genders = new Map([['male','Masculino'],['female','Femenino'],['non-binary','No binario'],['unknown','Desconocido']]);
const types = new Map([['home','Vivienda'],['business','Negocio'],['school','Centro educativo'],['park','Parque'],['plant','Planta industrial'],['landmark','Otro']]);
const roles = new Map([
 ['student','Estudiante'],['housewife','Ama de casa'],['homemaker','Responsable del hogar'],['teacher','Docente'],
 ['principal','Director escolar'],['school principal','Director escolar'],['superintendent','Superintendente'],
 ['police officer','Agente de policía'],['police chief','Jefe de policía'],['chief of police','Jefe de policía'],
 ['safety inspector','Inspector de seguridad'],['nuclear safety inspector','Inspector de seguridad nuclear'],
 ['nuclear power plant safety inspector','Inspector de seguridad nuclear'],['safety inspector at the springfield nuclear power plant','Inspector de seguridad nuclear'],
 ['bartender','Cantinero'],['bartender and owner of moe’s tavern','Propietario y cantinero'],
 ['shopkeeper','Comerciante'],['business owner','Empresario'],['owner of the springfield nuclear power plant','Propietario de la central nuclear'],
 ['assistant to mr. burns','Asistente del señor Burns'],['doctor','Médico'],['physician','Médico'],
 ['surgeon','Cirujano'],['mayor','Alcalde'],['clown','Payaso'],['television personality','Personalidad de televisión'],
 ['actor','Actor'],['actress','Actriz'],['musician','Músico'],['bus driver','Conductor de autobús'],
 ['scientist','Científico'],['engineer','Ingeniero'],['reverend','Reverendo'],['priest','Sacerdote'],
 ['retired','Jubilado'],['unemployed','Sin empleo'],['news anchor','Presentador de noticias'],
 ['criminal','Delincuente'],['lawyer','Abogado'],['accountant','Contador'],['journalist','Periodista'],
 ['nurse','Enfermero/a'],['chef','Cocinero/a'],['cashier','Cajero/a'],['farmer','Agricultor/a'],
 ['firefighter','Bombero/a'],['mechanic','Mecánico/a'],['salesperson','Vendedor/a'],
]);
const uses = new Map([
 ['bar','Bar'],['tavern','Taberna'],['school','Escuela'],['elementary school','Escuela primaria'],
 ['home','Vivienda'],['house','Casa'],['residence','Residencia'],['restaurant','Restaurante'],
 ['power plant','Central eléctrica'],['nuclear power plant','Central nuclear'],['hospital','Hospital'],
 ['church','Iglesia'],['store','Tienda'],['shop','Tienda'],['convenience store','Tienda de conveniencia'],
 ['office','Oficina'],['park','Parque'],['museum','Museo'],['library','Biblioteca'],['prison','Prisión'],
 ['police station','Comisaría'],['bar and tavern','Taberna'],['school building','Centro educativo'],
 ['supermarket','Supermercado'],['cemetery','Cementerio'],['stadium','Estadio'],['factory','Fábrica'],
 ['mall','Centro comercial'],['restaurant and bar','Restaurante y bar'],['government building','Edificio público'],
]);
const norm=value=>typeof value==='string'?value.trim().replace(/\s+/g,' ').toLocaleLowerCase('en'):'';
const label=(map,value)=>map.get(norm(value))||null;
export const estadoEs=value=>label(statuses,value);
export const generoEs=value=>label(genders,value);
export const tipoEs=value=>label(types,value);
export const ocupacionEs=value=>label(roles,value);
export const usoEs=value=>label(uses,value);
export function shortMetaEs(item,kind){
  if(kind==='character')return [ocupacionEs(item?.occupation),estadoEs(item?.status)].filter(Boolean).join(' · ');
  if(kind==='location')return [item?.town||null,usoEs(item?.use)].filter(Boolean).join(' · ');
  return Number.isInteger(item?.season)?`Temporada ${item.season}`:'';
}
/**
 * text=null significa que NO existe una reseña española verificable.
 * Nunca se sustituye una descripción inglesa por una paráfrasis inventada.
 */
export function descriptionEs(item,kind){
  const bio=verifiedBioEs(item,kind);
  if(bio)return {text:bio,source:'Contexto editorial en español · No es traducción de la API'};
  if(!Number.isInteger(item?.externalId)&&typeof item?.name==='string'){
    const original=editorialEs(item,kind);
    if(original)return {text:original,source:'Archivo editorial original · Independiente de la API'};
  }
  return {text:null,source:'Información estructurada de The Simpsons API'};
}
export function originalText(item,kind){
 return (kind==='episode'?item?.synopsis||item?.description:item?.description)||'';
}
export function verifiedFacts(item,kind){
  if(kind==='character')return [
    ['Ocupación',ocupacionEs(item?.occupation)],['Estado',estadoEs(item?.status)],
    ['Género',generoEs(item?.gender)],['Nacimiento',item?.birthdate||null],
  ].filter(([,v])=>v!=null&&v!=='');
  if(kind==='location')return [
    ['Localidad',item?.town||null],['Uso',usoEs(item?.use)],
  ].filter(([,v])=>v!=null&&v!=='');
  return [['Temporada',item?.season],['N.º global de capítulo',item?.episodeNumber],['Emisión',item?.airdate]].filter(([,v])=>v!=null&&v!=='');
}
