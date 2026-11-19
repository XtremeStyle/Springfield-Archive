// Springfield Archive — movimiento progresivo, accesible y sin dependencias.
// El contenido permanece visible si JavaScript, IntersectionObserver o CSS moderno fallan.
// No ocultar tarjetas de catalogo: IntersectionObserver puede dejarlas en blanco
// durante scroll rapido, capturas de pagina o navegadores con ahorro de energia.
const TARGETS = 'main > section, .home-teaser, .stat, .duff-project, .learning-grid article, .ficha, .case-results, .source-panel';
let started = false;

export function prefersLessMotion() {
    return typeof window !== 'undefined' && typeof window.matchMedia === 'function'
        && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

export function initMotion() {
    if (started || typeof document === 'undefined') { return; }
    started = true;
    const reduced = prefersLessMotion();
    if (!reduced && typeof IntersectionObserver !== 'undefined') {
        const observer = new IntersectionObserver((entries) => {
            for (const entry of entries) {
                if (!entry.isIntersecting) { continue; }
                entry.target.classList.add('is-revealed');
                observer.unobserve(entry.target);
            }
        }, { rootMargin: '0px 0px -22px 0px', threshold: 0.06 });
        const observed = new WeakSet();
        const observe = (node, initial = false) => {
            if (!(node instanceof Element)) { return; }
            const elements = [node, ...node.querySelectorAll(TARGETS)].filter(e => e.matches(TARGETS));
            for (const el of elements) {
                if (observed.has(el)) { continue; }
                observed.add(el);
                // Nada de ocultar lo que ya está visible al entrar a la página.
                const visible = initial && el.getBoundingClientRect().top < innerHeight * 0.98;
                el.classList.add('reveal-ready');
                if (visible) { el.classList.add('is-revealed'); }
                else { observer.observe(el); }
            }
        };
        document.documentElement.classList.add('motion-enabled');
        observe(document.querySelector('main'), true);
        const main = document.querySelector('main');
        if (main && typeof MutationObserver !== 'undefined') {
            new MutationObserver(mutations => {
                for (const mutation of mutations) {
                    for (const node of mutation.addedNodes) { observe(node); }
                }
            }).observe(main, {childList: true, subtree: true});
        }
    }

    // Barra de lectura + acceso útil para volver al inicio en catálogos extensos.
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'back-to-top';
    button.textContent = '↑';
    button.setAttribute('aria-label', 'Volver al inicio de la página');
    button.title = 'Volver arriba';
    document.body.append(button);
    button.addEventListener('click', () => window.scrollTo({top: 0, behavior: reduced ? 'instant' : 'smooth'}));
    let scheduled = false;
    const update = () => {
        scheduled = false;
        const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
        const progress = maxScroll <= 0 ? 0 : Math.max(0, Math.min(1, window.scrollY / maxScroll));
        document.documentElement.style.setProperty('--read-progress', String(progress));
        button.classList.toggle('is-visible', window.scrollY > 500);
    };
    const schedule = () => {
        if (scheduled) { return; }
        scheduled = true;
        window.requestAnimationFrame(update);
    };
    window.addEventListener('scroll', schedule, {passive: true});
    window.addEventListener('resize', schedule, {passive: true});
    schedule();
}
