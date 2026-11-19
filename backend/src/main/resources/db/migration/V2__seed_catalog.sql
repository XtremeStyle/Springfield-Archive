-- V2__seed_catalog.sql
-- Carga editorial inicial del catálogo de Springfield Archive.
-- Datos redactados de forma original, sin copia de textos protegidos.
-- Inserciones explícitas y deterministas: 40 personajes y 20 lugares.

INSERT INTO characters (slug, name, occupation, status, description) VALUES
-- Familia Simpson (5)
('homer-simpson', 'Homer Simpson', 'Inspector de seguridad en la central nuclear de Springfield', 'Alive', 'Patriarca de la familia Simpson, vecino de Evergreen Terrace conocido por su visceral apetito, desgana y lealtad familiar. Trabaja como inspector de seguridad en la planta nuclear, un puesto que ha ejercido con un descuido legendario durante décadas.'),
('marge-simpson', 'Marge Simpson', 'Ama de casa', 'Alive', 'Esposa de Homer, madre de Bart, Lisa y Maggie, y columna emocional de la familia. Su característico flequillo azul y su tolerancia infinita la han convertido en una de las figuras más reconocibles de todo el universo de Springfield.'),
('bart-simpson', 'Bart Simpson', 'Estudiante de la escuela primaria de Springfield', 'Alive', 'El hijo mayor de la familia, once a doce años, incorregible en clase y genial en la calle. Sus travesuras han sembrado el caos en la primaria durante casi toda su vida escolar.'),
('lisa-simpson', 'Lisa Simpson', 'Estudiante de la escuela primaria de Springfield', 'Alive', 'La hija intelectual de los Simpson: precoz, musical, ecologista y con un espíritu cívico fuera de serie. Toca el saxofón, lee por encima de su edad y es la conciencia moral de la serie.'),
('maggie-simpson', 'Maggie Simpson', NULL, 'Alive', 'La bebé de la familia Simpson. Apenas habla; se comunica con su característico chupete, gestos y un par de escapadas memorables que han quedado en el imaginario colectivo.'),

-- Familiares y círculo cercano (5)
('abraham-simpson', 'Abraham Simpson', 'Pensionista', 'Alive', 'Padre de Homer y patriarca del legado singels. Viejo veterano recluido en la residencia de ancianos de Springfield; sus recuerdos de la guerra y de la vida antes del boom industrial suelen ser cualquier cosa menos fiables.'),
('mona-simpson', 'Mona Simpson', NULL, 'Alive', 'Madre de Homer y esposa de Abraham. En el pasado una activista medioambiental perseguida por sus acciones directas, reapareció en las vidas de los Simpson cuando su familia más la necesitaba.'),
('patty-bouvier', 'Patty Bouvier', 'Empleada del Departamento de Vehículos de Motor', 'Alive', 'Hermana gemela de Selma y tía de Bart, Lisa y Maggie. Cínica, seca y colaboradora veterana de Vehículos de Motor, donde su interés por renovar los permisos es prácticamente nulo.'),
('selma-bouvier', 'Selma Bouvier', 'Empleada del Departamento de Vehículos de Motor', 'Alive', 'Hermana gemela de Patty. Compañera de mostrador, madre adoptiva de Ling y una de las personas más resignadas de Springfield; con el tiempo ha ido flexibilizando sus exigencias.'),
('ling-bouvier', 'Ling Bouvier', NULL, 'Alive', 'Hija adoptiva de Selma y parientes lejanas de los Simpson tras una ceremonia improvisada en China y una gestión caótica que Selma siempre prefiere olvidar.'),

