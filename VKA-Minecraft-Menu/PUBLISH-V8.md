# VKA Menu Minecraft 26.3 — correction de raccord panoramique v8

Le panorama v7 superposait le panneau central provenant d'une ancienne
illustration sur un autre panorama généré. Dans la zone de transition,
une double lanterne et des détails de château / feuillage étaient
visibles en double : défaut confirmé dans la capture du joueur.

La version v8 utilise une SEULE source panoramique continue. Seul
l'intérieur du panneau, derrière les boutons peints, est nettoyé pour
laisser les vrais boutons Minecraft 3D dessiner leurs états hover/pressed.
Aucun découpage ou fondu entre deux paysages n'est autorisé.

Ce changement est limité à la ressource graphique, pas aux actions des
boutons, à la gestion Microsoft, au VKA Luncher ou au serveur.

Test de compilation Minecraft/Fabric 26.3 et de comparaison des paysages
réussi : https://github.com/studiovanclaude-byte/VKA-Modpack/actions/runs/45029650253

La publication contient une nouvelle Release avec un JAR unique. Le
manifeste officiel ne peut être modifié qu'après contrôle de la taille,
du SHA-256 et vérification HTTP du JAR public.

La validation finale du rendu dans Minecraft reste à effectuer sur PC.
