#!/usr/bin/env python3
"""Reconstitue la maquette VKA approuvée en texture PNG pour Minecraft."""
from pathlib import Path
import hashlib
from PIL import Image

root=Path(__file__).resolve().parents[1]
source=root / "art/vka_menu_approved.webp"
destination=root / "src/client/resources/assets/vkamenu/textures/gui/vka_menu.png"
expected="fe24ae946a995acdc72fbcf223af316887a0b92f4edfebb6ff84e32316962315"
actual=hashlib.sha256(source.read_bytes()).hexdigest()
if actual!=expected:
    raise SystemExit(f"Ressource VKA modifiée : SHA-256 invalide ({actual})")
destination.parent.mkdir(parents=True,exist_ok=True)
with Image.open(source) as artwork:
    artwork.convert("RGB").resize((1672,941),Image.Resampling.LANCZOS).save(destination,"PNG",optimize=True)
print(f"Visuel VKA intégré : {destination} ({destination.stat().st_size} octets)")
