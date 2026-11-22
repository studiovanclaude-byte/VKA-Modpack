#!/usr/bin/env python3
"""VKA 3D menu V4: new clean art, individual moving stone buttons.

The approved new clean image is stored as base64 WebP *source*, not as a screenshot
upscaled from a low-resolution menu. This script decodes the full artwork,
removes the baked-in buttons from the centre background, then exports
three individual high-quality button sprites for their actual 3D movement.
"""
from pathlib import Path
import argparse
import base64
import hashlib
import io
from PIL import Image, ImageDraw, ImageFilter

BASE = (1672, 941)
RECTS = (
    (432, 472, 790, 101),
    (432, 587, 790, 101),
    (432, 702, 790, 101),
)
UP = 2
ART = Path(__file__).resolve().parents[1] / "art/vka-clean-native-v4.webp.b64"
EXPECTED_WEBP_BYTES = 227020

parser = argparse.ArgumentParser()
parser.add_argument("--output", required=True)
args = parser.parse_args()

encoded = "".join(ART.read_text(encoding="ascii").split())
source_bytes = base64.b64decode(encoded,validate=True)
if len(source_bytes) != EXPECTED_WEBP_BYTES:
    raise RuntimeError(f"Fichier image VKA incorrect : {len(source_bytes)}")
with Image.open(io.BytesIO(source_bytes)) as input_image:
    base = input_image.convert("RGB")
if base.size != BASE:
    raise RuntimeError(f"Format source inattendu : {base.size}")

output = Path(args.output)
output.mkdir(parents=True,exist_ok=True)

for index,(x,y,width,height) in enumerate(RECTS):
    sprite=base.crop((x,y,x+width,y+height))
    sprite=sprite.resize((width*UP,height*UP),Image.Resampling.LANCZOS)
    sprite=sprite.filter(ImageFilter.UnsharpMask(radius=0.85,percent=85,threshold=2))
    sprite.save(output/f"vka_button_{index}.png",optimize=True)

# Dessous des boutons : reconstruire une surface panneau noir-vert continue.
# Sans cela, une copie du bouton reste dessinée quand son sprite se soulève.
background=base.copy()
from PIL import ImageDraw
left,top,right,bottom = 413,458,1244,823
patch=Image.new("RGB",(right-left,bottom-top))
pp=patch.load()
pixels=base.load()
for yy in range(top,bottom):
    l=pixels[398,yy]
    r=pixels[1259,yy]
    for xx in range(left,right):
        t=(xx-left)/(right-left-1)
        # Dégradé panneau sombre avec texture d'origine conservée sur les côtés.
        pp[xx-left,yy-top]=tuple(round((1-t)*l[c]+t*r[c]) for c in range(3))
mask=Image.new("L",patch.size,0)
draw=ImageDraw.Draw(mask)
draw.rectangle((6,6,patch.width-7,patch.height-7),fill=255)
mask=mask.filter(ImageFilter.GaussianBlur(radius=5))
background.paste(patch,(left,top),mask)
background=background.resize((BASE[0]*UP,BASE[1]*UP),Image.Resampling.LANCZOS)
background=background.filter(ImageFilter.UnsharpMask(radius=1.0,percent=105,threshold=2))
background.save(output/"vka_menu_hd.png",optimize=True)

for file in sorted(output.glob("*.png")):
    with Image.open(file) as image:
        print(file.name,image.size,file.stat().st_size,"bytes")
assert len(list(output.glob("*.png")))==4
print("New VKA clean art SHA-256",hashlib.sha256(source_bytes).hexdigest())
