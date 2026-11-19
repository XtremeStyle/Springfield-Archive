/** Contextos editoriales BREVES, de autoría propia; no son traducciones de la API.
 * Solo se aplican a nombres inequívocos muy conocidos de la serie.
 * Para todos los demás registros se presentan únicamente datos estructurados.
 */
const norm = name => String(name || '').toLocaleLowerCase('es').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, ' ').trim();
const CHARACTERS = new Map(Object.entries({
  'homer simpson':'Trabaja en la central nuclear de Springfield y vive con Marge, Bart, Lisa y Maggie. Su vida familiar y sus decisiones impulsivas son centrales en la serie.',
  'marge simpson':'Es la madre de Bart, Lisa y Maggie y pareja de Homer. Suele ser la voz sensata del hogar, aunque también tiene sus propias historias e intereses.',
  'bart simpson':'Hijo mayor de Homer y Marge. Estudia en la Escuela Primaria de Springfield y es conocido por sus bromas y conflictos con la autoridad.',
  'lisa simpson':'Hija de la familia Simpson, estudia en la Escuela Primaria de Springfield. Destaca por su curiosidad, su interés por la música y sus convicciones.',
  'maggie simpson':'Es la hija menor de la familia Simpson. Habitualmente aparece con su chupete y se comunica sin apenas palabras.',
  'abraham simpson':'Es el padre de Homer y abuelo de Bart, Lisa y Maggie. Sus recuerdos, historias exageradas y visitas familiares forman parte de la vida de Springfield.',
  'ned flanders':'Vecino de la familia Simpson y padre de Rod y Todd. Es conocido por su religiosidad, su cordialidad y su relación peculiar con Homer.',
  'moe szyslak':'Atiende la taberna de Moe, frecuentada por Homer y sus amigos. Su establecimiento es un punto de encuentro habitual de Springfield.',
  'krusty the clown':'Payaso y presentador televisivo del universo de Springfield. Su imagen pública contrasta a menudo con los problemas de su vida profesional.',
  'seymour skinner':'Director de la Escuela Primaria de Springfield. Su trabajo lo enfrenta repetidamente a las bromas de Bart y a las exigencias de la administración educativa.',
  'milhouse van houten':'Amigo cercano de Bart y compañero de escuela. Participa en muchas de sus aventuras y suele quedar atrapado en sus consecuencias.',
  'ralph wiggum':'Alumno de la Escuela Primaria de Springfield e hijo del jefe Wiggum. Es conocido por sus observaciones inesperadas.',
  'charles montgomery burns':'Propietario de la central nuclear de Springfield y jefe de Homer. Representa el poder económico de la ciudad y suele actuar guiado por sus propios intereses.',
  'waylon smithers jr':'Es el asistente y colaborador más cercano del señor Burns, con quien trabaja en la central nuclear de Springfield.',
  'apu nahasapeemapetilon':'Es conocido por atender el Kwik-E-Mart, un comercio frecuentado por los habitantes de Springfield.',
  'chief clancy wiggum':'Es el jefe de policía de Springfield y padre de Ralph. Su manera de trabajar aporta buena parte del humor de las historias policiales.',
}));
const LOCATIONS = new Map(Object.entries({
  'moes tavern':'Taberna de Springfield atendida por Moe Szyslak, frecuentada por Homer y otros vecinos. Es un lugar de reunión recurrente de la serie.',
  'springfield nuclear power plant':'Central nuclear propiedad del señor Burns. Homer trabaja allí y numerosos episodios exploran sus riesgos y su peculiar administración.',
  'kwik e mart':'Tienda de conveniencia de Springfield asociada a Apu. Es escenario frecuente de encuentros entre los personajes.',
  'springfield elementary school':'Escuela donde estudian Bart y Lisa, dirigida por Seymour Skinner. A lo largo de la serie aparecen sus profesores, alumnos y rutinas escolares.',
  'simpson house':'Hogar de Homer, Marge, Bart, Lisa y Maggie en Evergreen Terrace. Es uno de los principales escenarios de la serie.',
  'moe s tavern':'Taberna de Springfield atendida por Moe Szyslak, frecuentada por Homer y otros vecinos.',
}));
export function verifiedBioEs(item, kind){
  // La reseña no se confunde con la API y nunca altera el dato del proveedor.
  if(!Number.isInteger(item?.externalId) || item.externalId<=0)return null;
  const key=norm(item.name);
  return (kind==='character'?CHARACTERS:kind==='location'?LOCATIONS:new Map()).get(key)||null;
}
