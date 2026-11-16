# Springfield Archive

Un proyecto de software desarrollado como **entrega académica pública y pieza de portafolio profesional**.

Springfield Archive se está diseñando y construyendo de forma incremental, poniendo el énfasis en la calidad del código, la seguridad, la reproducibilidad y las buenas prácticas de ingeniería. Este repositorio es la fuente de verdad del proyecto y se desarrolla en abierto.

> **Estado: fase inicial.** Todavía no existe código de la aplicación. Este repositorio contiene únicamente la inicialización del proyecto (licencia, reglas de ignorado y documentación). Las funcionalidades se documentarán aquí solo cuando existan y estén verificadas.

## De qué trata el proyecto

Springfield Archive explora el diseño de un archivo digital: organizar, describir y presentar material catalogado mediante una aplicación web mantenible. El alcance y las funcionalidades se definirán y documentarán a medida que avance el proyecto. No se presentarán funcionalidades como implementadas sin que hayan sido desarrolladas y verificadas.

## Aviso legal de propiedad intelectual

Springfield Archive es un **proyecto fan y académico no oficial**, desarrollado con fines educativos y de portafolio. **No está afiliado, autorizado, patrocinado ni respaldado por** The Walt Disney Company, 20th Century Studios, 20th Television, Gracie Films, Matt Groening ni ningún otro titular de derechos de *The Simpsons*.

«The Simpsons», sus personajes, nombres, títulos de episodios, imágenes, audio, guiones y marcas asociadas son propiedad de sus respectivos titulares. Nada de lo contenido en este repositorio otorga ni implica licencia alguna sobre propiedad intelectual de terceros, y en este repositorio no se distribuyen activos de terceros.

## Stack previsto y enfoque

Decisiones aprobadas hasta ahora:

- **Java 21** (OpenJDK)
- **Spring Boot** (previsto)
- **Maven Wrapper** — permite utilizar una versión definida de Maven sin requerir una instalación global.
- **PostgreSQL** ejecutándose en **Docker Compose** para desarrollo — el contenedor de la base de datos **no** se arrancará hasta que el desarrollo del backend lo necesite
- Entrega incremental: cambios pequeños y revisables, con verificación en cada paso

## Principios

- Calidad por encima de la velocidad
- Sin credenciales, tokens ni datos privados en el repositorio — nunca
- Entornos reproducibles mediante versiones de herramientas y dependencias controladas, y pasos de configuración documentados
- Cada cambio se verifica antes de publicarse

## Flujo de desarrollo

Este proyecto sigue un flujo incremental basado en la revisión: cada fase se propone, se revisa y se aprueba antes de su implementación. Consulta el historial de commits para ver la evolución del proyecto.

## Licencia

El código original de este proyecto se publica bajo la **Licencia MIT** — consulta [LICENSE](LICENSE).

La licencia MIT se aplica **exclusivamente al código original creado para este repositorio**. **No** otorga derecho alguno sobre The Simpsons — sus marcas, nombres, personajes, episodios, imágenes, audio, guiones — ni sobre ninguna otra propiedad intelectual de terceros. Este proyecto no concede ni implica licencia alguna sobre propiedad intelectual de terceros.
