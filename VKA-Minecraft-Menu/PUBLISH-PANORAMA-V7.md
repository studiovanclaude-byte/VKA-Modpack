# VKA Menu v7 — vrai panorama étendu aux côtés

Le rendu v6 contenait des bandes horizontales horribles parce que les bords du
visuel étaient étirés pour combler les bandes latérales sur écran large.

## Correctif
- Nouvelle illustration de paysage générée spécifiquement pour les côtés gauche/droit.
- Référence centrale originale du panneau/logo VKA préservée au pixel près.
- Texture panoramique finale 4464 × 1882 ; source artistique intégrée à ce dépôt,
  et vérifiée par SHA-256 avant décodage.
- Projection dynamique au ratio de la fenêtre (16:9, 21:9, 4:3), sans aucun
  étirement des bandes de bord ni répétition de château.
- Vrais boutons 3D v6 et son au survol inchangés.
- Aucun changement aux fichiers des mods du serveur.

## Tests
Compilation Fabric Minecraft 26.3 + vérification des textures et du cadrage :
https://github.com/studiovanclaude-byte/VKA-Modpack/actions/runs/45028259094

Le lancement client doit encore être vérifié sur un PC réel afin de confirmer
visuellement la transition après déconnexion. La Release et le manifeste sont
publiés seulement après compilation et comparaison byte-à-byte du JAR.
