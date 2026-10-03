#!/usr/bin/env python3
# xin.py <action>...  (DISPLAY and XAUTHORITY must be set) actions: rclick | lclick | key:<keysym> | type:<text> | sleep:<sec> | move:x,y
import sys, os, glob, time
from Xlib import X, XK, display
from Xlib.ext import xtest
assert 'DISPLAY' in os.environ and 'XAUTHORITY' in os.environ


d = display.Display()
def key(sym):
    code = d.keysym_to_keycode(XK.string_to_keysym(sym))
    shift = sym.isupper() and len(sym) == 1
    if shift: xtest.fake_input(d, X.KeyPress, d.keysym_to_keycode(XK.XK_Shift_L))
    xtest.fake_input(d, X.KeyPress, code); d.sync(); time.sleep(0.05)
    xtest.fake_input(d, X.KeyRelease, code)
    if shift: xtest.fake_input(d, X.KeyRelease, d.keysym_to_keycode(XK.XK_Shift_L))
    d.sync(); time.sleep(0.05)
names = {' ': 'space', '.': 'period', '/': 'slash', '-': 'minus', '=': 'equal', '(': 'parenleft', ')': 'parenright', '"': 'quotedbl', ',': 'comma', '\n': 'Return'}
for a in sys.argv[1:]:
    if a in ('rclick', 'lclick'):
        b = 3 if a == 'rclick' else 1
        xtest.fake_input(d, X.ButtonPress, b); d.sync(); time.sleep(0.1)
        xtest.fake_input(d, X.ButtonRelease, b); d.sync()
    elif a.startswith('key:'): key(a[4:])
    elif a.startswith('type:'):
        for ch in a[5:]: key(names.get(ch, ch))
    elif a.startswith('sleep:'): time.sleep(float(a[6:]))
    elif a.startswith('move:'):
        x, y = map(int, a[5:].split(',')); xtest.fake_input(d, X.MotionNotify, x=x, y=y); d.sync()
    time.sleep(0.2)
