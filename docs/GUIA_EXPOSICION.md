# Guion de demostración | Springfield Archive 1.0

La presentación propuesta dura entre 5 y 7 minutos. **Sustituye los resultados de ejemplo por datos observados en Omarchy**; no presentes pruebas simuladas como acceso real a PostgreSQL.

## Apertura (40 segundos)

Springfield Archive no es un catálogo estático ni una página montada directamente sobre la API externa: importa, conserva, consulta y presenta datos reales de personajes, episodios y lugares mediante Java y PostgreSQL. Su interfaz simula un archivo municipal cómico.

## Arquitectura (60 segundos)

Abrir el diagrama del README. Explicar: The Simpsons API -> cliente Java -> importador con checkpoints -> PostgreSQL y Flyway -> API REST `/api/v1` -> JS ES Modules. Mostrar estado `completed` solo si el backend lo confirma. Explicar qué ocurre con registros fallidos y por qué la web sigue funcionando durante una interrupción del proveedor.

## Navegación y datos (90 segundos)

1. En el inicio mostrar las estadísticas reales y el estado de sincronización.
2. Buscar un personaje, abrir ficha e inspeccionar su imagen, procedencia y datos; guardar como favorito.
3. Buscar un lugar; mostrar el filtro y su ficha sin registros duplicados editoriales.
4. Navegar por temporadas de episodios; aclarar que `episode_number` es el ordinal global de la serie.
5. Explicar que la API está principalmente en inglés: se tradujeron etiquetas estructuradas y se atribuyen los textos originales; no se inventaron biografías automáticas.

## Springfield Files (90 segundos)

Abrir **Caso 742**. Explicar que hay **seis fases jugables**: tres actos que recuperan un personaje, un lugar y un episodio; una auditoría donde se retira un documento que no pertenece al caso; una mesa que ordena los tres ID externos; y una caja fuerte cuyo código utiliza la suma de los dígitos de cada ID y el sello **742**. Mostrar la inspección de candidatos, la penalización de intentos erróneos y una elección real. Los datos proceden de Java/PostgreSQL; el argumento y el juego son ficción original, **no** relaciones narrativas ofrecidas por The Simpsons API.

## Duff y calidad (60 segundos)

Ir a **Sobre mí**, abrir una lata Duff con audio voluntario, mostrar la descripción y el enlace de un proyecto propio y los créditos de los trabajos colaborativos. Indicar diseño responsive, teclado, reducción de movimiento, 404, estados de error, seguridad y pruebas en CI.

## Preguntas técnicas probables

- **¿Por qué Java?** Persistencia, independencia del proveedor, paginación y seguridad de importación.
- **¿Qué hay realmente en la base de datos?** Los registros externos se identifican por `external_id`; las semillas editoriales se conservan separadas.
- **¿Evitan inyecciones?** Consultas parametrizadas JDBC y validación de filtros.
- **¿Y XSS?** Escape de texto, rutas de medios validadas y CSP.
- **¿La puntuación es inviolable?** No; es una experiencia recreativa con estado local. No se finge una clasificación de servidor.
- **¿Se ven capítulos completos?** La API aporta metadatos, no una fuente de vídeo con licencia de reproducción; no hay streaming no autorizado.
- **¿Por qué no React/Tailwind?** JS modular y CSS propio cubren las necesidades reales con menos complejidad.

## Comprobaciones antes de exponer

```bash
./INICIAR_SPRINGFIELD.run estado
python3 scripts/verify-apis.py --local --require-synced
cd backend && ./mvnw -B verify
```

Registrar el resultado real de Omarchy antes de declarar aprobada la versión 1.0.
