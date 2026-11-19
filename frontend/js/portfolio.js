import { mountChrome, escapeHtml, escapeAttr } from './components.js';
import { projects, publicProfile } from './portfolio-data.js';
import {emitSound} from './sound.js';

mountChrome('sobre-mi');
// La fotografía del autor se sirve estáticamente desde sobre-mi.html
// (/assets/profile/jose-manuel.webp); no hay rama dinámica que mantener.
if (publicProfile.contactEmail && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(publicProfile.contactEmail)) {
    const strip = document.querySelector('.contact-strip');
    const a = document.createElement('a');
    a.className = 'cta';
    a.href = `mailto:${publicProfile.contactEmail}`;
    a.textContent = 'Enviar correo ↗';
    strip?.append(a);
}


const root = document.getElementById('projects');
const safeHref = (href) => {
    try {
        const url = new URL(href);
        return url.protocol === 'https:' && url.hostname === 'github.com' ? url.href : '';
    } catch (_) { return ''; }
};

root.innerHTML = projects.map((project, index) => `
    <article class="duff-project" data-project="${escapeAttr(project.id)}">
      <span class="project-kind">${escapeHtml(project.category)} · expediente ${String(index + 1).padStart(2, '0')}</span>
      <button type="button" class="duff-can" aria-expanded="false" aria-controls="detail-${escapeAttr(project.id)}"
          aria-label="Abrir lata para ver ${escapeAttr(project.title)}">
        <span class="can-top" aria-hidden="true"><span class="can-tab"></span></span>
        <span class="can-body" aria-hidden="true">
          <span class="can-brand">DUFF</span>
          <span class="can-sub">PORTFOLIO · ${String(index + 1).padStart(2, '0')}</span>
        </span>
        <span class="can-bottom" aria-hidden="true"></span>
        <span class="can-fizz" aria-hidden="true">
          ${Array.from({length: 7}, () => '<i></i>').join('')}
        </span>
      </button>
      <div class="project-detail" id="detail-${escapeAttr(project.id)}" hidden>
        <h3>${escapeHtml(project.title)}</h3>
        <p>${escapeHtml(project.description)}</p>
        <p class="project-credit">${escapeHtml(project.credit)}</p>
        <a class="project-link" href="${escapeAttr(safeHref(project.href))}" target="_blank" rel="noopener noreferrer">Ver repositorio en GitHub <span aria-hidden="true">↗</span></a>
      </div>
      <p class="can-hint">${escapeHtml(project.label)} · Pulsa para destapar</p>
    </article>
`).join('');

root.addEventListener('click', (event) => {
    const trigger = event.target.closest('button.duff-can');
    if (!trigger) { return; }
    const card = trigger.closest('.duff-project');
    const detail = card?.querySelector('.project-detail');
    if (!detail) { return; }
    const willOpen = trigger.getAttribute('aria-expanded') !== 'true';
    // Solo una lata abierta a la vez; conservamos foco y semántica nativa.
    root.querySelectorAll('.duff-project').forEach((item) => closeCan(item));
    emitSound(willOpen?'open':'close');
    if (willOpen) {
        card.classList.add('is-open');
        trigger.setAttribute('aria-expanded', 'true');
        trigger.setAttribute('aria-label', `Cerrar lata de ${card.querySelector('.project-detail h3')?.textContent || 'proyecto'}`);
        detail.hidden = false;
    }
});

function closeCan(card) {
    const trigger = card.querySelector('.duff-can');
    const detail = card.querySelector('.project-detail');
    if (!trigger || !detail) { return; }
    // No ocultar un panel si el foco está dentro, sin devolverlo a su botón.
    if (detail.contains(document.activeElement)) { trigger.focus(); }
    card.classList.remove('is-open');
    trigger.setAttribute('aria-expanded', 'false');
    trigger.setAttribute('aria-label', `Abrir lata para ver ${detail.querySelector('h3')?.textContent || 'proyecto'}`);
    detail.hidden = true;
}

document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
        const open = root.querySelector('.duff-project.is-open');
        if (open) { closeCan(open); open.querySelector('.duff-can').focus(); }
    }
});

