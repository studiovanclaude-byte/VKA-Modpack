#!/usr/bin/env python3
"""VKA v9: preserve v8 seamless landscape, clean all baked button artifacts.

Only the old three painted buttons and their shadows within the CENTRAL DARK
PANEL are inpainted, using its own left/right unobstructed dark material.
Landscapes, water, castle, frame and VKA logo remain unchanged.
"""
import argparse, base64, hashlib, io
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageChops

SOURCE=Path(__file__).resolve().parents[1]/"art/vka-panorama-generated.webp.b64"
SHA="0cbd7736a0b11cb1e851e7f6c5e7e1494290696314b73c696fb3fed2fc80abed"
SOURCE_SIZE=(1931,814)
OUTPUT_SIZE=(4464,1882)
ap=argparse.ArgumentParser()
ap.add_argument("--output",required=True)
args=ap.parse_args()
out=Path(args.output);out.mkdir(parents=True,exist_ok=True)

blob=base64.b64decode("".join(SOURCE.read_text().split()),validate=True)
assert hashlib.sha256(blob).hexdigest()==SHA,"La scène VKA a été modifiée"
with Image.open(io.BytesIO(blob)) as image:
    assert image.size==SOURCE_SIZE
    original=image.convert("RGB")

# Outer bounds encompass all three BAKED buttons, bevels and drop shadows.
# Bottom is kept above the genuine ornamental frame along the bottom.
L,T,R,B=601,384,1322,704
clean=original.copy()
op=original.load(); cp=clean.load()
for y in range(T,B):
    # Limit samples to the panel's unlit region; do not spread the original
    # bright ornamental green stripe along the bottoms of the old controls.
    sy=min(y,656)
    left=[int(sum(op[x,sy][c] for x in range(583,596))/13) for c in range(3)]
    right=[int(sum(op[x,sy][c] for x in range(1327,1341))/14) for c in range(3)]
    for x in range(L,R):
        t=(x-L)/(R-L-1)
        cp[x,y]=tuple(round((1-t)*left[c]+t*right[c]) for c in range(3))

# Feather only in dark, empty panel margin at least 15px away from all
# original button borders. Corners and edges remain smooth, not smeared.
mask=Image.new("L",SOURCE_SIZE,0)
ImageDraw.Draw(mask).rectangle((L+9,T+10,R-9,B-9),fill=255)
mask=mask.filter(ImageFilter.GaussianBlur(5))
scene=Image.composite(clean,original,mask)
scene=scene.resize(OUTPUT_SIZE,Image.Resampling.LANCZOS)
dest=out/"vka_menu_panorama.png"
scene.save(dest,optimize=True)

reference=original.resize(OUTPUT_SIZE,Image.Resampling.LANCZOS)
# Only the central button zone is allowed to change.
for box in ((0,0,1250,1882),(3200,0,4464,1882),(0,0,4464,820)):
    assert ImageChops.difference(scene.crop(box),reference.crop(box)).getbbox() is None,box
# Scan regions where all three old buttons had previously been painted:
# very low luminance must remain, no silver frame or white glyph.
scale=OUTPUT_SIZE[0]/SOURCE_SIZE[0]
for x0,y0,x1,y1 in ((645,421,1250,460),(645,517,1250,559),(645,617,1250,655)):
    region=scene.crop(tuple(round(z*scale) for z in (x0,y0,x1,y1)))
    extrema=region.getextrema()
    assert max(channel[1] for channel in extrema)<85, (x0,y0,extrema)
print("Clean VKA v9 pano",dest,scene.size,dest.stat().st_size)
