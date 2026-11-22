# VKA Menu 3D — mise à jour HD v2

Cette version remplace le rendu à image centrée et bouton surligné en vert de la v1.

- **Plein écran** : cadrage de couverture (cover) qui remplit l'affichage Minecraft.
- **Graphismes** : fond retravaillé en 3344 × 1882 pixels, filtrage plus net.
- **Boutons 3D** : trois textures séparées en pierre, sans éclairage vert déclenché au survol ;
  au survol le bouton monte avec une ombre plus longue, à l'appui il s'enfonce.
- **Actions conservées** : Rejoindre VKA, Options et touches, Quitter le jeu.
- **Client Minecraft** : Fabric 26.3. Aucun changement côté serveur.
- **Distribution** : nouveau JAR dans une Release VKA-Modpack, remplacement automatique de l'ancien JAR dans `manifest.json`.

Compilation/test des ressources Minecraft 26.3 réussis sur la branche avant publication :
https://github.com/studiovanclaude-byte/VKA-Modpack/actions/runs/45021050391

Cette Release v2 est distincte de la v1. Le manifeste pointe vers le binaire officiel
uniquement après vérification stricte de sa taille et de son SHA-256.
