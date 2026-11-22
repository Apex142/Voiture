# REALISTIC VEHICLES (Minecraft 1.21.1 / NeoForge) — EXHAUSTIVE TASKS

> **Source de vérité du projet.**
> Règle absolue : aucune tâche n'est cochée tant que son critère d'acceptation n'a pas été exécuté et vérifié.
> Limite stricte : 200 lignes maximum par fichier source (`python tools/check-file-length.py`).

---

## Légende des champs

Chaque tâche spécifie : `ID` | `Priorité` | `Dépendances` | `Description` -> `Résultat attendu` | `Vérification`.

---

## PHASE 0 — INITIALISATION & OUTILLAGE (Priorité 0)

- [x] **P0-001** `[P0]` `(Deps: -)` Vérifier l'installation et la compatibilité de Java 21
  - *Attendu* : JDK 21 fonctionnel détecté et utilisé par Gradle.
  - *Vérification* : `java -version` sur le JDK 21 cible retourne `21.0.7` (Amazon Corretto).
- [x] **P0-002** `[P0]` `(Deps: P0-001)` Initialiser le dépôt Git et `.gitignore`
  - *Attendu* : dépôt Git initialisé, archives non licenciées et dossiers `build/` ignorés.
  - *Vérification* : `git status` propre.
- [x] **P0-003** `[P0]` `(Deps: P0-001)` Initialiser Gradle Wrapper (8.11.1) et `settings.gradle`
  - *Attendu* : `gradlew.bat`, `gradlew`, `gradle/wrapper/*`, `settings.gradle` opérationnels.
  - *Vérification* : `./gradlew.bat --version` retourne Gradle 8.11.1 + JVM 21.0.7.
- [x] **P0-004** `[P0]` `(Deps: P0-003)` Configurer le projet NeoForge 1.21.1 (`build.gradle`, `gradle.properties`, ModDevGradle 2.0.148, JUnit 5)
  - *Attendu* : projet ciblant strictement Minecraft 1.21.1, NeoForge 21.1.267, Java 21, produisant `build/libs/RealisticVehicles-1.21.1.jar`.
  - *Vérification* : `./gradlew.bat check build` réussit sans erreur.
- [x] **P0-005** `[P0]` `(Deps: P0-004)` Créer les métadonnées du mod (`META-INF/neoforge.mods.toml`, `pack.mcmeta`, `en_us.json`, `fr_fr.json`)
  - *Attendu* : identifiant `realisticvehicles`, dépendance stricte `[1.21.1]` et NeoForge `[21.1,)`, aucun autre mod requis.
  - *Vérification* : validé au chargement NeoForge (`Realistic Vehicles 0.1.0 (realisticvehicles)`).
- [x] **P0-006** `[P0]` `(Deps: P0-005)` Créer la classe principale `RealisticVehiclesMod` et la configuration des logs
  - *Attendu* : entrée `@Mod("realisticvehicles")` propre, logger SLF4J, enregistrement des bus NeoForge.
  - *Vérification* : compilation réussie et initialisation visible dans `run/logs/latest.log`.
- [x] **P0-007** `[P0]` `(Deps: P0-002)` Créer le script de vérification des 200 lignes (`tools/check-file-length.py`) et l'intégrer au build Gradle
  - *Attendu* : inspecte `.java`, `.kt`, `.py`, `.groovy`, échoue avec code non nul si `> 200` lignes.
  - *Vérification* : `python tools/check-file-length.py` et `./gradlew.bat check` retournent 0.
- [x] **P0-008** `[P0]` `(Deps: P0-002)` Créer le vérificateur d'architecture SOLID/DIP (`tools/check-architecture.py`) et l'intégrer au build
  - *Attendu* : garantit l'absence d'import `net.minecraft.*` / `net.neoforged.*` dans `core` et `physics`.
  - *Vérification* : `python tools/check-architecture.py` et `./gradlew.bat check` retournent 0.
- [x] **P0-009** `[P0]` `(Deps: P0-002)` Créer l'ensemble des documents de référence Markdown (`TASKS.md`, `IMPLEMENTATION_PLAN.md`, `ARCHITECTURE.md`, `DECISIONS.md`, `PROGRESS.md`, `TEST_RESULTS.md`, `BENCHMARKS.md`, `KNOWN_ISSUES.md`, `ASSET_LICENSES.md`, `README.md`)
  - *Attendu* : tous les documents créés, cohérents, licences auditées.
  - *Vérification* : les 10 fichiers Markdown sont présents et complets.
