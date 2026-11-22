# REALISTIC VEHICLES — IMPLEMENTATION PLAN

## 1. Vision et contraintes structurantes

- **Cible stricte** : Minecraft Java Edition **1.21.1** + **NeoForge 21.1.x** + **Java 21**.
- **Distribution unique** : un seul fichier `build/libs/RealisticVehicles-1.21.1.jar` sans aucun mod tiers requis.
- **Découplage strict** : le moteur physique (`com.realisticvehicles.physics.*`) et le noyau (`com.realisticvehicles.core.*`) n'ont **aucune** dépendance envers Minecraft, NeoForge ou le renderer (vérifié automatiquement par `tools/check-architecture.py`).
- **Limite stricte de taille** : aucun fichier source ne dépasse **200 lignes** (vérifié automatiquement par `tools/check-file-length.py` et câblé dans `./gradlew check`).
- **Ordre de priorité strict** :
  1. Phase 0 : Projet compilable, outillage qualité, démarrage NeoForge.
  2. Phase 1 : Première voiture jouable conduite par notre propre moteur physique (châssis rigide, 4 roues, suspensions, traction, freinage, direction Ackermann, collisions blocs, contrôles Z/S/Q/D/Espace reconfigurables).
  3. Phase 2 : Physique avancée (Pacejka, anti-roulis, transfert de charge, différentiels, ABS/TCS, sauts/retournements, collisions véhicule-véhicule).
  4. Phase 3 : Intégration Vulkan native embarquée (LWJGL Vulkan + VMA + SPIR-V + chargeur GLB/glTF 2.0 + remplacement/intégration du pipeline de rendu Minecraft sans mod externe).
  5. Phases 4–12 : Modèle 3D réaliste légalement redistribuable, caméras/HUD, animations complètes joueur/ouvrants, dégâts mécaniques & déformations hybrides node-beam/morph, interactions/clés/carburant, audio dynamique, multijoueur autoritaire, optimisation extrême & livraison finale.

## 2. Stratégie technique par couche

### 2.1 Physique automobile (Phases 1–2)
- **Unités SI cohérentes** : mètres (1 bloc Minecraft = 1 m), kilogrammes, secondes, Newtons, Newton-mètres, radians.
- **Intégration à pas fixe** : `FIXED_DT = 1.0 / 120.0` s (120 Hz) avec accumulateur borné (`MAX_SUBSTEPS = 8`) pour garantir la stabilité des suspensions raides (~1.4 Hz) même si un tick serveur Minecraft dure 50 ms (20 Hz).
- **Corps rigide 3D** : position `Vec3d`, vitesse linéaire `Vec3d`, orientation `Quat4d`, vitesse angulaire `Vec3d`, masse & tenseur d'inertie 3x3 (pavé plein), intégration symplectique (semi-implicit Euler) avec gardes NaN/Inf.
- **Suspensions 4 roues** : 4 raycasts depuis les points d'ancrage en repère châssis vers le bas local ; loi de Hooke `F = k * x` + amortisseur asymétrique compression/détente `c_bump / c_rebound` ; force normale bornée `>= 0`.
- **Modèle de pneus (Strategy)** :
  - Phase 1 : `BrushTireModel` réaliste avec glissement longitudinal, angle de dérive latéral, cercle de friction combiné (`sqrt(Fx^2 + Fy^2) <= mu * Fz`) et amortissement visqueux basse vitesse pour éliminer toute instabilité numérique à l'arrêt.
  - Phase 2 : `PacejkaTireModel` (Magic Formula) interchangeable par injection de dépendance.
- **Chaîne cinématique** : moteur à courbe de couple RPM, boîte de vitesses (R, N, 1..6), frein principal, frein à main (roues arrière), puis différentiels (Open/LSD/Locked) et ABS en Phase 2.
- **Collisions monde** : `CollisionWorld` abstrait implémenté par `MinecraftCollisionWorld` (interrogation exacte des `VoxelShape` Minecraft + cartographie des blocs vers `SurfaceProperties`). Résolution impulsionnelle multi-points du châssis avec restitution, frottement et émission `VehicleCollisionEvent`.

### 2.2 Stratégie Vulkan (Phase 3)
- **Références étudiées** : VulkanMod (LGPL-3.0, architecture d'interception GLFW/RenderSystem/chunk meshing), LWJGL Vulkan bindings (BSD-3-Clause), Vulkan Memory Allocator (MIT), Shaderc/SPIR-V (Apache-2.0).
- **Principe d'indépendance** : aucune réutilisation non conforme de code tiers et aucune dépendance de mod externe. Notre backend (`com.realisticvehicles.vulkan.*`) implémente `RenderBackend`, embarque ses bindings LWJGL Vulkan + VMA + shaders SPIR-V précompilés dans le JAR unique, et gère explicitement la création de fenêtre GLFW sans contexte OpenGL (`GLFW_NO_API`), la swapchain, la synchronisation par frame-in-flight et le rendu complet (monde + véhicules PBR/GLB + UI).
- **Phase 1–2** : un backend provisoire (`ProvisionalVehicleRenderer`) respecte le contrat `RenderBackend`/`VehicleRenderState` afin de valider immédiatement la conduite physique avant d'attaquer la migration complète du renderer Minecraft vers Vulkan.

### 2.3 Réseau & Contrôles (Phases 1 & 10)
- **Simulation autoritaire serveur** dès la Phase 1 : le client capture les entrées (`VehicleKeyMappings` avec contexte de conflit dédié véhicule pour ne pas perturber la marche à pied), les envoie via `VehicleInputPayload` (`CustomPacketPayload` NeoForge 1.21.1), et le serveur diffuse l'état physique et les roues via `VehicleStateSyncPayload`.
- Cela garantit une compatibilité immédiate solo (serveur intégré) et prépare directement le multijoueur (Phase 10).

### 2.4 Assets & Licences (Phase 4+)
- Politique stricte de zéro asset non licencié : l'archive `mercedes-benz-slk-55-amg-2012.zip` présente dans le dossier local contient un modèle issu de Forza Motorsport 7 (`FM7`) sans licence de redistribution ; elle est exclue du dépôt et du JAR.
- En Phase 4, nous produirons/intégrerons un modèle GLB original ou vérifié CC0/CC-BY avec attribution complète dans `ASSET_LICENSES.md`.
