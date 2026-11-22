# REALISTIC VEHICLES — ASSET & DEPENDENCY LICENSES

> Audit continu de toutes les dépendances techniques et ressources (Règle 19).
> Aucune ressource sans licence claire et redistribuable ne doit être embarquée dans `RealisticVehicles-1.21.1.jar`.

## 1. Dépendances logicielles (Build / Runtime)

| Composant | Version | Licence | Statut | Notes |
|---|---|---|---|---|
| Minecraft Java Edition | 1.21.1 | Mojang EULA (fourni par le joueur) | Externe (non embarqué) | Cible stricte |
| NeoForge | 21.1.267 | LGPL-2.1 | Chargeur (non embarqué) | Seul mod loader requis |
| JUnit Jupiter (Tests) | 5.11.3 | EPL-2.0 | Test uniquement (non embarqué) | Exécution des tests unitaires |
| LWJGL Vulkan (`lwjgl-vulkan`) | 3.3.3 | BSD-3-Clause | Embarqué (`jarJar`) dans le JAR unique | Bindings natifs Vulkan |
| SLF4J API | Fourni par Minecraft/NeoForge | MIT | Fourni par runtime | Logging |

## 2. Ressources embarquées dans le mod

| Ressource | Auteur | Licence | Fichier dans le dépôt |
|---|---|---|---|
| Code source Realistic Vehicles | Projet Realistic Vehicles | Propriétaire / Projet | `src/main/java/**` |
| Fichiers de langue (`en_us.json`, `fr_fr.json`) | Projet Realistic Vehicles | Projet | `src/main/resources/assets/realisticvehicles/lang/` |
| Texture unie provisoire (`white.png`) | Générée programmatiquement (1x1 px) | CC0 / Projet | `src/main/resources/assets/realisticvehicles/textures/entity/` |

## 3. Archives locales fournies par l'utilisateur (non commitées dans Git)

| Fichier local | Statut | Décision |
|---|---|---|
| `mercedes-benz-slk-55-amg-2012.zip` | Fourni localement par l'utilisateur (`Mercedes_SLK55_AMG_2012_FM7_Sketchfab.glb`). | Chargé dynamiquement à la demande en local (`GlbModelRegistry`) pour le preset `slk55` sans être commité dans Git ni redistribué dans le JAR public. |

## 4. Bibliothèques prévues pour Phase 3 (Vulkan — à vérifier à l'intégration)

| Composant prévu | Licence prévue | Compatibilité distribution JAR unique |
|---|---|---|
| LWJGL Vulkan (`lwjgl-vulkan`) | BSD-3-Clause | Compatible (avec notice de licence) |
| LWJGL VMA (`lwjgl-vma`) | BSD-3-Clause / MIT (AMD VMA) | Compatible (avec notice de licence) |
| LWJGL Shaderc ou SPIR-V précompilé | Apache-2.0 / BSD-3-Clause | Compatible (préférer SPIR-V précompilé au build pour alléger le JAR) |
