# Déploiement VKA Cités — séparation GitHub / Mindstrator

État au 25 novembre 2026 : **préparation GitHub disponible ; nouveau JAR client non encore publié**.

## Responsabilités

- **GitHub / VKA Luncher** : publie le JAR client unifié dans une release GitHub, puis référence exactement son nom, sa taille et son SHA-256 dans le manifeste. Le launcher synchronise automatiquement les fichiers du manifeste.
- **Mindstrator (serveur Minecraft)** : nécessite l'installation distincte du JAR **serveur** Cités compatible avec les mods serveur, suivie d'un redémarrage et de la consultation des logs.
- Changer `manifest.json` sur GitHub **ne transfère aucun fichier sur Mindstrator** et ne redémarre pas le serveur.

## État de référence

- Manifeste officiel : `manifest.json` — version `2026.11.24-ui-rc4-casino` au début de la préparation.
- JAR client officiel initial : `VKA-Unifie-26.3-TEST33.6-UI-RC4.jar` (SHA-256 : `f12a1fd51e33e3f736000b40dc88b348ed15127ece8f95758765bafa6d9086b4`).
- JAR serveur Cités déjà compilé localement : `C:\dev\vka-civ\cites\build\libs\vka-cites-0.1.0-dev.1.jar`.
- JAR client corrigé préparé localement : la RC4 avec `vka_nemesis_client 0.2.4-fortif.2`, `vka_cites`, `vka_ui`, le centrage des onglets et deux corrections de boutons. **Ce binaire corrigé n'est PAS présent dans les releases GitHub tant qu'il n'y est pas explicitement envoyé.**

## Publication sécurisée dans le manifeste

Workflow : [VKA Cités — publier un JAR déjà disponible sur GitHub](.github/workflows/vka-cites-publish.yml).

Le workflow demande le tag de la release, le nom du JAR, son SHA-256, la version du manifeste et le canal :

1. Vérifie l'existence du fichier dans une release GitHub du dépôt ;
2. Télécharge le JAR et contrôle son SHA-256 **avant toute modification** ;
3. Repère l'unique entrée `mods/VKA-Unifie-26.3-*.jar` du manifeste ;
4. Écrit `manifest-cites-test.json` pour le canal test, ou `manifest.json` pour la production ;
5. Refuse la promotion en production sans confirmation explicite et refuse d'utiliser une préversion GitHub pour la production.

**Ne pas invoquer le workflow tant que le nouveau JAR corrigé n'a pas été publié comme asset dans une release.** Ce dépôt ne contient pas les sources compilées du client corrigé et ne peut pas fabriquer ce JAR en modifiant uniquement le manifeste.

## Sur Mindstrator

Avant toute installation, sauvegarder le monde, les fichiers de configuration et le dossier `mods` du serveur. Arrêter le serveur. Vérifier qu'il n'y a ni ancien `vka-cites` ni duplicata d'identifiant Fabric. Installer le JAR Cités **serveur** (pas le JAR client UI), redémarrer, puis contrôler `latest.log` et les erreurs de chargement Fabric.

Ne pas déposer le JAR client de l'inventaire Minecraft dans le serveur dédié. Le JAR client unifié appartient au modpack du joueur via le VKA Luncher.

En cas d'échec : remettre les JAR serveur sauvegardés et restaurer le manifeste GitHub précédent.

## Ne pas confondre

- `VKA-Menu-3D-CLIENT-26.3-POLISHED-v9.jar` : menu 3D distinct, conservé.
- `vka-client-home.jar.disabled` : ancien mod désactivé, ne pas réactiver.
- Le bouton « Civilisation » n'est visible que si la bonne version du **client** est chargée et compatible avec le serveur.