-- Amigos y compañeros (8)
('ned-flanders', 'Ned Flanders', 'Dueño de Leftorium', 'Alive', 'El vecino ideal sobre el papel: profundamente religioso, sonriente y sospechosamente educado. Dirige Leftorium, una tienda especializada en artículos para zurdos, y es padre de Rod y Todd.'),
('maude-flanders', 'Maude Flanders', NULL, 'Deceased', 'Esposa de Ned, madre de Rod y Todd y voz amable de la casa Flanders. Su fallecimiento fue un punto de inflexión para la familia y para todo el vecindario de Evergreen Terrace.'),
('milhouse-van-houten', 'Milhouse Van Houten', 'Estudiante de la escuela primaria de Springfield', 'Alive', 'Mejor amigo de Bart desde la escuela, nieto de Kirk y Luann, brillante y miope. Su flequillo y sus gafas son tan icónicos como su lealtad a Bart incluso cuando los planes son malos.'),
('barney-gumble', 'Barney Gumble', NULL, 'Alive', 'Ex estrella del instituto y compañero de barra de Moe. Su relación con la bebida es legendaria en Springfield; dejó el alcohol en ocasiones importantes con resultados dispares.'),
('lenny-leonard', 'Lenny Leonard', 'Técnico de la central nuclear', 'Alive', 'Compañero de Homer y Carl en Sector 7-G. Uno de los pocos valores seguros del personal técnico de la planta nuclear, aunque sus opiniones laborales rara vez lleguen a alguna parte.'),
('carl-carlson', 'Carl Carlson', 'Técnico de la central nuclear', 'Alive', 'El tercer integrante del trío de Sector 7-G y compañero fiel de Lenny. Más contenido y reflexivo que Homer, que siempre encuentra motivos para gastar bromas con los datos.'),
('jimbo-jones', 'Jimbo Jones', 'Estudiante de la escuela primaria de Springfield', 'Alive', 'Uno de los matones del instituto, compañero de los gimansios de los Simpson y amigo de Kearney y Dolph. Suele aparecer como una amenaza ruidosa pero siempre derrotado por la lógica.'),
('seymour-skinner', 'Seymour Skinner', 'Director de la escuela primaria de Springfield', 'Alive', 'Director militar de la escuela primaria de Springfield, veterano estricto y nervioso con una obsesión por la disciplina. Su pasado como soldado marca cada una de sus decisiones.'),

-- Personalidades de Springfield (10)
('charles-montgomery-burns', 'Charles Montgomery Burns', 'Propietario de la central nuclear de Springfield', 'Alive', 'El anciano y millonario propietario de la central nuclear de Springfield y de prácticamente todo negocio que tenga algo de valor en la ciudad. Frío, calculador y con un sentido del humor que recuerda más al del siglo XIX que al del XXI.'),
('waylon-smithers', 'Waylon Smithers', 'Asistente ejecutivo de Burns', 'Alive', 'Mano derecha y subordinado de Burns. Devoto de su figura, educado, metódico y aparentemente el único que consigue mantener en pie la operación de la planta la mayoría de los días.'),
('krusty-the-clown', 'Krusty the Clown', 'Presentador de televisión', 'Alive', 'El payaso más famoso de Springfield y estrella del canal Krusty. Un ídolo infantil con una vida personal que no encaja precisamente en el arquetipo de héroe juvenil.'),
('apu-nahasapeemapetilon', 'Apu Nahasapeemapetilon', 'Dueño del Kwik-E-Mart', 'Alive', 'Emprendedor e inmigrante que regenta el Kwik-E-Mart, el único supermercado abierto a todas horas de la ciudad, popular por su carácter servicial y sus promociones impredecibles.'),
('moe-szyslak', 'Moe Szyslak', 'Dueño y bartender de la Taberna de Moe', 'Alive', 'Propietario de la Taberna de Moe, bar de toda la vida frecuentado por Homer y su círculo. Moe se mueve entre la amargura y la entrega a sus clientes más leales, algo que nunca llega a admitir.'),
('edna-krabappel', 'Edna Krabappel', 'Profesora de la escuela primaria de Springfield', 'Deceased', 'Maestra de 4.º grado a la que muchos alumnos temían por su apatía y su sarcasmo. Durante años ejerció como una fuerza del orden en la primaria pese a no valorar mucho su propia profesión.'),
('groundskeeper-willie', 'Groundskeeper Willie', 'Conserje de la escuela primaria de Springfield', 'Alive', 'Escocés, temperamental y fieramente patriótica con la escuela a su cargo. Su acento, su personalidad y sus descuidadas chanzas deportivas son sellos inconfundibles de Springfield.'),
('otto-mann', 'Otto Mann', 'Conductor del autobús escolar', 'Alive', 'Chófer del autobús escolar y seguidor apasionado del heavy metal. Vive metido en su furgoneta de gira y de concierto; el entorno escolar es más bien un mero accidente laboral.'),
('timothy-lovejoy', 'Timothy Lovejoy', 'Reverendo de la Iglesia de Springfield', 'Alive', 'Pastor de la iglesia de Springfield. Pragmático y servicial cuando conviene, suele predicar con más énfasis en el deber cívico que en algo devastadoramente espiritual.'),
('helen-lovejoy', 'Helen Lovejoy', 'Ama de casa', 'Alive', 'Esposa de Timothy y amiga íntima de Marge. Gran defensora de la moral pública en Springfield, actúa como la voz estridente del vecindario en asuntos vecinales.'),

