# REALISTIC VEHICLES — PROGRESS LOG

## 2026-11-21 — Session 1 (Phase 0 complète + Phase 1 implémentée et validée en tests automatisés + Minecraft GameTests)

### Phase 0 (Validée à 100 %)
- Environnement vérifié : JDK 21.0.7 (Corretto), Gradle 8.11.1 wrapper, Python 3.12.6, Git 2.51.2, GPU Vulkan 1.4.341.
- Dépôt Git initialisé avec `.gitignore` excluant l'archive FM7 non licenciée (`mercedes-benz-slk-55-amg-2012.zip`).
- Configuration NeoForge `21.1.267` pour Minecraft `1.21.1` strictement via ModDevGradle `2.0.148`.
- Scripts qualité créés et intégrés à `./gradlew.bat check` : `tools/check-file-length.py` (200 lignes max) et `tools/check-architecture.py` (isolation DIP).
- 10 documents de suivi et d'architecture créés (`TASKS.md`, `IMPLEMENTATION_PLAN.md`, `ARCHITECTURE.md`, `DECISIONS.md`, `PROGRESS.md`, `TEST_RESULTS.md`, `BENCHMARKS.md`, `KNOWN_ISSUES.md`, `ASSET_LICENSES.md`, `README.md`).

### Phase 1A & 1B — Moteur physique automobile pur Java 21 (Validé à 100 %)
- **Core & Contrats** : `Vec3d`, `Quat4d`, `Mat3d`, `MathUtil`, `EventBus`, `SimpleEventBus`, `VehicleCollisionEvent`, `CollisionWorld`, `RayHit`, `CollisionBox`, `SurfaceProperties`, `RenderBackend`, `BackendType`, `VehicleRenderState`, `WheelRenderState`.
- **Physique 120 Hz** : `RigidBodyConfig` (Builder), `RigidBodyState`, `RigidBodyIntegrator`, `WheelConfig` (Builder), `WheelState`, `SuspensionSolver` (multi-rayons + amortissement asymétrique), `SteeringConfig`, `AckermannSteeringSolver`, `TireModel` (Strategy), `BrushTireModel` (cercle de friction + stabilisation basse vitesse), `EngineConfig`, `VehicleEngine`, `TransmissionConfig`, `VehicleTransmission`, `DrivetrainSolver`, `ChassisCollider`, `CollisionResolver` (anti-snag sur jointures de blocs + impulsions), `VehicleConfig`, `VehiclePresetFactory` (Sedan FWD / Sport RWD), `VehiclePhysicsStep`, `VehicleSimulation` (Facade).
- **Tests JUnit 5** : **16/16 PASSED** (`MathAndEventTest`, `VehicleSubsystemsTest`, `VehicleSimulationIntegrationTest`, `PhysicsBenchmarkTest` : 20 véhicules en `0.5265 ms/tick`).

### Phase 1C — Intégration Minecraft 1.21.1 / NeoForge (Compilé 0 warning, GameTests 4/4 PASSED)
- **Serveur / Monde** : `BlockSurfaceMapper`, `MinecraftCollisionWorld`, `VehicleStateRepository` (NBT), `VehicleClientPose` (triple buffering), `VehicleEntity`, `ModEntities`, `ModCommands` (`/rv spawn [sedan|sport]`, `/rv info`, `/rv remove`), `VehicleInputPayload`, `VehicleStateSyncPayload`, `ModNetwork`.
- **Client** : `ClientPacketHandlers`, `VehicleKeyConflictContext`, `VehicleKeyMappings` (`Z/S/Q/D/Espace` reconfigurables), `VehicleInputHandler`, `ProvisionalMeshBuilder`, `ProvisionalRenderBackend`, `ProvisionalVehicleRenderer`, `ClientModEvents`, texture `white.png`.
- **GameTests Minecraft 1.21.1 (`runGameTestServer`)** : **4/4 PASSED en 1.018 s** (`vehicleSpawnsAndSettlesOnBlocks`, `vehicleDrivesAndSteersOnMinecraftBlocks`, `vehicleStopsAtMinecraftStoneWall`, `vehiclePersistsStateToNbt`).
- **Qualité** : 68 fichiers sources <= 200 lignes (`check-file-length.py` OK), 61 fichiers Java conformes DIP (`check-architecture.py` OK), JAR produit dans `build/libs/RealisticVehicles-1.21.1.jar`.

