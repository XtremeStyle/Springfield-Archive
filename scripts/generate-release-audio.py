#!/usr/bin/env python3
"""Original procedural foley for Springfield Archive (deterministic; no external samples).

Run manually to regenerate can-open.wav, vault.wav and alert.wav.
Dependencies for regeneration only: NumPy, SciPy. No runtime JS dependencies.
Not intended to impersonate actual copyrighted voices/music of The Simpsons.
"""
from __future__ import annotations
from pathlib import Path
import wave
import numpy as np
from scipy.signal import butter, sosfilt

ROOT=Path(__file__).resolve().parent.parent/'frontend/assets/audio'
SR=44100
rng=np.random.default_rng(742)

def tgrid(duration): return np.arange(round(duration*SR),dtype=np.float64)/SR

def nband(length,lo=200,hi=17000):
    data=rng.normal(size=length)
    if lo>0: data=sosfilt(butter(2,lo,fs=SR,btype='highpass',output='sos'),data)
    if hi<SR/2: data=sosfilt(butter(3,hi,fs=SR,btype='lowpass',output='sos'),data)
    return data

def burst(t,at,duration,body,attack=.003,decay=.16):
    rel=t-at
    env=np.where(rel>=0,np.minimum(1,np.maximum(rel,0)/attack),0.)*np.where(rel>=0,np.exp(-np.maximum(rel,0)/decay),0.)
    env=np.where(rel<=duration,env,0.)
    return body*env

def metal(t,at,f,decay,amplitude=1,phase=0):
    x=np.maximum(t-at,0)
    return np.where(t>=at,np.sin(2*np.pi*(f*x+3.7*x*x)+phase)*np.exp(-x/decay)*amplitude,0)

def master(stereo):
    stereo=np.nan_to_num(stereo)
    maxamp=np.max(np.abs(stereo))
    if maxamp: stereo=stereo/maxamp*.88
    # small saturation to avoid harsh synthetic peaks
    stereo=np.tanh(stereo*1.13)/np.tanh(1.13)*.87
    return np.asarray(np.clip(stereo,-1,1)*32767,dtype='<i2')

def save(name,stereo):
    ROOT.mkdir(parents=True,exist_ok=True)
    with wave.open(str(ROOT/name),'wb') as w:
        w.setnchannels(2);w.setsampwidth(2);w.setframerate(SR);w.writeframes(master(stereo).tobytes())

def can_open():
    """Una lata realista: palanca, apertura seca y escape corto de gas (sin melodías)."""
    t=tgrid(.94);N=len(t)
    left=np.zeros(N);right=np.zeros(N)
    tab=burst(t,.038,.085,nband(N,400,10500),attack=.0008,decay=.009)
    left+=.13*tab;right+=.11*tab
    # La anilla flexiona la lámina: crujido breve y resonancia de aluminio.
    pop=burst(t,.135,.16,nband(N,230,11500),attack=.0008,decay=.020)
    left+=.84*pop;right+=.75*pop
    left+=metal(t,.136,1570,.085,.22)+metal(t,.138,3200,.045,.15)
    right+=metal(t,.137,1590,.09,.19)+metal(t,.138,3260,.05,.13)
    # El cierre cede bajo la presión con un segundo chasquido amortiguado.
    snap=burst(t,.193,.1,nband(N,450,7200),attack=.001,decay=.014)
    left+=.28*snap;right+=.3*snap
    # Soplido corto de CO2, sin la cola teatral de versiones anteriores.
    hiss=nband(N,1300,12000)
    rel=np.maximum(t-.19,0)
    env=np.where((t>=.19)&(t<=.71),(1-np.exp(-rel/.017))*np.exp(-rel/.15),0)
    left+=.24*hiss*env
    right+=.22*np.roll(hiss,21)*env
    # Sólo dos burbujas discretas al final.
    for when,scale in ((.42,.05),(.52,.036)):
        tick=burst(t,when,.07,nband(N,600,4300),attack=.001,decay=.012)
        left+=scale*tick;right+=scale*.9*tick
    save('can-open.wav',np.stack([left,right],axis=1))

def vault():
    t=tgrid(2.05);N=len(t);a=np.zeros(N);b=np.zeros(N)
    friction=nband(N,90,1600)
    rel=t-.17;turn=np.where((rel>0)&(rel<.9),np.sin(np.pi*np.clip(rel/.9,0,1))**.8,0)
    a+=friction*turn*.23;b+=np.roll(friction,190)*turn*.18
    for time,freq in ((.29,550),(.56,790),(.9,1040),(1.21,560)):
        n=burst(t,time,.2,nband(N,250,10000),.001,.026)
        a+=n*.35+metal(t,time,freq,.35,.22)
        b+=n*.29+metal(t,time,freq*1.008,.4,.2,phase=.25)
    # Heavy latch, then low reverberant metallic chime.
    slam=burst(t,1.42,.6,nband(N,55,2400),.003,.09)
    a+=slam*.85+metal(t,1.43,182,.45,.27)
    b+=slam*.75+metal(t,1.43,184,.49,.26)
    save('vault.wav',np.stack([a,b],axis=1))

def alert():
    t=tgrid(.91);N=len(t)
    # A municipal stamp/buzzer rather than an alarm melody.
    wave1=np.sin(2*np.pi*(116+28*t)*t)+.21*np.sin(2*np.pi*239*t)
    pulse=(np.sin(2*np.pi*8*t)>-.2).astype(float)
    env=np.exp(-2.8*t)*pulse
    grit=nband(N,280,3500)*.15
    a=(wave1*.53+grit)*env
    b=(np.roll(wave1,38)*.49+np.roll(grit,110))*env
    save('alert.wav',np.stack([a,b],axis=1))

if __name__=='__main__':
    can_open();vault();alert()
    for name in ('can-open.wav','vault.wav','alert.wav'):
        print(name,(ROOT/name).stat().st_size)