-- Autoridades y profesionales (6)
('chief-wiggum', 'Clancy Wiggum', 'Jefe de policía de Springfield', 'Alive', 'Jefe de policía de la ciudad y padre de Ralph. Bienintencionado, habitualmente incompetente y aun así el más fiable dentro del departamento de seguridad local.'),
('inspector-chalmers', 'Superintendente Chalmers', 'Inspector municipal', 'Alive', 'Inspector de alto rango encargado de controlar la educación y los servicios públicos de Springfield. Sus visitas son la pesadilla profesional del director Skinner.'),
('julius-hibbert', 'Julius Hibbert', 'Pediatra', 'Alive', 'Médico del hospital general de Springfield, con una sonrisa encantadora y una tendencia a ofrecer diagnósticos tan inimaginables como sus honorarios.'),
('marvin-monroe', 'Marvin Monroe', 'Psiquiatra', 'Alive', 'El psicólogo de Springfield de confianza pública para situaciones límite. Entre radioterapias, hiphoterapia y tarjetas por correo, su consulta siempre tiene un nuevo regalo para ofrecer.'),
('joe-quimby', 'Joe Quimby', 'Alcalde de Springfield', 'Alive', 'Alcalde de la ciudad. Bien conectado con políticos de todo tipo y maestro de la burocracia expansiva; su nombre real es desconocido para la mayoría y su historial está lleno de oscuras mañanas electorales.'),
('constance-harm', 'Constance Harm', 'Jueza', 'Alive', 'Jueza del tribunal municipal de Springfield, conocida por su rigor y por sentencias que nunca dejan a nadie indiferente. Una presencia dominadora en la sala de justicia.'),

-- Otros personajes secundarios (6)
('sideshow-bob', 'Sideshow Bob', 'Actor y ex asistente de Krusty', 'Alive', 'Antiguo ayudante de Krusty y clásico antagonista de la saga. Culto, ambicioso y poseedor de una voz operística inolvidable, que combina cultura académica con mala intención.'),
('sideshow-mel', 'Sideshow Mel', 'Asistente de Krusty', 'Alive', 'Nuevo ayudante de Krusty tras la caída de Bob. Más simpático y marginal, pero igual de desafortunado con los trabajos que le da la estrella del espectáculo.'),
('cletus-spuckler', 'Cletus Spuckler', 'Granjero', 'Alive', 'Habitante rural de Springfield y representante máximo de la lincolntown tradicional. De personalidad sencilla y familia amplia, sobrevive a merced de la agricultura y los imprevistos del campo.'),
('lionel-hutz', 'Lionel Hutz', 'Abogado', 'Alive', 'El abogado más notorio de Springfield. Hábil argumentando frente a jueces distraídos y eficaz contra demandas de cualquier tipo, especialmente las que ocurren en el bufete de la ciudad.'),
('rod-flanders', 'Rod Flanders', 'Estudiante de la escuela primaria de Springfield', 'Alive', 'El mayor de los gemelos Flanders. Más reservado y obediente que su hermano Todd; sus intereses religiosos y académicos suelen ser una extensión natural de su padre.'),
('todd-flanders', 'Todd Flanders', 'Estudiante de la escuela primaria de Springfield', 'Alive', 'El menor de los gemelos Flanders. Más sociable y nervioso que Rod; su comportamiento en clase le causa alegrías y palpitaciones a su madre Maude antes de que ella falleciera.');

INSERT INTO locations (slug, name, type, description) VALUES
-- Residenciales (3)
('simpson-house', 'Casa Simpson', 'home', 'Residencia de la familia Simpson en el número 742 de Evergreen Terrace. El epicentro de los eventos más caóticos del barrio y uno de los hogares más famosos de la animación.'),
('flanders-house', 'Casa Flanders', 'home', 'Hogar de la familia Flanders, justo enfrente de los Simpson. Ordenadísimo, decorado con gusto conservador y siempre con la nevera llena de productos ecológicos y bautizados.'),
('van-houten-house', 'Casa Van Houten', 'home', 'Residencia de la familia Van Houten en el mismo barrio de Evergreen Terrace. Lugar de origen de los mayores problemas sociales del pequeño y devoto Milhouse.'),