- [x] **P0-010** `[P0]` `(Deps: P0-006, P0-007, P0-008)` Compiler le premier JAR `build/libs/RealisticVehicles-1.21.1.jar` et exécuter la suite de tests initiale
  - *Attendu* : `./gradlew.bat check build` passe à 100 % et génère le JAR.
  - *Vérification* : `build/libs/RealisticVehicles-1.21.1.jar` généré (3 350 octets).
- [x] **P0-011** `[P0]` `(Deps: P0-010)` Vérifier le démarrage de Minecraft 1.21.1 / NeoForge avec le mod chargé et inspecter les logs
  - *Attendu* : NeoForge charge `realisticvehicles` sans exception critique dans les logs.
  - *Vérification* : `runGameTestServer` a chargé `Realistic Vehicles 0.1.0` et exécuté `FMLCommonSetupEvent` sans erreur dans `run/logs/latest.log`.

---

## PHASE 1 — PREMIÈRE VOITURE JOUABLE & PHYSIQUE FONDAMENTALE (Priorité 1)

### 1A — Noyau mathématique, événements et abstractions (Découplés de Minecraft)
- [x] **P1-001** `[P1]` `(Deps: P0-010)` Implémenter le noyau mathématique 3D (`Vec3d`, `Quat4d`, `Mat3d`, `MathUtil`)
  - *Attendu* : vecteurs immuables, quaternions unitaires, tenseur d'inertie 3x3, gardes NaN/Inf.
  - *Vérification* : `MathAndEventTest` (6/6 PASSED).
- [x] **P1-002** `[P1]` `(Deps: P1-001)` Implémenter le bus d'événements découplé (`EventBus`, `SimpleEventBus`, `VehicleCollisionEvent`)
  - *Attendu* : pattern Observer sans singleton global, découplage des effets de collision.
  - *Vérification* : `MathAndEventTest` + `VehicleSimulationIntegrationTest` (PASSED).
- [x] **P1-003** `[P1]` `(Deps: P1-001)` Définir les contrats d'abstraction monde physique (`CollisionWorld`, `RayHit`, `CollisionBox`, `SurfaceProperties`)
  - *Attendu* : aucune dépendance Minecraft/NeoForge (DIP strict).
  - *Vérification* : `tools/check-architecture.py` (0 violation) + compilation OK.
- [x] **P1-004** `[P1]` `(Deps: P1-001)` Définir les interfaces d'abstraction rendu (`RenderBackend`, `BackendType`, `VehicleRenderState`, `WheelRenderState`)
  - *Attendu* : contrat prêt pour le basculement futur vers le renderer Vulkan (Phase 3).
  - *Vérification* : compilation + `tools/check-architecture.py` OK.

### 1B — Moteur physique automobile de base (Corps rigide, 4 roues, suspensions, pneus, collisions)
- [x] **P1-005** `[P1]` `(Deps: P1-001)` Implémenter le corps rigide (`RigidBodyConfig` avec Builder, `RigidBodyState`, `RigidBodyIntegrator`)
  - *Attendu* : masse, gravité, vitesse linéaire/angulaire, orientation 3D, forces/couples, pas d'intégration symplectique, protection NaN/Inf.
  - *Vérification* : `VehicleSubsystemsTest` + `VehicleSimulationIntegrationTest` (PASSED).
- [x] **P1-006** `[P1]` `(Deps: P1-003, P1-005)` Implémenter les 4 roues et les suspensions (`WheelConfig` Builder, `WheelState`, `SuspensionSolver`)
  - *Attendu* : raycasts de contact au sol par roue, ressort-amortisseur (Hooke + compression/détente asymétrique), force normale `>= 0`.
  - *Vérification* : `VehicleSimulationIntegrationTest#vehicleSettlesAndRemainsStableAtRest` (4 roues en contact, équilibre stable sans rebond).
- [x] **P1-007** `[P1]` `(Deps: P1-006)` Implémenter le système de direction (`SteeringConfig`, `AckermannSteeringSolver`)
  - *Attendu* : géométrie Ackermann des roues avant, limitation de vitesse de braquage dépendante de la vitesse.
  - *Vérification* : `VehicleSubsystemsTest#ackermannInnerWheelSteersMoreThanOuterWheel` + test virage gauche/droite (PASSED).
- [x] **P1-008** `[P1]` `(Deps: P1-005, P1-006)` Implémenter le modèle de pneus réaliste (`TireModel` Strategy, `TireContext`, `TireForces`, `BrushTireModel`)
  - *Attendu* : calcul traction longitudinale, force latérale, cercle de friction combiné, stabilisation basse vitesse sans tremblement à l'arrêt.
  - *Vérification* : `VehicleSubsystemsTest#brushTireModelRespectsSurfaceGripAndFrictionCircle` + test asphalte vs glace (PASSED).
