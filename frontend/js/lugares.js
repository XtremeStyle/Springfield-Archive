// Springfield Archive — Lugares
import { initList } from './list.js';
initList({
    endpoint: '/locations',
    kind: 'location',
    emptyText: 'Ningún lugar coincide con la búsqueda.',
    cardSubtitle: (l) => [l.town, l.use].filter(Boolean).join(' · '),
});
