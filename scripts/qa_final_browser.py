"""Pruebas DOM en Chromium de módulos reales con API simulada.
No se presenta como prueba Spring Boot/PostgreSQL ni red externa.
"""
from __future__ import annotations
import base64
import json
import re
from pathlib import Path
from playwright.sync_api import sync_playwright
ROOT=Path(__file__).resolve().parent.parent
FRONT=ROOT/'frontend'
OUT=ROOT/'qa-final'
OUT.mkdir(exist_ok=True)
CHARS=[dict(id=i,externalId=i,slug=f'persona-{i}',name=f'Persona {i:02}',occupation=o,status=s,
            gender='Male',age=30+i,portraitPath=f'/character/{i}.webp') for i,(o,s) in enumerate([
('Teacher','Alive'),('Police officer','Alive'),('Nuclear safety inspector','Alive'),
('Bartender','Alive'),('Doctor','Alive'),('School principal','Alive'),('Clown','Alive'),
('Mayor','Alive'),('Business owner','Alive'),('News anchor','Alive'),
('Scientist','Alive'),('Bus driver','Alive')],start=1)]
LOCS=[dict(id=i,externalId=i,slug=f'lugar-{i}',name=f'Lugar {i:02}',town='Springfield',use=use,
           imagePath=f'/location/{i}.webp') for i,use in enumerate([
'Bar','School','Restaurant','Nuclear power plant','Hospital','Store','Church','Museum',
'Police station','Park','Prison','Office'],start=1)]
EPIS=[dict(id=i,externalId=i,slug=f'capitulo-{i}',name=f'Capítulo {i:02}',season=1+(i//13),
           episodeNumber=i,airdate='1990-01-01',imagePath=f'/episode/{i}.webp') for i in range(1,21)]
DATA=dict(characters=CHARS,locations=LOCS,episodes=EPIS)

MOCK_JS='''globalThis.fetch=async (input)=>{
 const u=new URL(String(input),'https://springfield.test');
 const p=u.pathname.split('/');const kind=p[3];
 const data=globalThis.__qaData;
 let body,status=200;
 if(kind==='stats')body={characters:{total:data.characters.length},locations:{total:data.locations.length},episodes:{total:data.episodes.length}};
 else if(kind==='sync'&&p[4]==='status')body=[];
 else if(kind==='episodes'&&p[4]==='seasons')body=[{season:1,episodes:12},{season:2,episodes:8}];
 else if(data[kind]){
  let rows=data[kind].slice();const q=(u.searchParams.get('q')||'').toLowerCase();
  if(u.searchParams.get('source')==='editorial')rows=[];
  if(q)rows=rows.filter(x=>x.name.toLowerCase().includes(q));
  const season=u.searchParams.get('season');if(season)rows=rows.filter(x=>x.season===Number(season));
  if(u.searchParams.get('order')==='za')rows.reverse();
  const page=Number(u.searchParams.get('page')||0),size=Number(u.searchParams.get('size')||20);
  if(p[4]&&!['external','seasons'].includes(p[4])){
   body=data[kind].find(x=>x.slug===p[4]);if(!body){status=404;body={title:'No existe'}}
  } else if(p[4]==='external'){
   body=data[kind].find(x=>x.externalId===Number(p[5]));if(!body){status=404;body={title:'No existe'}}
  }else body={content:rows.slice(page*size,(page+1)*size),page,size,totalPages:Math.ceil(rows.length/size),totalElements:rows.length};
 } else {status=404;body={title:'No existe'}}
 return {ok:status===200,status,headers:new Headers({'content-type':'application/json'}),json:async()=>body,text:async()=>JSON.stringify(body)};
};'''

def module_url(file_name, cache=None):
    cache={} if cache is None else cache
    path=(FRONT/'js'/file_name).resolve()
    if path in cache:return cache[path]
    text=path.read_text()
    text=text.replace('window.location.origin', '"https://springfield.test"')
    text=re.sub(r'from\s+[\'\"]\./([^\'\"]+)[\'\"]',lambda m:'from '+json.dumps(module_url(m.group(1),cache)),text)
    url='data:text/javascript;base64,'+base64.b64encode(text.encode()).decode()
    cache[path]=url
    return url

def page_load(page, html_name, entry, url='https://springfield.test/'):
    html=(FRONT/html_name).read_text()
    html=re.sub(r'<script[^>]+type="module"[^>]*></script>','',html)
    html=re.sub(r'<link[^>]+href="/css/[^\"]+\.css(?:\?v=[^\"]+)?"[^>]*>','',html)
    css='\n'.join((FRONT/'css'/x).read_text().replace('@import "tokens.css";','') for x in ['tokens.css','main.css', 'noir.css' if html_name=='game.html' else 'final.css'])
    html=html.replace('</head>','<style>'+css+'</style></head>')
    page.set_content(html,wait_until='domcontentloaded')
    # about:blank no permite localStorage; simularlo explícitamente para pruebas offline.
    page.evaluate("""() => {const data=new Map();Object.defineProperty(window,'localStorage',{configurable:true,value:{getItem:k=>data.get(k)??null,setItem:(k,v)=>data.set(k,String(v)),removeItem:k=>data.delete(k),clear:()=>data.clear(),key:i=>[...data.keys()][i]??null,get length(){return data.size;}}});}""")
    page.evaluate('(data)=>{globalThis.__qaData=data;'+MOCK_JS+'}',DATA)
    page.evaluate('async url => await import(url)',module_url(entry))


def run():
    errors=[]
    with sync_playwright() as pw:
        browser=pw.chromium.launch(headless=True, executable_path='/usr/bin/chromium', args=['--no-sandbox','--disable-gpu'])
        for width in (1366,390,320):
            context=browser.new_context(viewport={'width':width,'height':880},reduced_motion='reduce')
            page=context.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
            page_load(page,'game.html','noir-game.js')
            assert page.locator('#noir-begin').is_visible()
            page.screenshot(path=str(OUT/f'01-intro-{width}.png'),full_page=True)
            page.locator('#noir-begin').click()
            page.locator('#noir-envelope').wait_for(timeout=12000)
            confirmed=[]
            for n in range(3):
                page.locator('#noir-envelope').click()
                assert page.locator('#noir-clues').is_visible()
                options=page.locator('.noir-option');assert options.count()==4
                # La respuesta correcta se obtiene solo de la prueba simulada del test, no del juego.
                # Las fichas de prueba del contrato incluyen claves por tipo y un conjunto de hechos únicos.
                rows=[o for o in (CHARS if n==0 else LOCS if n==1 else EPIS)]
                hinted=page.locator('#noir-clues').inner_text()
                actual_values=page.locator('#noir-clues dd').all_inner_texts()
                choices=options.all()
                pick=None
                for choice in choices:
                    name=choice.locator('strong').inner_text()
                    row=next((x for x in rows if x['name']==name),None)
                    if row is None:continue
                    if n==0:
                        from_text={'Teacher':'Docente','Police officer':'Agente de policía','Nuclear safety inspector':'Inspector de seguridad nuclear','Bartender':'Cantinero', 'Doctor':'Médico','School principal':'Director escolar','Clown':'Payaso','Mayor':'Alcalde','Business owner':'Empresario','News anchor':'Presentador de noticias','Scientist':'Científico','Bus driver':'Conductor de autobús'}[row['occupation']]
                        is_right=from_text==actual_values[0] and actual_values[1]=='Vivo'
                    elif n==1:
                        uses={'Bar':'Bar','School':'Escuela','Restaurant':'Restaurante','Nuclear power plant':'Central nuclear','Hospital':'Hospital','Store':'Tienda','Church':'Iglesia','Museum':'Museo','Police station':'Comisaría','Park':'Parque','Prison':'Prisión','Office':'Oficina'}
                        is_right=uses[row['use']]==actual_values[0]
                    else:
                        is_right=str(row['season'])==actual_values[0] and str(row['episodeNumber'])==actual_values[1]
                    if is_right:
                        pick=choice;break
                if pick is None: raise AssertionError('Test no pudo inferir respuesta de pistas visibles: '+hinted)
                if n==0:
                    assert all('Ficha reservada' in x for x in page.locator('.noir-option small').all_inner_texts())
                    others=[o for o in choices if o is not pick]
                    for idx,candidate in enumerate(others):
                        candidate.click()
                        assert 'Datos reservados' in page.locator('#noir-inspection').inner_text()
                        page.locator('#noir-inspect').click()
                        assert 'Datos de la ficha:' in page.locator('#noir-inspection').inner_text()
                        if idx==1:assert 'Integridad 100%' in page.locator('#noir-status').inner_text()
                    assert 'Integridad 94%' in page.locator('#noir-status').inner_text()
                    page.evaluate('window.scrollTo(0,0)')
                    page.wait_for_timeout(125)
                    page.screenshot(path=str(OUT/f'05-inspeccion-{width}.png'),full_page=True)
                confirmed.append(pick.locator('strong').inner_text())
                pick.click();page.locator('#noir-verify').click()
                assert page.get_by_text('Verificación correcta.',exact=False).count()>=1
                page.locator('#noir-next').click()
                if n<2:page.locator('#noir-envelope').wait_for(timeout=8000)
            # El cuarto documento es real, pero no fue uno de los tres confirmados.
            page.locator('.noir-audit-file').first.wait_for(timeout=8000)
            page.screenshot(path=str(OUT/f'09-auditoria-{width}.png'),full_page=True)
            assert page.locator('.noir-audit-file').count()==4
            intruder=next(card for card in page.locator('.noir-audit-file').all()
                          if card.locator('strong').inner_text() not in confirmed)
            if width==1366:
                wrong=next(card for card in page.locator('.noir-audit-file').all() if card.locator('strong').inner_text() in confirmed)
                wrong.click();page.locator('#noir-audit-verify').click()
                assert 'Pierdes 14' in page.locator('#noir-audit-feedback').inner_text()
            intruder.click();page.locator('#noir-audit-verify').click()
            assert page.get_by_text('Lote limpio.',exact=False).is_visible()
            page.locator('#noir-next').click()
            page.locator('.noir-order-file').first.wait_for(timeout=8000)
            page.screenshot(path=str(OUT/f'10-ordenacion-{width}.png'),full_page=True)
            if width==1366:
                page.locator('#noir-sort-verify').click()
                assert '−9' in page.locator('#noir-sort-feedback').inner_text()
            # Se reordena de manera interactiva, sin modificar el estado del juego.
            for target in range(3):
                rows=page.locator('.noir-order-file').all()
                kinds={'PERSONA':0,'LUGAR':1,'EPISODIO':2}
                keys=[]
                for row in rows:
                    meta=row.locator('small').inner_text()
                    keys.append((int(meta.split()[-1]),kinds[meta.split(' ')[0]]))
                desired=sorted(keys)[target]
                pos=keys.index(desired)
                while pos>target:
                    page.locator('.noir-order-file').nth(pos).locator('[data-move="up"]').click()
                    pos-=1
            page.locator('#noir-sort-verify').click()
            assert page.get_by_text('Secuencia recuperada.',exact=False).is_visible()
            page.locator('#noir-next').click()
            page.locator('#noir-vault-form').wait_for(timeout=8000)
            page.screenshot(path=str(OUT/f'11-cifrado-{width}.png'),full_page=True)
            receipts=page.locator('.noir-receipt small').all_inner_texts()
            ids=[int(text.split('#')[-1].strip()) for text in receipts]
            digits=[str((sum(int(ch) for ch in str(id))+key)%10) for id,key in zip(ids,(7,4,2))]
            for i,d in enumerate(digits):page.locator(f'[data-digit="{i}"]').select_option(d)
            if width==1366:
                page.locator('[data-digit="0"]').select_option(str((int(digits[0])+1)%10))
                page.locator('#noir-vault-form button[type="submit"]').click()
                assert '−12' in page.locator('#noir-vault-feedback').inner_text()
                page.locator('[data-digit="0"]').select_option(digits[0])
            page.locator('#noir-vault-form button[type="submit"]').click()
            assert page.get_by_text('Informe entregado.',exact=False).is_visible()
            page.screenshot(path=str(OUT/f'02-victoria-{width}.png'),full_page=True)
            assert not page.evaluate('document.documentElement.scrollWidth > innerWidth + 2'),f'game overflow {width}'
            page.close();context.close()
        for name,entry in [('personajes.html','personajes.js'),('lugares.html','lugares.js')]:
            for width in (1366,390,320):
                context=browser.new_context(viewport={'width':width,'height':860},reduced_motion='reduce')
                page=context.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
                page_load(page,name,entry)
                page.locator('.catalog-card').first.wait_for(timeout=10000)
                assert page.locator('.catalog-card').count()>=8
                assert page.locator('#source').input_value()=='curated'
                page.screenshot(path=str(OUT/f'03-{name[:-5]}-{width}.png'),full_page=True)
                assert not page.evaluate('document.documentElement.scrollWidth > innerWidth + 2'),f'{name} overflow {width}'
                page.close();context.close()
        # Una ficha editorial conocida y una importada: etiqueta de origen fiel.
        context=browser.new_context(viewport={'width':1000,'height':850},reduced_motion='reduce')
        page=context.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
        # ficha.js se ejecuta con query param reemplazado en el módulo.
        source=(FRONT/'js'/'ficha.js').read_text()
        # For this isolated DOM test the origin is about:blank, so use known public slug from mock.
        source=source.replace("const slug=new URLSearchParams(location.search).get('slug')||'';","const slug='persona-1';")
        source=source.replace("const KIND=document.body.dataset.kind || (location.pathname.includes('personaje')?'character':location.pathname.includes('episodio')?'episode':'location');", "const KIND='character';")
        from tempfile import NamedTemporaryFile
        f=FRONT/'js'/'__qa_ficha_temp__.js'
        try:
            f.write_text(source)
            page_load(page,'personaje.html',f.name)
            page.locator('.final-ficha').wait_for(timeout=12000)
            assert page.get_by_text('THE SIMPSONS API / REGISTRO IMPORTADO').is_visible()
            page.screenshot(path=str(OUT/'04-ficha.png'),full_page=True)
        finally:f.unlink(missing_ok=True)
        context.close()
        # Additional complete-product regression: home, episodes, Duff portfolio, and favorites.
        checks=[('index.html','home.js'),('episodios.html','episodios.js'),('sobre-mi.html','portfolio.js'),('favoritos.html','favoritos.js')]
        for name,entry in checks:
            context=browser.new_context(viewport={'width':390,'height':860},reduced_motion='reduce')
            page=context.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
            page_load(page,name,entry)
            if name=='index.html':
                page.locator('#stats .stat').first.wait_for(timeout=10000)
                assert page.locator('#stats .stat').count()==4
                page.locator('#discovery-character .discovery-open').first.wait_for(timeout=10000)
                assert page.locator('.discovery-open').count()==3
                original=page.locator('#discovery-character h3').inner_text()
                page.locator('[data-next=character]').click()
                assert page.locator('#discovery-character h3').inner_text()!=original
                favorite=page.locator('#discovery-character [data-favorite]')
                favorite.click();assert favorite.get_attribute('aria-pressed')=='true'
                assert page.locator('#sync-states .sync-state').count()==3
                page.screenshot(path=str(OUT/'06-portada-390.png'),full_page=True)
            elif name=='episodios.html':
                page.locator('.catalog-card').first.wait_for(timeout=10000)
                assert page.locator('.catalog-card').count()>=8
                assert page.locator('#season option').count()>=2
                page.screenshot(path=str(OUT/'07-episodios-390.png'),full_page=True)
            elif name=='sobre-mi.html':
                assert page.locator('.duff-project').count()>=3
                first=page.locator('.duff-can').first
                first.click()
                assert first.get_attribute('aria-expanded')=='true'
                assert page.locator('.project-detail').first.is_visible()
                if page.locator('.duff-project').count()>1:
                    page.locator('.duff-can').nth(1).click()
                    assert first.get_attribute('aria-expanded')=='false'
                    assert page.locator('.project-detail').nth(1).is_visible()
                page.screenshot(path=str(OUT/'08-duff-390.png'),full_page=True)
            else:
                assert 'Todav' in page.locator('#results').inner_text()
            assert not page.evaluate('document.documentElement.scrollWidth > innerWidth + 2'),f'overflow {name}'
            page.close();context.close()
        browser.close()
    if errors:raise AssertionError('JavaScript browser errors: '+str(errors))
    print('QA_FINAL_BROWSER_PASS: 3 partidas x 6 fases en 3 viewports, catálogos, ficha, descubrimientos, Duff y favoritos. API simulada, NO PostgreSQL.')
if __name__=='__main__':run()