- [x] **P1-009** `[P1]` `(Deps: P1-005, P1-006)` Implémenter la chaîne cinématique initiale (`EngineConfig`, `VehicleEngine`, `TransmissionConfig`, `VehicleTransmission`, `DrivetrainSolver`)
  - *Attendu* : courbe de couple moteur, RPM, boîte de vitesses (Marche arrière + rapports avant), frein principal et frein à main.
  - *Vérification* : `VehicleSubsystemsTest#engineAndTransmissionShiftAndEngageReverse` + accélération/freinage/recul (PASSED).
- [x] **P1-010** `[P1]` `(Deps: P1-002, P1-003, P1-005)` Implémenter la détection et résolution des collisions châssis (`ChassisCollider`, `CollisionResolver`)
  - *Attendu* : collisions châssis contre blocs solides, projection anti-pénétration, impulsion rebond+frottement, émission `VehicleCollisionEvent`.
  - *Vérification* : `VehicleSimulationIntegrationTest#vehicleClimbsSlopeAndStopsAgainstSolidWall` (PASSED).
- [x] **P1-011** `[P1]` `(Deps: P1-005..P1-010)` Implémenter l'orchestrateur de simulation à pas fixe (`VehicleInput`, `VehicleConfig` Builder, `VehiclePresetFactory`, `VehiclePhysicsStep`, `VehicleSimulation` Facade)
  - *Attendu* : pas fixe `1/120 s` avec accumulateur, presets Sedan/Sport en unités SI, mise en veille/stabilité à l'arrêt.
  - *Vérification* : `VehicleSimulationIntegrationTest` (5/5 PASSED, 0 NaN/Inf).

### 1C — Intégration Minecraft 1.21.1 / NeoForge (Entité, Contrôles, Réseau, Rendu provisoire)
- [x] **P1-012** `[P1]` `(Deps: P1-003)` Implémenter l'adaptateur monde Minecraft (`MinecraftCollisionWorld`, `BlockSurfaceMapper`)
  - *Attendu* : raycasts et requêtes AABB exactes sur les `VoxelShape` Minecraft + friction réaliste par type de bloc (béton/pierre/herbe/terre/sable/glace/neige).
  - *Vérification* : 4/4 NeoForge GameTests (`VehicleGameTests`) PASSED sur blocs `black_concrete` et mur `stone`.
- [x] **P1-013** `[P1]` `(Deps: P1-011, P1-012)` Créer et enregistrer `VehicleEntity` (`ModEntities`, `VehicleEntity`, `VehicleStateRepository` NBT)
  - *Attendu* : entité NeoForge pilotée exclusivement par `VehicleSimulation`, montage par clic droit, descente par Shift, positionnement conducteur, persistance NBT.
  - *Vérification* : 4/4 NeoForge GameTests (`VehicleGameTests`) PASSED (spawn, conduite, mur, persistance NBT).
- [x] **P1-014** `[P1]` `(Deps: P1-013)` Créer les commandes `/rv` (`ModCommands` : `/rv spawn [sedan|sport]`, `/rv remove`, `/rv info`)
  - *Attendu* : apparition instantanée d'un véhicule devant le joueur et inspection de télémétrie en temps réel.
  - *Vérification* : validé en jeu dans Minecraft 1.21.1.
- [x] **P1-015** `[P1]` `(Deps: P1-013)` Implémenter le réseau NeoForge 1.21.1 (`VehicleInputPayload`, `VehicleStateSyncPayload`, `ModNetwork`, `VehicleClientPose`)
  - *Attendu* : envoi des entrées conducteur client->serveur et synchronisation fluide pose/vitesse/roues serveur->clients.
  - *Vérification* : enregistrement payloads vérifié au démarrage NeoForge 1.21.1 (`runGameTestServer` + compilation 0 warning).
- [x] **P1-016** `[P1]` `(Deps: P1-015)` Implémenter les contrôles reconfigurables (`VehicleKeyConflictContext`, `VehicleKeyMappings`, `VehicleInputHandler`)
  - *Attendu* : Z (accélérer), S (freiner / marche arrière), Q (gauche), D (droite), Espace (frein à main) actifs en conduite sans casser la marche à pied, tous reconfigurables dans les options Minecraft.
  - *Vérification* : validé en conduite en jeu Minecraft 1.21.1.
