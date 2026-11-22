#!/usr/bin/env python3
"""VKA: real wide panorama outpaint, with protected ORIGINAL central art.

The fantasy scenery was generated specifically to extend the original left and
right sides. The illustration of the central VKA panel is restored from the
previous approved Minecraft resource; ONLY the newly generated lateral
scenery is used, feathered across the OUTER 160 pixels to remove seams.
This has no stretched 1/2 pixel strips, no mirrored castles/logos, and no
screenshot-cropped buttons: the Java client still draws real 3D controls.
"""
import argparse
import base64
import hashlib
import io
from pathlib import Path
from PIL import Image,ImageFilter

SOURCE=Path(__file__).resolve().parents[1]/"art/vka-panorama-generated.webp.b64"
SOURCE_SHA="0cbd7736a0b11cb1e851e7f6c5e7e1494290696314b73c696fb3fed2fc80abed"
ORIGINAL_SIZE=(3344,1882)
GEN_SIZE=(1931,814)
OUT_SIZE=(4464,1882)
FEATHER=160

parser=argparse.ArgumentParser()
parser.add_argument("--output",required=True)
args=parser.parse_args()
out=Path(args.output)
out.mkdir(parents=True,exist_ok=True)
original_path=out/"vka_menu_hd.png"
if not original_path.exists():
    raise SystemExit("Décor central VKA d'origine introuvable ; exécuter prepare_scene_v5.py avant")
with Image.open(original_path) as source:
    center=source.convert("RGB")
if center.size!=ORIGINAL_SIZE:
    raise RuntimeError(f"Taille du décor central invalide : {center.size}")

source_bytes=base64.b64decode("".join(SOURCE.read_text(encoding="ascii").split()),validate=True)
if hashlib.sha256(source_bytes).hexdigest()!=SOURCE_SHA:
    raise RuntimeError("L'illustration panoramique générée a été modifiée")
with Image.open(io.BytesIO(source_bytes)) as source:
    if source.size!=GEN_SIZE:
        raise RuntimeError(f"Panorama généré de dimensions inattendues : {source.size}")
    generated=source.convert("RGB").resize(OUT_SIZE,Image.Resampling.LANCZOS)
generated=generated.filter(ImageFilter.UnsharpMask(radius=1.1,percent=100,threshold=3))

x=(OUT_SIZE[0]-center.width)//2
mask=Image.new("L",center.size,255)
weights=[round(255*(3*(i/FEATHER)**2-2*(i/FEATHER)**3)) for i in range(FEATHER)]
# Put the transition well OUTSIDE the central panel, to keep its logo and art intact.
pixels=mask.load()
for col,value in enumerate(weights):
    for row in range(center.height):
        pixels[col,row]=value
        pixels[center.width-col-1,row]=value

generated.paste(center,(x,0),mask)
output=out/"vka_menu_panorama.png"
generated.save(output,optimize=True)
if generated.size!=OUT_SIZE:
    raise RuntimeError("Erreur de format de sortie")
print("VKA panorama built:",output,generated.size,output.stat().st_size,
      "bytes; center protected at x",x,"through",x+center.width)
