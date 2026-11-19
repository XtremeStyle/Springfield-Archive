"""Foley original de lata de aluminio. Composición sintética, no grabaciones comerciales.
Salida: PCM estéreo 44.1 kHz; ninguna música, voz ni muestras externas.
"""
from pathlib import Path
import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR=44100
RNG=np.random.default_rng(8716)
ROOT=Path(__file__).resolve().parents[1]/'frontend/assets/audio'

def noise(length, low=0, high=0):
    x=RNG.standard_normal(int(SR*length))
    if low and high: sos=butter(3,(low,high),btype='bandpass',fs=SR,output='sos')
    elif low: sos=butter(3,low,btype='highpass',fs=SR,output='sos')
    else: sos=butter(3,high,btype='lowpass',fs=SR,output='sos')
    return sosfilt(sos,x)

def put(buf,at,wave,pan=0):
    pos=round(at*SR)
    if pos>=len(buf):return
    n=min(len(wave),len(buf)-pos)
    p=np.clip(pan,-1,1)
    left=np.sqrt((1-p)/2);right=np.sqrt((1+p)/2)
    buf[pos:pos+n,0]+=wave[:n]*left
    buf[pos:pos+n,1]+=wave[:n]*right

def ring(dur, frequencies, decay=12, amplitude=.35):
    t=np.arange(round(dur*SR))/SR
    signal=sum(np.sin(2*np.pi*f*t)*np.exp(-t*decay*(1+(i*.12))) for i,f in enumerate(frequencies))/len(frequencies)
    return amplitude*signal

def tap(dur=.08,strength=.7):
    t=np.arange(round(dur*SR))/SR
    return strength*(noise(dur,900,13000)*.23+ring(dur,[680,1270,2330,4120],42,.8))*np.exp(-t*35)

def write(name,wav):
    # Mild limiter to avoid clipping while retaining a tangible metallic pop.
    wav=np.tanh(wav*1.6)/np.tanh(1.6)
    peak=max(np.max(np.abs(wav)),.001)
    wav=wav/peak*.84
    wavfile.write(ROOT/name,SR,(wav*32767).astype('<i2'))

# Lata: roce de anilla -> chasquido agudo -> rotura del sello -> silbido
# presurizado -> resonancia fina del aluminio y burbujas aisladas.
buf=np.zeros((round(2.25*SR),2),dtype='float64')
put(buf,.035,tap(.11,.52),pan=-.18)
put(buf,.120,ring(.27,[440,890,2010,3360],21,.30),pan=.04)
put(buf,.245,tap(.12,1.0),pan=-.06)
put(buf,.270,ring(.5,[124,251,514,1029,2140],16,.58),pan=.12)
put(buf,.285,tap(.08,.42),pan=.2)
# Fuga de gas turbulenta (crece de golpe y desaparece de forma natural).
dur=1.43;t=np.arange(round(SR*dur))/SR
cloud=noise(dur,700,12500)
hiss=((1-np.exp(-t*55))*np.exp(-t*2.5)) * cloud * .43
put(buf,.335,hiss,pan=-.17)
# Rebote metálico y tapa vibrando.
for i,at in enumerate((.52,.60,.72,.86)):
    put(buf,at,ring(.23,[940-90*i,1880-120*i,2930-80*i],15,.13/(1+i*.18)),pan=(-.2 if i%2 else .2))
# Burbujas discretas: cada una con tono descendente corto y material elástico.
for i,at in enumerate((.57,.68,.83,.98,1.14,1.33,1.47,1.63,1.80)):
    dur=.09; t=np.arange(round(SR*dur))/SR
    f=390+RNG.uniform(-110,290)
    chirp=np.sin(2*np.pi*(f*t+250*t*t))*np.exp(-t*38)*(.06+.04*RNG.random())
    put(buf,at,chirp,pan=RNG.uniform(-.55,.55))
put(buf,1.93,ring(.22,[1120,2290,3410],24,.10),pan=.25)
write('can-open.wav',buf)

buf=np.zeros((round(.7*SR),2),dtype='float64')
put(buf,.04,tap(.1,.72),-.1)
put(buf,.07,ring(.37,[330,675,1210,2480],17,.47),.2)
put(buf,.34,tap(.08,.29),.1)
write('can-close.wav',buf)
print('Efectos de lata originales regenerados: estéreo 44.1 kHz; apertura 2.25 s, cierre 0.70 s')