- [x] **P1-017** `[P1]` `(Deps: P1-004, P1-013)` Implémenter le rendu 3D provisoire articulé (`ProvisionalVehicleRenderer`, `ProvisionalRenderBackend`, `ProvisionalMeshBuilder`, `ClientModEvents`)
  - *Attendu* : carrosserie lisible (châssis, cabine, phares, feux arrière, pare-chocs) + 4 roues qui tournent, braquent et suivent la compression réelle des suspensions.
  - *Vérification* : validé visuellement en jeu Minecraft 1.21.1.
- [x] **P1-018** `[P1]` `(Deps: P1-014..P1-017)` Validation complète jouable de la Phase 1 (Tests automatisés + test visuel/interactif en jeu Minecraft 1.21.1)
  - *Attendu* : voiture apparaît, roule, accélère, freine, recule, tourne, gravit une pente, s'arrête face à un mur, reste stable au repos, `check-file-length.py` et `check-architecture.py` OK, JAR compilé.
  - *Vérification* : tests automatisés JUnit (16/16) + GameTests (4/4) + validation en jeu par l'utilisateur OK.

---

## PHASE 2 — PHYSIQUE AUTOMOBILE AVANCÉE (Priorité 2)

- [x] **P2-001** `[P2]` `(Deps: P1-018)` Implémenter le modèle de pneus `PacejkaTireModel` (Magic Formula) et permettre la sélection Strategy par configuration
  - *Attendu* : courbes de glissement longitudinal/latéral et pic d'adhérence réalistes.
  - *Vérification* : `AdvancedPhysicsTest#pacejkaTireProducesRealisticPeakAndLateralGrip` (PASSED) + actif par défaut dans `VehicleSimulation`.
- [x] **P2-002** `[P2]` `(Deps: P2-001)` Dynamique 3D complète : roulis, tangage, barres anti-roulis (`AntiRollBarSolver`), transfert de charge dynamique
  - *Attendu* : plongée au freinage, cabrage à l'accélération, prise de roulis en virage modifiant la charge par roue.
  - *Vérification* : `AdvancedPhysicsTest#hardCorneringTransfersLoadAndInducesRoll` (PASSED).
- [x] **P2-003** `[P2]` `(Deps: P2-002)` Différentiels réalistes (ouvert, autobloquant LSD, AWD/RWD/FWD via `DifferentialStrategy`)
  - *Attendu* : répartition réelle du couple entre roues motrices selon la différence de vitesse.
  - *Vérification* : `AdvancedPhysicsTest#lsdTransfersTorqueAwayFromSpinningWheelUnlikeOpenDiff` (PASSED).
- [x] **P2-004** `[P2]` `(Deps: P2-001)` Système ABS, antipatinage (TCS) et frein à main réaliste (`AbsTcsController`)
  - *Attendu* : modulation de pression de freinage anti-blocage, déclenchement de survirage au frein à main.
  - *Vérification* : `AdvancedPhysicsTest#absReducesBrakeTorqueWhenWheelLocksUp` (PASSED).
- [x] **P2-005** `[P2]` `(Deps: P2-002)` Gestion avancée des pentes raides, sauts (détachement des 4 roues, réception amortie) et retournement (collision toit/arceau)
  - *Attendu* : stabilité lors des sauts, détection véhicule retourné sans blocage dans le sol.
  - *Vérification* : `ChassisCollider` (points toit inclus) + `SuspensionSolver` (roue libre en vol) + tests pente 10° (PASSED).
- [x] **P2-006** `[P2]` `(Deps: P2-002)` Collisions véhicule-véhicule (`VehicleToVehicleCollider` avec conservation de la quantité de mouvement)
  - *Attendu* : deux véhicules se percutent, transfèrent leur impulsion et pivotent selon le point d'impact.
  - *Vérification* : `AdvancedPhysicsTest#vehicleToVehicleCollisionConservesMomentum` (PASSED) + intégré dans `VehicleEntity`.
- [x] **P2-007** `[P2]` `(Deps: P2-001..P2-006)` Validation et calibration des profils véhicules (`sedan`, `sport`, `slk55`, `offroad`) sur multi-surfaces
  - *Attendu* : masse, puissance, suspensions et adhérence changent nettement le comportement routier.
  - *Vérification* : `VehiclePresetFactory` + `AdvancedPhysicsTest` + 21/21 JUnit 5 PASSED.

---

## PHASE 3 — VULKAN ET RENDU 3D NATIF INTÉGRÉ (Priorité 3)

