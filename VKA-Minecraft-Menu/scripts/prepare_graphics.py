#!/usr/bin/env python3
"""Prépare l'illustration VKA à partir du menu publié (sans créer un nouveau dessin).
Construit le fond 2x et les TROIS boutons pierre séparés, pour un véritable
mouvement 3D lors du survol/clic, sans overlay vert lumineux.
"""
from pathlib import Path
import argparse
import io
import zipfile
from PIL import Image, ImageFilter

BASE = (1672, 941)
RECTS = ((450, 465, 748, 87), (450, 574, 748, 83), (450, 682, 748, 88))
ART_PATH = "assets/vkamenu/textures/gui/vka_menu.png"
UP = 2

parser = argparse.ArgumentParser()
parser.add_argument("--jar", required=True, help="JAR VKA 3D source, déjà public")
parser.add_argument("--output", required=True, help="Répertoire de ressources GUI du mod")
args = parser.parse_args()

with zipfile.ZipFile(args.jar) as archive:
    base = Image.open(io.BytesIO(archive.read(ART_PATH))).convert("RGB")
if base.size != BASE:
    raise RuntimeError(f"Illustration inattendue: {base.size}, attendue {BASE}")

out = Path(args.output)
out.mkdir(parents=True, exist_ok=True)

# Conserver chaque bouton avec son relief, ses pictogrammes et son texte
# (un PNG distinct par bouton pour l'animer en translation verticale).
for idx, (x,y,w,h) in enumerate(RECTS):
    sprite = base.crop((x,y,x+w,y+h))
    sprite = sprite.resize((w*UP,h*UP), Image.Resampling.LANCZOS)
    sprite = sprite.filter(ImageFilter.UnsharpMask(radius=1.1, percent=115, threshold=3))
    sprite.save(out/f"vka_button_{idx}.png", optimize=True)

# L'ancienne illustration est plate : ses boutons étaient directement peints
# dans le décor. On restaure le fond du panneau derrière les boutons pour
# qu'ils puissent réellement monter / descendre sans doublon visible.
background = base.copy()
pixels = background.load()
source = base.load()
from PIL import ImageDraw, ImageFilter

for x,y,w,h in RECTS:
    left,right=x-15,x+w+15
    top,bottom=y-10,y+h+13
    patch=Image.new("RGB",(right-left,bottom-top))
    px=patch.load()
    for yy in range(top,bottom):
        l=source[x-28, min(BASE[1]-1,max(0,yy))]
        r=source[x+w+27, min(BASE[1]-1,max(0,yy))]
        for xx in range(left,right):
            t=(xx-left)/max(1,right-left-1)
            # dégradé sombre vert/noir, dans le prolongement du fond original
            px[xx-left,yy-top]=tuple(int(l[c]*(1-t)+r[c]*t) for c in range(3))
    # Couper l'ancien bouton puis adoucir les bords pour garder la
    # continuité de la texture centrale du panneau.
    mask=Image.new("L",patch.size,0)
    draw=ImageDraw.Draw(mask)
    draw.rectangle((8,6,patch.width-9,patch.height-7),fill=255)
    mask=mask.filter(ImageFilter.GaussianBlur(radius=5))
    background.paste(patch,(left,top),mask)

background = background.resize((BASE[0]*UP,BASE[1]*UP),Image.Resampling.LANCZOS)
background = background.filter(ImageFilter.UnsharpMask(radius=1.2,percent=130,threshold=3))
background.save(out/"vka_menu_hd.png",optimize=True)

for file in sorted(out.glob("*.png")):
    with Image.open(file) as image:
        print(file.name,image.size,file.stat().st_size,"bytes")
assert len(list(out.glob("*.png"))) == 4
