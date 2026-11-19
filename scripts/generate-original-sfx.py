"""Original Springfield Archive Foley-inspired synthetic sound effects.
No third-party samples, character voices or copyrighted music are used.
Regeneration only: Python 3, NumPy and SciPy; generated WAVs have no runtime dependencies.
"""
from pathlib import Path
import wave
import numpy as np
from scipy.signal import butter, sosfilt

SR=22050
ROOT=Path(__file__).resolve().parents[1]/'frontend/assets/audio'
ROOT.mkdir(parents=True,exist_ok=True)
rng=np.random.default_rng(2046)

def bandnoise(size,low=None,high=None):
    x=rng.standard_normal(size)
    if low and high: return sosfilt(butter(2,[low,high],btype='bandpass',fs=SR,output='sos'),x)
    if low: return sosfilt(butter(2,low,btype='highpass',fs=SR,output='sos'),x)
    if high: return sosfilt(butter(2,high,btype='lowpass',fs=SR,output='sos'),x)
    return x

def layer(buf, start, signal):
    pos=int(start*SR)
    if pos<0 or pos>=len(buf):return
    n=min(len(buf)-pos,len(signal)); buf[pos:pos+n]+=signal[:n]

def click(dur=.08, pitch=1200, amp=.3):
    n=int(dur*SR); t=np.arange(n)/SR
    transient=bandnoise(n,max(200,pitch*.45),min(10000,pitch*2))
    return amp*(.7*transient/3 + .3*np.sin(2*np.pi*pitch*t))*np.exp(-t*55)

def sweep(dur,f0,f1,amp=.3,decay=6):
    t=np.arange(int(dur*SR))/SR
    freq=f0+(f1-f0)*np.minimum(1,t/max(.001,dur))
    phase=np.cumsum(2*np.pi*freq/SR)
    return amp*np.sin(phase)*np.exp(-t*decay)

def write(name,a):
    a=np.clip(a,-1,1)
    peak=np.max(np.abs(a)) or 1
    a=np.clip(a/max(1,peak/.85),-1,1)
    with wave.open(str(ROOT/name),'wb') as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(SR)
        f.writeframes((a*32767).astype('<i2').tobytes())

# Aluminum can opening: lever scratch -> pressure seal pop -> CO2 hiss -> rim resonance.
a=np.zeros(int(1.2*SR))
layer(a,.02,click(.115,2150,.40))
layer(a,.10,click(.10,3500,.27))
layer(a,.154,sweep(.24,430,125,.38,19))
layer(a,.157,click(.11,720,.45))
t=np.arange(int(.83*SR))/SR
hiss=(1-np.exp(-t*95))*np.exp(-t*3.6)*bandnoise(len(t),1000,8000)*.21
layer(a,.19,hiss)
for start in [.32,.39,.46,.57,.64,.74,.82]:
    layer(a,start,sweep(.065,420+rng.uniform(0,350),950+rng.uniform(0,200),.095,26))
layer(a,.87,click(.19,1100,.21))
write('can-open.wav',a)

# Thin metal can rim / tapping aluminum lid.
a=np.zeros(int(.36*SR));layer(a,.015,click(.10,1800,.45))
layer(a,.026,sweep(.25,920,700,.14,14));layer(a,.135,click(.09,1200,.17));write('can-close.wav',a)

# Shuffling papers, fingerprint dusting, typewriter tap.
a=np.zeros(int(.7*SR))
for st in [0,.17,.29,.44]:
    dur=.20;t=np.arange(int(dur*SR))/SR
    env=np.sin(np.minimum(1,t/dur)*np.pi)**1.4
    layer(a,st,bandnoise(len(t),500,5800)*env*.13)
layer(a,.33,click(.05,1700,.25));write('paper.wav',a)

# Rubber stamp striking paper.
a=np.zeros(int(.45*SR));layer(a,.06,click(.065,450,.48));layer(a,.07,sweep(.22,220,95,.28,20))
layer(a,.18,click(.08,650,.18));write('stamp.wav',a)

# Comical original rubber-boing, not any melody or recognizable episode sound.
a=np.zeros(int(.70*SR));layer(a,.025,sweep(.48,580,180,.30,4))
layer(a,.09,sweep(.37,270,440,.13,10));write('success.wav',a)
a=np.zeros(int(.7*SR));layer(a,.06,sweep(.52,440,105,.40,3))
layer(a,.09,bandnoise(int(.35*SR),500,2000)*np.exp(-np.arange(int(.35*SR))/SR*12)*.09)
write('fail.wav',a)
a=np.zeros(int(.35*SR));layer(a,.035,click(.085,900,.35));layer(a,.10,click(.095,1200,.3));write('click.wav',a)
a=np.zeros(int(.5*SR));layer(a,.01,click(.07,1100,.3));layer(a,.10,sweep(.35,200,750,.23,9));write('hint.wav',a)
a=np.zeros(int(.55*SR));layer(a,.08,sweep(.3,650,180,.21,5));layer(a,.15,click(.04,2500,.23));layer(a,.25,sweep(.19,220,700,.19,9));write('donut.wav',a)
a=np.zeros(int(.9*SR));layer(a,.02,bandnoise(int(.25*SR),200,2500)*np.exp(-np.arange(int(.25*SR))/SR*12)*.17)
layer(a,.23,click(.075,900,.25));layer(a,.32,sweep(.55,125,260,.20,5));write('start.wav',a)

# Very soft ambient library/nuclear archive ventilation and distant documents.
dur=9;t=np.arange(int(dur*SR))/SR
a=bandnoise(len(t),high=310)*.009+np.sin(2*np.pi*57*t)*.005
for start in [1.1,2.9,4.5,6.8,7.7]: layer(a,start,click(.035,800,.07))
fade=np.minimum(1,t/.65)*np.minimum(1,(dur-t)/.65)
a*=np.clip(fade,0,1)
write('archive-ambience.wav',a)
print('Generated',len(list(ROOT.glob('*.wav'))),'original WAV sounds:',sum(p.stat().st_size for p in ROOT.glob('*.wav')),'bytes')