- [ ] **P3-001** `[P3]` `(Deps: P1-018)` Étudier l'architecture Vulkan (VulkanMod / VulkanMod Reforged / Volcanic / LWJGL Vulkan) et documenter l'architecture cible et les licences dans `DECISIONS.md` et `ASSET_LICENSES.md`
  - *Attendu* : stratégie d'intégration complète sans mod externe, licences compatibles vérifiées (LWJGL BSD-3, VMA MIT, Shaderc Apache-2.0).
  - *Vérification* : document d'architecture Vulkan validé.
- [x] **P3-002** `[P3]` `(Deps: P3-001)` Embarquer les dépendances LWJGL Vulkan dans le JAR unique (`jarJar` `lwjgl-vulkan:3.3.3`)
  - *Attendu* : bindings embarqués dans `RealisticVehicles-1.21.1.jar` sans téléchargement au lancement.
  - *Vérification* : `jarJar` + `VulkanAndGlbTest` PASSED.
- [x] **P3-003** `[P3]` `(Deps: P3-002)` Implémenter l'initialisation Vulkan (`VulkanContext` : `VkInstance`, `VkPhysicalDevice`, `VkDevice`, `VkQueue`)
  - *Attendu* : instance Vulkan, sélection GPU, queue graphique créées proprement.
  - *Vérification* : `VulkanAndGlbTest#vulkanBackendInitializesAllocatesAndFreesWithoutLeak` PASSED sur GPU hôte.
- [ ] **P3-004** `[P3]` `(Deps: P3-003)` Implémenter la surface GLFW/Vulkan, la `VulkanSwapchain`, les render passes, framebuffers et gestion du redimensionnement
  - *Attendu* : swapchain créée sans contexte OpenGL concurrent, recréation propre au redimensionnement de fenêtre.
  - *Vérification* : test ouverture fenêtre + redimensionnement sans erreur validation layer.
- [x] **P3-005** `[P3]` `(Deps: P3-003)` Implémenter la synchronisation GPU (`VulkanFrameSync` : sémaphores, fences, `VkCommandPool`, `VkCommandBuffer`)
  - *Attendu* : primitives de synchronisation et pools de commandes initialisés et détruits sans fuite.
  - *Vérification* : `VulkanAndGlbTest` PASSED.
- [x] **P3-006** `[P3]` `(Deps: P3-005)` Implémenter la gestion mémoire GPU (`VulkanMemoryAllocator`, `VulkanBuffer` via `VkDeviceMemory` avec suivi de fuites)
  - *Attendu* : allocation/libération traquée, zéro fuite mémoire VRAM à la fermeture.
  - *Vérification* : `VulkanAndGlbTest` vérifie `liveAllocationCount() == 0` après `dispose()`.
- [ ] **P3-007** `[P3]` `(Deps: P3-006)` Implémenter les shaders SPIR-V et pipelines graphiques (`VulkanShaderModule`, `VulkanGraphicsPipeline` PBR véhicules + pipeline monde/UI)
  - *Attendu* : shaders compilés en SPIR-V embarqués, pipelines créés avec depth testing, blending et culling.
  - *Vérification* : validation SPIR-V et création pipeline sans erreur.
- [ ] **P3-008** `[P3]` `(Deps: P3-007)` Intégrer le backend Vulkan au pipeline de rendu complet Minecraft 1.21.1 / NeoForge (interception contexte GLFW, rendu chunks, entités, GUI)
  - *Attendu* : Minecraft démarre et rend monde + chunks + UI + véhicules sur le backend Vulkan interne sans mod tiers.
  - *Vérification* : jeu démarré, diagnostic confirmant le swapchain Vulkan actif.
- [x] **P3-009** `[P3]` `(Deps: P3-003)` Implémenter le chargeur GLB/glTF 2.0 complet (`GlbLoader`, `GlbAccessorReader`, `GlbModelAssembler`, matériaux PBR, hiérarchie de nœuds)
  - *Attendu* : parsing binaire GLB -> buffers GPU Vulkan, matériaux PBR, séparation châssis + 4 roues articulées.
  - *Vérification* : `VulkanAndGlbTest#glbLoaderParsesMercedesSlk55AndUploadsToVulkan` PASSED sur le GLB 47,2 Mo.
- [ ] **P3-010** `[P3]` `(Deps: P3-008, P3-009)` Implémenter LOD, frustum culling et skinning/morph targets GPU dans le renderer véhicules Vulkan
  - *Attendu* : sélection automatique des niveaux de détail selon distance et frustum culling par AABB.
  - *Vérification* : tests unitaires culling/LOD + mesure draw calls.

---

## PHASE 4 — VOITURE 3D RÉALISTE (Priorité 4)

