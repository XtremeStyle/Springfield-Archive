/** Datos públicos revisados para la entrega del 18 de noviembre de 2026.
 * photoUrl: imagen WebP local (ej. /assets/profile/jose.webp), o null para mostrar monograma.
 * contactEmail: correo de contacto que se desee hacer público; por defecto null.
 */
export const publicProfile = Object.freeze({
    name: 'José Manuel Valencia Gallego',
    alias: 'XtremeStyle',
    photoUrl: '/assets/profile/jose-manuel.webp',
    contactEmail: null,
    github: 'https://github.com/XtremeStyle',
});

// Datos editoriales del portafolio. Revisar firmas de autoría antes de publicar.
// La cuenta GitHub XtremeStyle aparece en los materiales proporcionados.
export const projects = Object.freeze([
    {
        id: 'springfield',
        title: 'Springfield Archive',
        label: 'Archivo Springfield',
        category: 'Proyecto central',
        description: 'Enciclopedia interactiva con Java, Spring Boot, JavaScript modular, PostgreSQL y datos de The Simpsons API.',
        credit: 'Desarrollado por José Manuel Valencia Gallego (XtremeStyle).',
        href: 'https://github.com/XtremeStyle/Springfield-Archive',
    },
    {
        id: 'laboratorio',
        title: 'Laboratorio Académico',
        label: 'Laboratorio',
        category: 'Proyecto de aprendizaje',
        description: 'Plataforma académica con misiones, entregas y seguimiento de progreso. Implementada con Java y Spring Boot.',
        credit: 'Desarrollado por José Manuel Valencia Gallego (XtremeStyle).',
        href: 'https://github.com/XtremeStyle/laboratorio-academia',
    },
    {
        id: 'aulas',
        title: 'Proyecto Aulas de Ciudad',
        label: 'Aulas de Ciudad',
        category: 'Trabajo compartido',
        description: 'Repositorio académico de actividades realizadas en el proceso de formación.',
        credit: 'Trabajo colaborativo con Miguel Ángel Pérez García y José Manuel Valencia Gallego; crédito compartido.',
        href: 'https://github.com/peres12478/Proyecto-Aulas-de-Ciudad',
    },
    {
        id: 'museo',
        title: 'Viaje interestelar',
        label: 'Viaje interestelar',
        category: 'Trabajo compartido',
        description: 'Concepto de museo digital interestelar con interacción por gestos mediante Python y OpenCV.',
        credit: 'Proyecto de Miguel Ángel Pérez García y José Manuel Valencia Gallego, según la descripción pública del repositorio.',
        href: 'https://github.com/peres12478/Viaje_interestelar',
    },
]);