-- Comerciales y gastronómicos (6)
('moe-s-tavern', 'Taberna de Moe', 'business', 'El bar clásico de Springfield y refugio de Homer desde hace décadas. Ambientada con alma de cantina de pueblo, cerveza con historia y una decoración que los clientes habituales ya no se molestan en juzgar.'),
('kwik-e-mart', 'Kwik-E-Mart', 'business', 'Ultramarinos regentado por Apu, punto de referencia del vecindario por su servicio continuo y su surtido invariable. La clientela local lo valora tanto por su disponibilidad como por su personalidad.'),
('krusty-burger', 'Krusty Burger', 'business', 'Cadena de comida rápida fundada por Krusty. Famosa por su publicidad engañosa, su catálogo infinito de productos imposibles y la devoción inquebrantable de sus fans más jóvenes.'),
('leftorium', 'Leftorium', 'business', 'La tienda de Ned Flanders especializada en artículos para zurdos. Un nicho de mercado atendido con dedicación religiosa y el punto de referencia local para cualquier artículo zurdo que no encuentres en otro sitio.'),
('springfield-mall', 'Springfield Mall', 'business', 'El gigante comercial de Springfield, centrado en grandes superficies, restaurantes y entretenimiento. Punto de encuentro de todo el vecindario para cualquier ocasión consumista.'),
('lard-lad-donuts', 'Lard Lad Donuts', 'business', 'Rosquellería clásica de la ciudad, famosa por sus DNLs y su carácter pecaminoso. Un orgullo gastronómico local que siempre encuentra el momento justo para una exageración.'),

-- Institucionales y educativos (5)
('springfield-elementary', 'Escuela primaria de Springfield', 'school', 'La escuela elemental del barrio. En ella coinciden niños de la más diversa procedencia social bajo la estricta y nerviosa dirección del señor Skinner.'),
('town-hall', 'Ayuntamiento de Springfield', 'landmark', 'Sede del poder municipal. Lugar donde se resuelven las decisiones políticas de la ciudad entre discursos encubiertos y votos sorpresivos.'),
('springfield-general-hospital', 'Hospital general de Springfield', 'landmark', 'Centro sanitario principal de la ciudad. Dirigido por administradores que buscan eficiencia por encima de todo y con médicos que ofrecen diagnósticos tan variados como su rango de salarios.'),
('police-station', 'Cuartel de la policía de Springfield', 'landmark', 'Cuartel general del Departamento de Policía de Springfield. Famoso por su burocracia imparable y por las frecuentes intervenciones espontáneas de la fuerza local.'),
('first-church-of-springfield', 'Iglesia de Springfield', 'landmark', 'Templo religioso de la comunidad liderado por el reverendo Lovejoy. Punto de encuentro vecinal para celebraciones oficiales y charlas sobre civismo religioso.'),

-- Industriales y laborales (2)
('springfield-nuclear-power-plant', 'Central nuclear de Springfield', 'plant', 'Planta de energía que suministra electricidad a Springfield. Operada por una plantilla mínima bajo la supervisión de Burns y smithers. El símbolo industrial más peligroso de la ciudad.'),
('duff-brewery', 'Cervecería Duff', 'plant', 'Cervecería situada a las afueras de Springfield y punto de peregrinación para cualquier bebedor de la ciudad. Única productora del consumo masivo de cerveza Duff en la región.'),

-- Recreativos y públicos (4)
('springfield-downs', 'Hipódromo de Springfield', 'landmark', 'Hipódromo local consagrado a las carreras de caballos y a las apuestas clandestinas. Punto de encuentro dominical para la gente con más fe que estrategia.'),
('androids-dungeon-arcade', 'Android''s Dungeon Arcade', 'landmark', 'Galería de la calle principal donde Bart trabaja como experto en pinball, donde la nostalgia miope encuentra salida en el pinball clásico. Un clásico de la cultura videojueguil de la ciudad.'),
('springfield-museum', 'Museo de Springfield', 'landmark', 'Museo municipal dedicado a la cultura local y a la historia de la ciudad. Lugar de visitas familiares obligatoria y con exposiciones de divulgación sorprendentemente completas.'),
('springfield-zoo', 'Zoo de Springfield', 'landmark', 'Zoo municipal con colecciones de animales de todos los continentes. Un referente de ocio familiar y de debate vecinal sobre financiación pública.');