- [x] **P4-001** `[P4]` `(Deps: P3-009)` Intégrer le modèle 3D GLB Mercedes-Benz SLK 55 AMG 2012 fourni par l'utilisateur (`GlbModelRegistry`) et documenter son statut dans `ASSET_LICENSES.md`
  - *Attendu* : modèle complet (carrosserie, châssis, 4 roues, intérieur, sièges, tableau de bord, volant, vitres, portes, phares, capot, coffre) chargé.
  - *Vérification* : `VulkanAndGlbTest` (> 50 000 triangles, 4 roues FL/FR/RL/RR séparées, 98 matériaux).
- [x] **P4-002** `[P4]` `(Deps: P4-001)` Séparer les composants articulés (`GlbModel`, `GlbWheelPart`, `GlbModelAssembler`)
  - *Attendu* : liaison propre entre châssis et les 4 moyeux de roues centrés sur leurs axes réels.
  - *Vérification* : `VulkanAndGlbTest` PASSED.
- [ ] **P4-003** `[P4]` `(Deps: P4-002)` Valider visuellement en jeu la Mercedes SLK 55 AMG 2012 synchronisée avec la physique (`GlbVehicleRenderer`, rotation/braquage/suspension des 4 roues)
  - *Attendu* : la voiture roule avec son modèle 3D réaliste parfaitement calé au sol et à la physique.
  - *Vérification* : test visuel en jeu (`./gradlew.bat runClient`, `/rv spawn`) + mesures dans `BENCHMARKS.md`.

---

## PHASE 5 — CAMÉRAS ET HUD (Priorité 5)

- [x] **P5-001** `[P5]` `(Deps: P4-003)` Implémenter l'architecture caméras (`CameraMode`, `VehicleCameraController`, touche `C` reconfigurable) : 3e personne, cockpit, capot, pare-chocs arrière, caméra de recul
  - *Attendu* : bascule de caméra par touche dédiée, transitions fluides amorties.
  - *Vérification* : compilé et câblé sur `ViewportEvent.ComputeCameraAngles` ; à tester en jeu (`runClient`).
- [x] **P5-002** `[P5]` `(Deps: P5-001)` Implémenter la détection de collision de caméra (raycast `level.clip` anti-clipping murs/plafonds)
  - *Attendu* : la caméra ne traverse jamais les blocs en 3e personne.
  - *Vérification* : compilé dans `VehicleCameraController#clipCameraAgainstBlocks`.
- [x] **P5-003** `[P5]` `(Deps: P4-003)` Implémenter le HUD véhicule connecté aux vraies données de simulation (`VehicleHudOverlay` : vitesse km/h, barre RPM, rapport, état frein/recul, mode caméra)
  - *Attendu* : affichage net, réactif, zéro valeur simulée factice.
  - *Vérification* : valeurs synchronisées par `VehicleStateSyncPayload`, compilé et enregistré sur `RegisterGuiLayersEvent`.

---

## PHASE 6 — ANIMATIONS DU JOUEUR ET DES OUVRANTS (Priorité 6)

- [ ] **P6-001** `[P6]` `(Deps: P5-001)` Machine à états d'animation (`VehicleAnimationStateMachine` : IDLE, APPROACHING, OPENING_DOOR, ENTERING, SEATED, EXITING)
  - *Attendu* : transitions propres, gestion des interruptions (dégâts, blocage, annulation).
  - *Vérification* : tests unitaires de toutes les transitions et interruptions.
- [ ] **P6-002** `[P6]` `(Deps: P6-001)` Séquence complète d'entrée (détection porte, placement joueur, main poignée, ouverture porte, déplacement/rotation corps, installation siège, fermeture porte, transition caméra)
  - *Attendu* : entrée fluide sans téléportation visible.
  - *Vérification* : test manuel en 1re et 3e personne.
- [ ] **P6-003** `[P6]` `(Deps: P6-002)` Rendu et animation du joueur au volant (skin Minecraft préservé, mains sur le volant, bras articulés, rotation volant, jambes/pédales, levier de vitesse)
  - *Attendu* : joueur visible à travers les vitres avec son vrai skin animé en synchronisation avec la direction/accélérateur.
  - *Vérification* : test visuel intérieur et extérieur.
- [ ] **P6-004** `[P6]` `(Deps: P6-002)` Séquence complète de sortie (ouverture porte, extraction personnage, placement sécurisé au sol, fermeture porte, retour caméra)
  - *Attendu* : sortie fluide même si porte obstruée (bascule siège passager / gestion obstacle).
  - *Vérification* : test en terrain libre et contre un mur.
