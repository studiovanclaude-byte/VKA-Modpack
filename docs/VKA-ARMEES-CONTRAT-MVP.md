# VKA Armées — contrat de développement MVP (27 novembre 2026)

> **État : contrat technique en préparation, pas un mod installé ni une fonctionnalité livrée.** Ce document accompagne les sources locales `C:\dev\vka-civ`, qui ne sont pas publiées dans ce dépôt de distribution. Aucun changement de `manifest.json`, aucune installation Minestrator.

## But du MVP

Permettre au fondateur d'une Cité VKA de recruter et gérer une armée jusqu'à **25 soldats de campagne** via les bâtiments/PNJ, sans commande pour le joueur. Les gardes NPC Maker sont comptés séparément et Village Guard reste autonome.

### Responsabilités

| Composant | Responsabilité | Ne doit pas faire |
| --- | --- | --- |
| **VKA Cités / VKA Armées** | Source de vérité sur ville, propriétaire, recherche, recrutement, quota, ressources, soldes, ordres, persistance, permissions | Dépendre d'une entité chargée pour garantir une opération économique |
| **NPC Maker** | Personnages de caserne, sentinelles, patrouilles des claims, interactions | Compter parmi les 25 soldats d'expédition |
| **Village Guard** | Défense ordinaire des villageois | Être réquisitionné comme soldat de campagne VKA |
| **Living NPC** | Fournir *éventuellement* des avatars/comportements militaires sur le terrain | Être la source de vérité des effectifs, stocks, équipes et autorisations |
| **Open Parties and Claims (OPAC)** | Appartenance et protection territoriale vérifiées côté serveur | Autoriser implicitement les actions si groupe/permissions sont absents |
| **VKA UI** | Écrans semi-RP : caserne, capitaine, entrepôt, mission et retour | Exposer des commandes joueurs |

## Attention : limites de Living NPC

La version publiée **Living NPC 1.0.5 pour Minecraft 26.3** est diffusée comme **datapack**. La documentation publique décrit **deux équipes configurées manuellement** (`team1`, `team2`) et des fonctions `living_npc:player_guard/team1/guard1...` / `team2/guard10...`. Cela ne correspond pas à des armées dynamiques par Cité, ni au quota VKA de 25. **Il faut auditer l'archive et ses droits avant toute intégration.**

Le projet amont est indiqué **All Rights Reserved** ; ne pas recopier, redistribuer, modifier ou reconditionner ses fichiers sans vérifier les autorisations. L'adaptateur VKA doit pouvoir refuser une capacité non disponible et laisser l'état de la Cité intact. Ne pas présumer que des fonctions upstream supportent des nombres arbitraires de villes, des propriétaires UUID, des sauvegardes ou les commandes de retrait.

## Tranche 1 : domaine pur et transactions

À réaliser dans les sources **locales** du module Java VKA, avec tests unitaires :

1. `ArmyState` / `SoldierRecord` persistés par `cityId` et version de schéma ; `soldierId` UUID stable, rang, statut, équipement et dates logiques.
2. Quota : `recruited + training + ready + deployed + wounded <= 25` par Cité ; les morts `fallen` ne prennent pas de place ; la création d'un 26e est refusée **avant** débit.
3. Recrutement : contrôle fondateur/permissions côté serveur, bâtiment `caserne_1` actif, recherche militaire acquise, coût disponible ; validation et débit **atomiques**. Nom et skin ne sont pas choisis pour contourner les permissions.
4. Approvisionnement : réservation indivisible des unités de nourriture/eau/solde et de l'équipement dans les stocks Cités ; rejet sans consommation partielle. Durées autorisées : **1, 3 ou 7 jours**.
5. Campagne : `PREPARED -> DEPLOYED -> RETURNING -> COMPLETED`, plus `ABORTED`; réconciliation après redémarrage, idempotence des ordres et absence de duplication de butin/stock.
6. Si un PNJ/avatar refuse d'apparaître ou qu'un chunk n'est pas chargé, **on n'annule pas silencieusement le soldat** et on ne crée pas de doublon : ordre en attente, nouvelle tentative sûre, identité stable.
7. Afficher des erreurs utiles dans la langue du joueur : « caserne absente », « recherche manquante », « quota atteint », « provisions insuffisantes », « territoire non autorisé », « génération en attente ».

