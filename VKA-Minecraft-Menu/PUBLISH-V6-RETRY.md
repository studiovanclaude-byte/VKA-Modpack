# Publication du correctif VKA Menu v6

Le workflow de publication précédent a été interrompu avant compilation parce que
le script de génération d'image avait été appelé sous un nom inexistant. Le
workflow corrige maintenant cette référence vers `prepare_scene_v5.py`.

Le code du correctif a déjà réussi les tests Minecraft/Fabric 26.3 :
https://github.com/studiovanclaude-byte/VKA-Modpack/actions/runs/45026543540

Publier en Release officielle puis mettre à jour le manifeste uniquement après
les tests et la comparaison exacte du JAR public.