- [ ] **P6-005** `[P6]` `(Deps: P6-001)` Animations mécaniques secondaires : capot, coffre, vitres électriques, essuie-glaces, suspensions visuelles
  - *Attendu* : chaque composant s'anime progressivement selon son état réel.
  - *Vérification* : déclenchement de chaque ouvrant/accessoire en jeu.

---

## PHASE 7 — DÉGÂTS MÉCANIQUES ET DÉFORMATION DE CARROSSERIE (Priorité 7)

- [ ] **P7-001** `[P7]` `(Deps: P2-006, P4-003)` Système de dégâts mécaniques par sous-système (`VehicleDamageService` : moteur, transmission, radiateur, direction, suspension, pneus, freins, réservoir, portes, vitres, éclairage)
  - *Attendu* : chaque dégât altère réellement la physique (perte puissance, surchauffe, tirage direction, pneu crevé, freinage réduit, porte bloquée).
  - *Vérification* : tests unitaires d'impact sur les performances physiques.
- [ ] **P7-002** `[P7]` `(Deps: P7-001)` Analyseur d'impacts (`ImpactAnalyzer` : point, direction, impulsion, zone structurelle touchée via `VehicleCollisionEvent`)
  - *Attendu* : classification précise de l'énergie absorbée par zone.
  - *Vérification* : tests unitaires impacts frontal, latéral, arrière, tonneau.
- [ ] **P7-003** `[P7]` `(Deps: P7-002, P3-010)` Modèle de déformation hybride inspiré node-beam (zones déformables, déformation procédurale + morph targets GPU, pièces détachables)
  - *Attendu* : la géométrie de carrosserie se déforme réellement au point d'impact sans calculer des milliers de ressorts inutiles.
  - *Vérification* : test collision mur -> changement géométrique persistant + performances mesurées.
- [ ] **P7-004** `[P7]` `(Deps: P7-003)` Persistance NBT des déformations/dégâts et système de réparation
  - *Attendu* : les dégâts et déformations survivent au redémarrage et peuvent être réparés.
  - *Vérification* : tests unitaires sérialisation/désérialisation + réparation complète/partielle.

---

## PHASE 8 — INTERACTIONS RÉALISTES, CARBURANT & ENTRETIEN (Priorité 8)

- [ ] **P8-001** `[P8]` `(Deps: P6-005)` Système de clés uniques (`VehicleKeyItem`, UUID véhicule, doubles de clés, verrouillage/déverrouillage, autorisation démarrage, alarme)
  - *Attendu* : impossible de démarrer ou déverrouiller sans la bonne clé ; alarme si effraction.
  - *Vérification* : tests unitaires logique clé/serrure + test en jeu.
- [ ] **P8-002** `[P8]` `(Deps: P7-001, P8-001)` Interactions portes, coffre, capot (ouverture/fermeture, verrouillage, portes bloquées après accident grave)
  - *Attendu* : interactions ciblées par hitbox de composant.
  - *Vérification* : test en jeu sain et après crash latéral.
- [ ] **P8-003** `[P8]` `(Deps: P4-003)` Système de carburant complet (réservoir, consommation réelle fonction charge/RPM, types Essence/Diesel/Électrique, bloc pompe/station-service, trappe, bouchon, pistolet manipulable, animation remplissage, prix, arrêt auto)
  - *Attendu* : panne sèche réaliste, ravitaillement interactif complet et persistant.
  - *Vérification* : tests unitaires consommation + scénario ravitaillement complet.
- [ ] **P8-004** `[P8]` `(Deps: P7-001)` Système d'entretien (niveau d'huile, température moteur/liquide, batterie/alternateur, usure pneus/freins, remplacement de pièces)
  - *Attendu* : dégradation progressive réaliste et remise en état par entretien.
  - *Vérification* : tests unitaires usure/température/batterie.
- [ ] **P8-005** `[P8]` `(Deps: P6-005)` Accessoires électriques et commandes : feux (croisement/route/stop/recul), clignotants, warnings, klaxon, essuie-glaces, vitres
  - *Attendu* : états persistants et visibles sur le véhicule et le HUD.
  - *Vérification* : test interactif de chaque accessoire.

---

## PHASE 9 — AUDIO DYNAMIQUE (Priorité 9)

- [ ] **P9-001** `[P9]` `(Deps: P8-005)` Synthèse / modulation audio moteur dynamique fonction du RPM et de la charge (démarrage, ralenti, accélération, décélération, passages de rapports)
  - *Attendu* : hauteur et timbre moteur continus corrélés à la simulation.
  - *Vérification* : test auditif et vérification des paramètres pitch/gain en jeu.
