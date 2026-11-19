"""Real browser regression for Springfield Files losing state (mocked Java API).
No network calls, no DB writes, no fabricated claim of end-to-end integration.
"""
from playwright.sync_api import sync_playwright
from qa_final_browser import CHARS, LOCS, EPIS, page_load

OCC={'Teacher':'Docente','Police officer':'Agente de policía',
     'Nuclear safety inspector':'Inspector de seguridad nuclear','Bartender':'Cantinero',
     'Doctor':'Médico','School principal':'Director escolar','Clown':'Payaso',
     'Mayor':'Alcalde','Business owner':'Empresario','News anchor':'Presentador de noticias',
     'Scientist':'Científico','Bus driver':'Conductor de autobús'}
USES={'Bar':'Bar','School':'Escuela','Restaurant':'Restaurante','Nuclear power plant':'Central nuclear',
      'Hospital':'Hospital','Store':'Tienda','Church':'Iglesia','Museum':'Museo',
      'Police station':'Comisaría','Park':'Parque','Prison':'Prisión','Office':'Oficina'}

def correct_name(act, hints):
    values=[x.strip() for x in hints]
    if act==0:
        return next(row['name'] for row in CHARS if OCC[row['occupation']]==values[0] and values[1]=='Vivo')
    if act==1:
        return next(row['name'] for row in LOCS if USES[row['use']]==values[0])
    return next(row['name'] for row in EPIS if str(row['season'])==values[0] and str(row['episodeNumber'])==values[1])

def run():
    with sync_playwright() as pw:
        b=pw.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox','--disable-gpu'])
        for width in (1366,390):
            p=b.new_page(viewport={'width':width,'height':800},reduced_motion='reduce')
            errors=[]
            p.on('pageerror',lambda e:errors.append(str(e)))
            page_load(p,'game.html','noir-game.js')
            p.locator('#noir-begin').click()
            lost=False
            for act in range(3):
                if lost:break
                p.locator('#noir-envelope').click()
                target=correct_name(act,p.locator('#noir-clues dd').all_inner_texts())
                others=[opt for opt in p.locator('.noir-option').all() if opt.locator('strong').inner_text()!=target]
                assert len(others)==3
                for opt in others:
                    opt.click()
                    p.locator('#noir-verify').click()
                    if p.locator('#noir-failed-heading').count():lost=True;break
                if not lost:
                    # If this act did not exhaust resources, the right answer advances it.
                    p.locator('.noir-option').filter(has_text=target).click()
                    p.locator('#noir-verify').click()
                    p.locator('#noir-next').click()
            assert lost,'Unlimited incorrect attempts must NOT count as victory.'
            assert '0%' in p.locator('#noir-status').inner_text() or 'Expediente cerrado' in p.locator('#noir-status').inner_text()
            assert p.locator('#noir-restart').is_visible()
            assert p.get_by_text('Informe entregado.',exact=False).count()==0
            p.locator('#noir-restart').click()
            assert p.locator('#noir-begin').is_visible()
            assert not errors,errors
            p.close()
        b.close()
    print('QA_GAME_OVER_PASS: failure ends the case, no victory exploit; replay available (API simulated).')

if __name__=='__main__':run()
