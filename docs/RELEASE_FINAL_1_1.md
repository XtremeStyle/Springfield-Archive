# Springfield Archive 1.1.0 — Informe de entrega

**18 de noviembre de 2026 · José Manuel Valencia Gallego / XtremeStyle**

## Cambio de producto

Se mantienen el estilo retro-tecnológico, las fichas y la integración Java. Se eliminan componentes redundantes de la portada y la barra de audio del portafolio Duff.

**Portada:** los tres bloques de personajes, episodios y lugares dejaron de ser listas pasivas. Cada uno permite descubrir otra ficha real, abrirla para investigarla, guardarla en favoritos y entrar al catálogo completo. Los datos se consultan desde Java/PostgreSQL y se evita repetir la misma ficha durante la exploración en curso. La portada conserva estadísticas y estados de sincronización, pero reduce contenido promocional repetido.

**Springfield Files:** se conserva el Caso 742 y su estilo narrativo, pero se amplía a seis fases con mecánicas diferenciadas: tres actos de investigación, detección de un expediente ajeno a las pruebas, ordenación de actas por su ID externo y cifrado final. El cifrado usa la suma de los dígitos de cada ID más 7, 4 o 2, en orden ascendente. Si hay IDs iguales, se desempata por Persona → Lugar → Episodio. Las consecuencias descuentan integridad; no se puede continuar cuando se agota. El juego no inventa relaciones del universo de la serie y no simula un ranking verificado por servidor.

**Duff:** se eliminó el laboratorio/barra de volumen que interrumpía el portafolio. Se conserva el interruptor de audio en cabecera, apagado de inicio. Se regeneró localmente un WAV estéreo de menos de un segundo: anilla, apertura breve y escape de gas. No hay reproducción automática, voces, canciones ni muestras remotas.

## Pruebas y límites

- `npm --prefix frontend test`: pruebas unitarias JavaScript, incluidos los nuevos casos de descubrimientos, auditoría, ordenación y cifrado.
- `npm --prefix frontend run validate`: 12 documentos HTML, recursos y semántica estática.
- `python3 -m unittest discover -s scripts -p 'test_*.py'`: lanzador, contratos y auditoría básica del paquete.
- `bash scripts/test-java-security.sh`: comprobaciones Java de seguridad independientes de Maven.
- `python3 scripts/qa_final_browser.py`: tres partidas de seis fases a 1366, 390 y 320 px, prueba de penalizaciones, fichas, portada, favoritos y Duff, con **API simulada**.
- `python3 scripts/qa_failure_browser.py`: se prueba que agotarse la integridad impide ganar y permite reiniciar.

**Pendiente de certificación de extremo a extremo:** Maven/Testcontainers, Flyway/PostgreSQL y sincronización real desde Omarchy, así como el despliegue HTTPS autorizado en proveedor gratuito. No confundir la simulación de navegador con la conexión a la base de datos de producción.

## Puesta en marcha

Desde la raíz: `./INICIAR_SPRINGFIELD.run iniciar`. En otra terminal: `./INICIAR_SPRINGFIELD.run sincronizar`, `./INICIAR_SPRINGFIELD.run estado` y `python3 scripts/verify-apis.py --local --require-synced`. No borrar volúmenes PostgreSQL ni sobrescribir el `.env` de una instalación anterior.

No se hizo push ni se desplegó. La foto y el contacto públicos no se han incorporado porque el autor todavía no los ha proporcionado.
