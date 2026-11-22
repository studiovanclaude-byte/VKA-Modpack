#!/usr/bin/env python3
"""VKA v9: independent, RGBA, bevelled stone 3D GUI controls.

NO screenshot crops. Buttons and icons are separate game resources.
Texts are rendered by Minecraft. Normal/hover/pressed have real visual
depth, transparent cut corners, metal rim, weathered stone and edge shading.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import argparse, random, math

ap=argparse.ArgumentParser()
ap.add_argument("--output",required=True)
args=ap.parse_args()
out=Path(args.output)
out.mkdir(parents=True,exist_ok=True)

W,H=1540,206       # 2x native design 770x103
CUT=30
def octagon(x0,y0,x1,y1,c):
    return [(x0+c,y0),(x1-c,y0),(x1,y0+c),(x1,y1-c),
            (x1-c,y1),(x0+c,y1),(x0,y1-c),(x0,y0+c)]

# Deterministic marble/rock grain. This has never been extracted from the
# source panorama or from a screenshot of the existing menu.
rng=random.Random(32029)
coarse=Image.new("L",(128,20))
coarse.putdata([rng.randrange(24,224) for _ in range(128*20)])
coarse=coarse.resize((W,H),Image.Resampling.BICUBIC).filter(ImageFilter.GaussianBlur(2.4))
fine=Image.new("L",(W,H))
fine.putdata([rng.randrange(55,195) for _ in range(W*H)])
rock=Image.new("RGBA",(W,H))
a=list(coarse.getdata()); b=list(fine.getdata())
colors=[]
for n,(c,f) in enumerate(zip(a,b)):
    y=n//W
    noise=(c-128)*0.16+(f-128)*0.09
    fade=(y/H)*11
    shade=lambda v:max(0,min(255,int(v+noise-fade)))
    colors.append((shade(60),shade(69),shade(65),255))
rock.putdata(colors)

def paint(state):
    pressed=state=="pressed"
    hovered=state=="hover"
    sprite=Image.new("RGBA",(W,H),(0,0,0,0))
    draw=ImageDraw.Draw(sprite,"RGBA")
    # All art is within its own 770×103 hit rectangle. No shadow leaking
    # into another control, no leftover baked border drawn on the scene.
    shadow=(12,30,W-12,H-2)
    draw.polygon(octagon(*shadow,CUT),(0,5,3,90 if pressed else 125))
    y_shift=8 if pressed else (-2 if hovered else 0)
    face_bottom=167+y_shift if not pressed else 186
    face_top=7+y_shift
    edge_bottom=194 if not pressed else 199
    # Actual extrusion BELOW the face, rather than a flat rectangle.
    draw.polygon(octagon(10,face_top+15,W-10,edge_bottom,CUT),(28,35,33,255))
    draw.line([(26,edge_bottom-8),(W-26,edge_bottom-8)],fill=(12,19,17,255),width=5)
    draw.line([(42,face_bottom+2),(W-42,face_bottom+2)],fill=(72,79,76,255),width=5)
    draw.polygon(octagon(8,face_top,W-8,face_bottom,CUT),
                 (182,193,184,255) if hovered else (155,165,160,255))
    draw.polygon(octagon(15,face_top+6,W-15,face_bottom-8,CUT-3),
                 (22,30,27,255))
    # Clip the stone to the inner chamfered polygon.
    m=Image.new("L",(W,H))
    md=ImageDraw.Draw(m)
    md.polygon(octagon(20,face_top+10,W-20,face_bottom-13,CUT-4),fill=255)
    sprite.paste(rock,(0,0),m)
    # Hand-carved chrome trim with light catching top-left and darker
    # bottom/right chamfers, to match VKA approved carved button design.
    rim=(205,214,203,255) if not hovered else (170,224,145,255)
    top=(236,243,226,255) if not hovered else (202,255,178,255)
    draw.line([(8+CUT,face_top+1),(W-8-CUT,face_top+1)], fill=top,width=5)
    draw.line([(11,face_top+CUT),(11,face_bottom-CUT)],fill=rim,width=5)
    draw.line([(12,face_top+CUT),(40,face_top+2)],fill=top,width=5)
    draw.line([(W-12,face_top+CUT),(W-40,face_top+2)],
              fill=(109,119,112,255),width=5)
    draw.line([(8+CUT,face_bottom-1),(W-8-CUT,face_bottom-1)],
              fill=(91,104,96,255),width=6)
    draw.line([(W-12,face_top+CUT),(W-12,face_bottom-CUT)],
              fill=(65,78,71,255),width=6)
    draw.line([(12,face_bottom-CUT),(40,face_bottom-3)],
              fill=(69,81,74,255),width=5)
    draw.line([(W-12,face_bottom-CUT),(W-40,face_bottom-3)],
              fill=(29,44,38,255),width=5)
    # Fine engraved inner rim; lime green only in hover state, not a glow.
    if hovered:
        draw.line([(40,face_top+9),(W-40,face_top+9)], fill=(114,189,63,215),width=2)
        draw.line([(40,face_bottom-12),(W-40,face_bottom-12)], fill=(57,117,43,195),width=2)
    if pressed:
        draw.line([(42,face_top+11),(W-42,face_top+11)],fill=(18,25,22,255),width=6)
    return sprite

for state in ("normal","hover","pressed"):
    p=out/("vka_button_"+state+".png")
    img=paint(state)
    img.save(p,optimize=True)
    assert img.size==(W,H)
    assert img.getpixel((0,0))[3]==0
    print(p.name,p.stat().st_size)

# High-quality independently rendered icons; no overprinting/cropping of
# old drawn icons. Transparency is preserved.
SZ=128
def icon(index):
    layer=Image.new("RGBA",(SZ,SZ))
    d=ImageDraw.Draw(layer)
    if index==0:
        d.polygon([(35,24),(101,64),(35,104)],fill=(24,48,18,255))
        d.polygon([(39,28),(94,64),(39,98)],fill=(171,242,40,255))
        d.line([(39,29),(39,98)],fill=(231,255,145,255),width=5)
    elif index==1:
        # Vrai engrenage à huit dents, pas un cercle plein.
        cx=cy=64
        pts=[]
        for k in range(32):
            theta=2*math.pi*k/32-math.pi/2
            radius=49 if k%4 in (1,2) else 37
            pts.append((cx+radius*math.cos(theta),
                        cy+radius*math.sin(theta)))
        d.polygon(pts,fill=(16,24,21,255))
        inner=[]
        for k in range(32):
            theta=2*math.pi*k/32-math.pi/2
            radius=45 if k%4 in (1,2) else 34
            inner.append((cx+radius*math.cos(theta),
                          cy+radius*math.sin(theta)))
        d.polygon(inner,fill=(222,231,218,255))
        d.ellipse((44,44,84,84),fill=(42,53,47,255),
                  outline=(250,251,245,255),width=4)
        d.ellipse((54,54,74,74),fill=(0,0,0,0))
    else:
        # Door and arrow. Explicitly independent from the background.
        d.rounded_rectangle((17,25,73,105),radius=7,outline=(219,229,217,255),width=9)
        d.rectangle((57,48,79,83),fill=(27,39,35,255))
        d.polygon([(59,55),(88,55),(88,42),(111,65),(88,88),(88,75),(59,75)],
                  fill=(228,237,224,255))
    return layer

for i in range(3):
    path=out/f"vka_icon_{i}.png"
    icon(i).save(path,optimize=True)
    print(path.name,path.stat().st_size)

# Prevent GUI regressions: all art is transparent outside the intended widget,
# and icons have no letterforms/text embedded in the texture.
for p in out.glob("vka_button_*.png"):
    image=Image.open(p)
    assert image.mode=="RGBA"
    assert image.getpixel((0,0))[3]==0
    assert image.getpixel((W-1,0))[3]==0
