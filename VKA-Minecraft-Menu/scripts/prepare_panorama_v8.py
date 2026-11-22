#!/usr/bin/env python3
"""VKA v8: panorama d'UN SEUL TENANT, sans coutures ni objets dupliqués.

La v7 mélangeait DEUX versions incompatibles du même paysage sur 160 pixels,
recréant une lanterne fantôme, des feuillages répétés et des raccords verticaux.
La v8 conserve les pixels d'une seule illustration panoramique approuvée,
et supprime uniquement les boutons BAKED à l'intérieur du panneau.
Les boutons sont dessinés par Minecraft comme vrais contrôles indépendants.
"""
import argparse
import base64
import hashlib
import io
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

SOURCE = Path(__file__).resolve().parents[1] / "art/vka-panorama-generated.webp.b64"
SOURCE_SHA = "0cbd7736a0b11cb1e851e7f6c5e7e1494290696314b73c696fb3fed2fc80abed"
SOURCE_SIZE = (1931, 814)
OUTPUT_SIZE = (4464, 1882)

parser = argparse.ArgumentParser()
parser.add_argument("--output", required=True)
args = parser.parse_args()
out = Path(args.output)
out.mkdir(parents=True, exist_ok=True)

binary = base64.b64decode("".join(SOURCE.read_text(encoding="ascii").split()), validate=True)
if hashlib.sha256(binary).hexdigest() != SOURCE_SHA:
    raise RuntimeError("La ressource panoramique VKA a changé : publication bloquée.")
with Image.open(io.BytesIO(binary)) as original:
    if original.size != SOURCE_SIZE:
        raise RuntimeError(f"Dimensions inattendues : {original.size}")
    generated = original.convert("RGB")

# IMPORTANT : aucun collage, aucune ancienne image centrale, aucun fondu
# entre deux paysages. Le château, l'arbre et les lanternes sont tous issus
# d'une seule illustration générée, donc leurs contours sont cohérents.
#
# Nettoyage exclusivement au milieu du panneau, à l'endroit où la source
# contient des boutons peints. Le reste du panorama reste pixel-identique.
# Remplissage par le matériau vert/noir existant de part et d'autre des
# boutons, suivi d'un très petit feather interne aux bordures du panneau.
left, top, right, bottom = 585, 382, 1334, 702
src = generated.load()
blank = generated.copy()
pixels = blank.load()
for y in range(top, bottom):
    # Prélèvements dans le panneau hors des boutons, sans capturer les objets.
    lc = tuple(sum(src[x, y][c] for x in range(570, 584)) / 14.0 for c in range(3))
    rc = tuple(sum(src[x, y][c] for x in range(1335, 1345)) / 10.0 for c in range(3))
    for x in range(left, right):
        t = (x - left) / (right - left - 1)
        pixels[x, y] = tuple(round(lc[c] * (1-t) + rc[c] * t) for c in range(3))

mask = Image.new("L", SOURCE_SIZE, 0)
ImageDraw.Draw(mask).rectangle((left+5, top+5, right-5, bottom-5), fill=255)
mask = mask.filter(ImageFilter.GaussianBlur(radius=3))
panorama = Image.composite(blank, generated, mask)

# La texture est simplement redimensionnée UNE SEULE fois. Aucune autre
# illustration ne sera superposée. Les contrôles Java restent totalement
# indépendants du fond.
panorama = panorama.resize(OUTPUT_SIZE, Image.Resampling.LANCZOS)
output = out / "vka_menu_panorama.png"
panorama.save(output, optimize=True)
assert panorama.size == OUTPUT_SIZE

# Les paysages à l'extrême gauche et droite restent strictement ceux de la
# même source, évitant les fantômes et coutures de la v7.
reference = generated.resize(OUTPUT_SIZE, Image.Resampling.LANCZOS)
from PIL import ImageChops
assert ImageChops.difference(panorama.crop((0, 0, 1200, OUTPUT_SIZE[1])),
                             reference.crop((0, 0, 1200, OUTPUT_SIZE[1]))).getbbox() is None
assert ImageChops.difference(panorama.crop((3250, 0, OUTPUT_SIZE[0], OUTPUT_SIZE[1])),
                             reference.crop((3250, 0, OUTPUT_SIZE[0], OUTPUT_SIZE[1]))).getbbox() is None
assert ImageChops.difference(panorama.crop((0, 0, OUTPUT_SIZE[0], 840)),
                             reference.crop((0, 0, OUTPUT_SIZE[0], 840))).getbbox() is None
print("Panorama VKA v8 sans collage :", output, OUTPUT_SIZE, output.stat().st_size, "bytes")
