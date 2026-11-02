#!/usr/bin/env python3
"""Makes NeonLut.kt: a colour look-up table (a 32x32x32 cube as a 1024x32 PNG, block = blue, x = red, y = green) that turns Mapbox
Standard's night map into a neon one: a near-black blue city, roads and labels glowing cyan, water deep blue, parks dark teal;
strongly coloured things (traffic, markers) are kept. (The buildings are the app's own red blocks, not part of this.) Run: python3 tools/make_neon_lut.py > app/src/main/kotlin/com/headunit/launcher/NeonLut.kt"""
import base64, colorsys, io
from PIL import Image

ACCENTS = {"yellow": (0xFC, 0xEE, 0x0A), "cyan": (0x19, 0xF0, 0xFF), "magenta": (0xFF, 0x3C, 0xCB)}

def sm(a, b, x):
    t = max(0.0, min(1.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)

def mix(c1, c2, t):
    return tuple(c1[i] + (c2[i] - c1[i]) * t for i in range(3))

def ramp(stops, x):
    for i in range(1, len(stops)):
        if x <= stops[i][0]:
            x0, c0 = stops[i - 1]; x1, c1 = stops[i]
            return mix(c0, c1, (x - x0) / (x1 - x0))
    return stops[-1][1]

def make(acc):
    a = tuple(v / 255.0 for v in acc)
    white = (1.0, 1.0, 1.0)
    hot = mix(a, white, 0.55)
    land = [(0.0, (0.010, 0.012, 0.030)), (0.12, (0.028, 0.034, 0.075)), (0.28, (0.055, 0.062, 0.125)),
            (0.42, mix((0.07, 0.07, 0.15), a, 0.30)), (0.62, mix((0.07, 0.07, 0.15), a, 0.68)), (0.85, a), (1.0, hot)]
    water = [(0.0, (0.004, 0.012, 0.040)), (0.5, (0.020, 0.060, 0.200)), (1.0, (0.10, 0.28, 0.62))]
    park = [(0.0, (0.004, 0.030, 0.030)), (0.5, (0.018, 0.100, 0.090)), (1.0, (0.08, 0.30, 0.24))]
    out = []
    for bl in range(32):
        for g in range(32):
            row = []
            for r in range(32):
                R, G, B = r / 31.0, g / 31.0, bl / 31.0
                lum = 0.2126 * R + 0.7152 * G + 0.0722 * B
                h, s, v = colorsys.rgb_to_hsv(R, G, B)
                hd = h * 360.0
                base = ramp(land, lum ** 0.9)
                w_water = sm(0.38, 0.55, s) * sm(185, 200, hd) * (1 - sm(255, 275, hd))
                w_park = sm(0.14, 0.30, s) * sm(65, 85, hd) * (1 - sm(165, 185, hd))
                col = base
                if w_water > 0: col = mix(col, ramp(water, lum * 1.6), w_water)
                if w_park > 0: col = mix(col, ramp(park, lum * 1.6), w_park)
                # strongly coloured (traffic, markers): kept, a little brighter
                keep = sm(0.62, 0.80, s) * sm(0.50, 0.70, v) * (1 - w_water)
                if keep > 0:
                    boosted = tuple(min(1.0, c * 1.08) for c in (R, G, B))
                    col = mix(col, boosted, keep)
                row.append(tuple(int(round(max(0.0, min(1.0, c)) * 255)) for c in col))
            out.append(row)
    im = Image.new("RGB", (1024, 32))
    for bl in range(32):
        for g in range(32):
            for r in range(32):
                im.putpixel((bl * 32 + r, g), out[bl * 32 + g][r])
    buf = io.BytesIO(); im.save(buf, "PNG", optimize=True)
    return base64.b64encode(buf.getvalue()).decode()

print("package com.headunit.launcher\n")
print("/**\n * The neon look's map colours: a colour look-up table (a 32x32x32 cube as a 1024x32 PNG), given to the Standard style as its\n"
      " * custom theme. Made by tools/make_neon_lut.py.\n */\nobject NeonLut {")
b = make(ACCENTS["cyan"])
print("    const val BASE64: String =")
chunks = [b[i:i + 120] for i in range(0, len(b), 120)]
print(" +\n".join(f'        "{c}"' for c in chunks))
print("}")
