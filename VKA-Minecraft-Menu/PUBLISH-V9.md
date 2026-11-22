# VKA Menu v9 — boutons pierre 3D biseautés, arrière-plan nettoyé

Retour visuel du joueur sur la v8 : le décor panoramique, le son de survol et les actions sont validés. Les anciens boutons gris rectangulaires et les pixels fantômes autour des boutons sont rejetés.

## Réalisation

- Le décor panoramique existant est conservé, sans changer les paysages ni le logo.
- La scène sous les boutons est nettoyée pour supprimer les anciennes bordures peintes, textes et ombres qui provoquaient les bavures de pixels.
- Les 3 boutons reçoivent une géométrie de pierre taillée avec coins transparent, contour métallique argenté, matière de pierre indépendante, ombres réelles et faces latérales.
- Trois états distincts (repos, survol discret vert, enfoncé), sans l'ancien halo lumineux.
- Icônes indépendantes nettes (triangle, engrenage 8 dents, sortie).
- Les textes restent dessinés par Minecraft, les clics, paramètres, reconnexion, quitter et le son de survol restent opérationnels.

## Validation

Compilation et contrôles des ressources sur Minecraft Fabric 26.3 :
https://github.com/studiovanclaude-byte/VKA-Modpack/actions/runs/45031293934

L'installation est effectuée uniquement après publication, vérification du JAR téléchargé et mise à jour du manifeste officiel.
Le rendu interactif final nécessite encore un contrôle utilisateur sur un vrai client Minecraft.