### Prochain objectif immédiat
1. Validation visuelle & interactive côté client (`./gradlew.bat runClient` : `/rv spawn`, conduite `Z/S/Q/D/Espace`, vérification visuelle des 4 roues et feux, puis clôture de `P1-014`, `P1-016`, `P1-017`, `P1-018`).
2. Démarrer la **Phase 2 (Physique avancée)** : `PacejkaTireModel`, barres anti-roulis & transfert de charge, différentiels Open/LSD/AWD, ABS/TCS, sauts/retournements, collisions véhicule-véhicule.

---

## 2026-11-22 — Session 2 (Phase 2 complète + Noyau Vulkan + Chargeur GLB + Mercedes-Benz SLK 55 AMG 2012)

### Réalisations
- **Phase 1 validée en jeu** par l'utilisateur (`P1-014`, `P1-016`, `P1-017`, `P1-018` cochées).
- **Phase 2 (Physique avancée — 100 % testée)** :
  - `PacejkaTireModel` (Magic Formula) actif par défaut.
  - `AntiRollBarSolver` (transfert de charge latéral et roulis 3D).
  - `DifferentialStrategy` (`OpenDifferential`, `LimitedSlipDifferential` LSD).
  - `AbsTcsController` (ABS + antipatinage TCS).
  - `VehicleToVehicleCollider` (collisions 3D entre véhicules avec conservation de la quantité de mouvement, intégré dans `VehicleEntitySyncHelper`).
  - Nouveaux presets calibrés dans `VehiclePresetFactory` : `slk55` (Mercedes-Benz SLK 55 AMG 2012 : 1 610 kg, V8 540 Nm, propulsion LSD) et `offroad`.
- **Phase 3 (Noyau Vulkan & Chargeur GLB)** :
  - `lwjgl-vulkan:3.3.3` embarqué dans le JAR unique via `jarJar`.
  - `VulkanContext`, `VulkanMemoryAllocator`, `VulkanBuffer`, `VulkanFrameSync`, `VulkanRenderBackend` vérifiés sur le GPU hôte avec 0 fuite VRAM.
  - `GlbLoader`, `GlbAccessorReader`, `GlbModelAssembler`, `GlbModel`, `GlbMaterial`, `GlbWheelPart` : parsing complet glTF 2.0 binaire et séparation automatique des 4 roues.
- **Phase 4 (Mercedes-Benz SLK 55 AMG 2012)** :
  - `GlbModelRegistry` + `GlbVehicleRenderer` chargent en tâche de fond `mercedes-benz-slk-55-amg-2012.zip`, uploadent les buffers sur Vulkan et affichent la vraie Mercedes SLK 55 AMG avec 4 roues articulées dans Minecraft 1.21.1 (`/rv spawn` par défaut).
- **Phase 5 (Caméras & HUD — Compilé et câblé)** :
  - `CameraMode` + `VehicleCameraController` : 5 modes de caméra (3e personne, cockpit, capot, pare-chocs arrière, caméra de recul) avec bascule par `C` (reconfigurable), transitions amorties et raycast anti-clipping.
  - `VehicleHudOverlay` + extension `VehicleStateSyncPayload` : vitesse km/h, barre RPM avec zone rouge, rapport engagé (`R`/`N`/`1..6`), voyant frein/recul et mode caméra actif.
- **Phases 6–9 (Noyaux métier & Tests unitaires — 31/31 PASSED)** :
  - `VehicleAnimationStateMachine` (entrée/sortie, portes, capot, coffre, essuie-glaces, vitres, volant).
  - `VehicleDamageService` + `ImpactAnalyzer` + `VehicleDamageState` (dégâts par sous-système, porte bloquée après choc latéral, déformation hybride des vertices, réparation).
  - `VehicleFuelSystem`, `VehicleKeyManager`, `VehicleMaintenanceState`, `VehicleAccessoriesState`, `DynamicEngineAudioController`.
- **Tests** : **31/31 JUnit 5 PASSED**, **110/110 fichiers <= 200 lignes**, **98/98 fichiers Java conformes DIP**.

### Prochain objectif immédiat
- Câbler `VehicleDamageService`, `VehicleFuelSystem`, `VehicleKeyManager` et `DynamicEngineAudioController` dans `VehicleEntity` / client HUD / OpenAL, puis finaliser le pipeline Vulkan complet (`P3-004`/`P3-007`/`P3-008`) et la réconciliation multijoueur (Phase 10).