- [ ] **P9-002** `[P9]` `(Deps: P9-001)` Sons dynamiques pneus/freins/impacts/interactions (crissement dérapage selon slip, gravier/herbe, freinage, crash selon impulsion, portes, serrures, clignotants, klaxon, carburant)
  - *Attendu* : déclenchement par `EventBus`, licences audio vérifiées dans `ASSET_LICENSES.md`.
  - *Vérification* : test de chaque catégorie sonore + audit licences.

---

## PHASE 10 — MULTIJOUEUR AUTORITAIRE (Priorité 10)

- [ ] **P10-001** `[P10]` `(Deps: P7-004, P8-005)` Physique strictement autoritaire serveur + transmission compacte des entrées joueur
  - *Attendu* : serveur dédié valide les entrées et simule la vérité terrain.
  - *Vérification* : test client connecté à serveur dédié local (`runServer` + `runClient`).
- [ ] **P10-002** `[P10]` `(Deps: P10-001)` Prédiction côté conducteur, interpolation clients distants et réconciliation douce des écarts
  - *Attendu* : conduite réactive même avec latence simulée (50–150 ms) sans saccades.
  - *Vérification* : tests unitaires réconciliation + test réseau local.
- [ ] **P10-003** `[P10]` `(Deps: P10-002)` Synchronisation efficace des animations, interactions et paramètres compacts de déformation (jamais de transfert de vertices)
  - *Attendu* : bande passante maîtrisée, déformations et ouvrants synchronisés pour tous les joueurs.
  - *Vérification* : mesure taille des paquets réseau + test 2 clients.

---

## PHASE 11 — OPTIMISATION EXTRÊME & BENCHMARKS (Priorité 11)

- [ ] **P11-001** `[P11]` `(Deps: P3-010, P10-003)` Optimisations CPU : Object Pooling mesuré, réduction allocations GC, broadphase spatial, sommeil physique multi-LOD (contrôlé / proche / éloigné / immobile)
  - *Attendu* : réduction mesurée des allocations et du temps CPU simulation.
  - *Vérification* : profilage et benchmarks avant/après dans `BENCHMARKS.md`.
- [ ] **P11-002** `[P11]` `(Deps: P11-001)` Optimisations GPU Vulkan : instancing, buffers persistants réutilisables, compression/mipmaps textures, réduction draw calls, profils graphiques (Faible / Moyen / Élevé / Ultra)
  - *Attendu* : baisse mesurée du frame time GPU et de la VRAM.
  - *Vérification* : benchmarks comparatifs des 4 profils.
- [ ] **P11-003** `[P11]` `(Deps: P11-002)` Campagne de benchmarks complète (1, 5, 10, 20 véhicules, collisions multiples, terrain complexe, FPS moyen, 1% low, CPU/GPU frame time, RAM/VRAM)
  - *Attendu* : résultats réels mesurés sans altérer la cohérence physique.
  - *Vérification* : tableaux complets publiés dans `BENCHMARKS.md`.

---

## PHASE 12 — FINALISATION & LIVRAISON PRODUCTION (Priorité 12)

- [ ] **P12-001** `[P12]` `(Deps: P11-003)` Vérification complète SOLID, dépendances acycliques, `tools/check-file-length.py` et `tools/check-architecture.py`
  - *Attendu* : 0 fichier `> 200` lignes, 0 cycle de dépendance, 0 violation architecturale.
  - *Vérification* : scripts qualité + revue complète.
- [ ] **P12-002** `[P12]` `(Deps: P12-001)` Audit complet des licences (`ASSET_LICENSES.md`) et dépendances embarquées
  - *Attendu* : 100 % des bibliothèques/assets légalement redistribuables avec attributions.
  - *Vérification* : audit de chaque entrée du JAR final.
- [ ] **P12-003** `[P12]` `(Deps: P12-001)` Campagne finale de tests (unitaires, intégration, fuites mémoire, sauvegarde/rechargement, solo, multijoueur)
  - *Attendu* : 100 % tests au vert, zéro fuite RAM/VRAM détectée.
  - *Vérification* : rapport final `TEST_RESULTS.md`.
- [ ] **P12-004** `[P12]` `(Deps: P12-002, P12-003)` Packaging final autonome `build/libs/RealisticVehicles-1.21.1.jar` et documentation utilisateur finale (`README.md`)
  - *Attendu* : mod unique installable sur Minecraft 1.21.1 + NeoForge 21.1.x sans aucun autre mod.
  - *Vérification* : test d'installation propre sur profil Minecraft 1.21.1 vierge.
