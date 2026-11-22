# VKA Menu 3D v6 — correctif d'affichage confirmé

Version v5 affichée chez le joueur : une large bande de matière grise débordait des boutons jusqu'au bord droit de l'écran.

Cause corrigée : l'appel de rendu recevait `x+w-2*inset` comme largeur, alors qu'il fallait `w-2*inset` dans `GuiGraphicsExtractor.blit`.

La v6 ajoute des contrôles de non-débordement, agrandit les libellés et évite de répéter des éléments du décor dans les extensions latérales. Elle conserve les boutons interactifs 3D, le son au survol et le cadrage du logo.

Compilation et tests sur Minecraft Fabric 26.3 réussis :
https://github.com/studiovanclaude-byte/VKA-Modpack/actions/runs/45026543540

Distribution : nouvelle Release GitHub, vérification stricte de l'asset puis remplacement du JAR v5 par le JAR v6 dans le manifeste officiel. Aucun fichier à déplacer côté joueur.
