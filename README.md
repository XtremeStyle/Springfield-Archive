# Springfield Archive

Soy **José Manuel Valencia Gallego (XtremeStyle)**. Desarrollé Springfield Archive como proyecto de aprendizaje de **Aulas de Ciudad**, para poner en práctica Java, programación orientada a objetos, bases de datos, JavaScript, diseño web y el consumo de una API pública.

La idea es explorar Springfield de una forma distinta: consultar personajes, lugares y episodios, guardar favoritos, jugar **Springfield Files** y conocer algunos de mis proyectos en la sección **Sobre mí**.

## ¿Cómo funciona?

- **Frontend:** HTML, CSS y JavaScript con módulos ES.
- **Backend:** Java 21, Spring Boot y API REST propia.
- **Base de datos:** PostgreSQL y migraciones Flyway.
- **Datos externos:** [The Simpsons API](https://thesimpsonsapi.com/).

Los datos se importan desde la API pública, se guardan en PostgreSQL y mi frontend los consulta a través del backend Java. No se inventan personajes ni relaciones para completar los resultados.

## Cómo ejecutarlo en Omarchy / Linux

Necesitas **Java 21, Docker con Compose y conexión a Internet** para descargar Maven la primera vez.

```bash
./INICIAR_SPRINGFIELD.run iniciar
```

Abre `http://127.0.0.1:8080`. Después, desde otra terminal:

```bash
./INICIAR_SPRINGFIELD.run sincronizar
./INICIAR_SPRINGFIELD.run estado
```

Antes de entregarlo también puedes comprobar:

```bash
npm --prefix frontend test
npm --prefix frontend run validate
cd backend && ./mvnw -B verify
```

La última prueba necesita Docker activo. La sincronización debe terminar en `completed` para los tres catálogos.

## Publicación

El frontend está preparado para **Netlify**, pero requiere un **backend Java publicado por HTTPS** y una base PostgreSQL disponible. Consulta **[SUBIR_A_GITHUB_Y_NETLIFY.md](SUBIR_A_GITHUB_Y_NETLIFY.md)** para los pasos y variables de entorno.

Mi perfil: [github.com/XtremeStyle](https://github.com/XtremeStyle). Algunos proyectos del portafolio son colaborativos y se reconocen con sus respectivos créditos.

Proyecto académico independiente, no afiliado a Los Simpson. Datos e imágenes externos: derechos de sus titulares; atribuciones en [docs/ATTRIBUTIONS.md](docs/ATTRIBUTIONS.md). Código propio bajo licencia MIT.