## Tranche 2 : adaptateurs de jeu et UI

- `ArmyWorldAdapter` : détecter si le backend vivant est compatible, créer/retirer un avatar en sûreté, maintenir `soldierId <-> entity UUID` et signaler les erreurs. Aucun mécanisme interne de Living NPC n'est supposé sans inspection du datapack.
- `ArmyClaimsAdapter` : droits sur ville/claims OPAC, aucun passage automatique si `argonauts_guilds` ou `prometheus` ne sont pas enregistrés.
- Capitaine/PNJ caserne : recrutement et consultation de la garnison via dialogues/UI, pas de commandes joueurs.
- Tour de garde : sentinelles NPC Maker distinctes ; jamais enrôlées comme soldats d'expédition.
- Écuries/palefrenier : montures débloquées à la bonne étape ; l'armée ne monte pas des créatures non compatibles tant que l'adaptateur n'a pas été testé.
- Gestion des combats PvE/PvP : règles serveur et créneaux autorisés, garde-fous sur les claims et les participants ; invasions barbares saison 2 hors MVP.

## Invariants et tests d'acceptation obligatoires

- 25 soldats recrutés dans ville A ; le 26e refusé sans débit ; ville B conserve son propre quota.
- 3 gardes NPC Maker et des Village Guards existants ne font pas baisser le quota d'armée.
- Commande concurrente de recrutement depuis deux clients : pas de double dépense ni de 26e soldat.
- Échec de génération d'avatar : état conservé, aucun doublon au redémarrage.
- Préparation 1, 3, 7 jours : prélèvements exacts ; demande 2 jours refusée ; manque d'eau ne consomme ni nourriture ni solde.
- Redémarrage pendant `DEPLOYED` et `RETURNING` : aucun soldat/stock perdu ou dupliqué.
- Anciennes sauvegardes des Cités sans module d'armées : migration par défaut vers garnison vide.
- Échec du contrôleur OPAC ou droits inconnus : recrutement et expédition refusés, pas de contournement.
- Soldat tué : perte comptabilisée une fois, avatar retiré, quota libéré selon règle VKA, restitution de campagne contrôlée.
- Aucune commande d'administration accessible aux joueurs pour recruter, combattre, changer de faction ou générer les soldats.
- Tests client/serveur Minecraft 26.3 Fabric : NPC Maker présent **des deux côtés** pour éviter les registres inconnus ; sauvegarde préalable des mondes.

## Ordre de livraison

1. **Contrat + schéma de données + tests du domaine** dans le dépôt source local VKA (non disponible dans GitHub VKA-Modpack).
2. **Contrôle réel de Living NPC** en environnement isolé et analyse de ses fonctions / compatibilité ; décision d'adaptateur ou de remplacement sans toucher à l'amont.
3. Recrutement dans la caserne et réservations des stocks ; puis retour d'expédition.
4. Gardes NPC Maker, tours de garde, intégration OPAC et UI de commandement.
5. Tests complets Gradle/GameTests + deux clients avant publication sur un canal d'essai.
6. **Aucune modification de `manifest.json` de production ou de Minestrator sans validation spécifique.**

### Sources vérifiées

- https://modrinth.com/datapack/living-npc
- https://modrinth.com/datapack/living-npc/version/1.0.5

### Blocage connu

Les sources actives de `C:\dev\vka-civ` n'existent pas dans ce dépôt GitHub. Les changements à cette documentation ne compilent **aucun** code Cités et n'influencent pas l'installation sur le PC. L'intégration Java doit repartir de ces sources locales, jamais d'un JAR décompilé ni d'une ancienne archive supposée à jour.
