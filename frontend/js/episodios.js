// The Simpsons API provides a series-wide episode number, not a within-season index.
import {initList} from './list.js';
initList({
    endpoint:'/episodes', kind:'episode',
    emptyText:'No se encontraron episodios con esos criterios.',
});
