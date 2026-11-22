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
RECTS = ((446, 459, 758, 104), (446, 566, 758, 103), (446, 676, 758, 102))
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

# L'illustration d'origine intègre les trois anciens boutons avec leurs ombres.
# Remplacer tout leur bloc par un panneau sombre homogène élimine les restes
# de bordures qui devenaient visibles pendant le survol des nouveaux boutons.
background = base.copy()
source = base.load()
from PIL import ImageDraw

left, top, right, bottom = 429, 441, 1229, 792
patch = Image.new("RGB", (right-left, bottom-top))
pp = patch.load()
for yy in range(top, bottom):
    # Fond noir-vert réel du panneau, échantillonné à l'extérieur des boutons.
    l = source[413, yy]
    r = source[1242, yy]
    for xx in range(left, right):
        t = (xx-left) / (right-left-1)
        pp[xx-left,yy-top] = tuple(round(l[c]*(1-t) + r[c]*t) for c in range(3))
mask = Image.new("L", patch.size, 0)
draw = ImageDraw.Draw(mask)
draw.rectangle((7,7,patch.width-8,patch.height-8),fill=255)
mask = mask.filter(ImageFilter.GaussianBlur(radius=4))
background.paste(patch, (left,top), mask)

background = background.resize((BASE[0]*UP,BASE[1]*UP),Image.Resampling.LANCZOS)
background = background.filter(ImageFilter.UnsharpMask(radius=1.2,percent=130,threshold=3))
background.save(out/"vka_menu_hd.png",optimize=True)

for file in sorted(out.glob("*.png")):
    with Image.open(file) as image:
        print(file.name,image.size,file.stat().st_size,"bytes")
assert len(list(out.glob("*.png"))) == 4
