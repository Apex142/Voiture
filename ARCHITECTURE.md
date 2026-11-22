# REALISTIC VEHICLES — ARCHITECTURE (SOLID & DESIGN PATTERNS)

## 1. Vue d'ensemble des couches

```text
+-------------------------------------------------------------------+
|  client / render.provisional / (Phase 3: vulkan)                  |
|  Entrées joueur, KeyMappings, Rendu 3D, Caméras, HUD              |
+---------------------------------+---------------------------------+
                                  | dépend des abstractions
                                  v
+-------------------------------------------------------------------+
|  minecraft (Adaptateur NeoForge 1.21.1)                           |
|  RealisticVehiclesMod, VehicleEntity, MinecraftCollisionWorld,    |
|  ModEntities, ModCommands, ModNetwork, VehicleStateRepository     |
+---------------------------------+---------------------------------+
                                  | dépend des abstractions
                                  v
+-------------------------------------------------------------------+
|  physics (Java 21 pur — ZERO import Minecraft/NeoForge/LWJGL)     |
|  simulation, body, suspension, tires, drivetrain, steering,       |
|  collision, world (CollisionWorld)                                |
+---------------------------------+---------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|  core & render.api (Java 21 pur)                                  |
|  math (Vec3d, Quat4d, Mat3d), event (EventBus), RenderBackend     |
+-------------------------------------------------------------------+
```

La règle d'isolation stricte est vérifiée automatiquement à chaque build par `tools/check-architecture.py`.

## 2. Application stricte des principes SOLID

| Principe | Application concrète dans le projet |
|---|---|
| **S — Single Responsibility** | Chaque classe porte une seule responsabilité : `RigidBodyIntegrator` intègre le mouvement, `SuspensionSolver` résout les 4 ressorts/amortisseurs, `BrushTireModel` calcule les forces pneumatiques, `VehicleEngine` calcule le couple moteur, `VehicleTransmission` gère les rapports, `CollisionResolver` résout les chocs, `AckermannSteeringSolver` calcule les angles de roues, `MinecraftCollisionWorld` adapte le monde Minecraft. |
| **O — Open/Closed** | Extension par interfaces sans modifier le cœur : `TireModel` (`BrushTireModel`, `PacejkaTireModel`), `RenderBackend` (`ProvisionalRenderBackend`, futur `VulkanRenderBackend`), `CollisionWorld` (`MinecraftCollisionWorld`, `FlatGroundWorld` en tests). |
| **L — Liskov Substitution** | Tout `TireModel` respecte le contrat de signe et de bornes finies sur `TireForces` ; tout `CollisionWorld` garantit des normales unitaires et des distances positives. |
| **I — Interface Segregation** | Interfaces courtes et ciblées : `TireModel` (1 méthode), `CollisionWorld` (2 méthodes), `EventBus` (2 méthodes), `RenderBackend` (cycle de vie minimal), `VehicleCommand` (1 méthode). |
| **D — Dependency Inversion** | `VehicleSimulation` et `VehiclePhysicsStep` dépendent uniquement des interfaces `TireModel`, `CollisionWorld`, `EventBus`, jamais de `Level`, `Entity` ou `PoseStack`. |

## 3. Justification des Design Patterns utilisés

| Pattern | Emplacement | Problème réel résolu |
|---|---|---|
| **Strategy** | `TireModel` (`BrushTireModel`, futur `PacejkaTireModel`), futur `DifferentialStrategy`, `RenderBackend` | Remplacer dynamiquement le modèle physique de pneu ou le backend de rendu sans modifier `VehiclePhysicsStep`. |
| **Factory** | `VehiclePresetFactory` | Instancier des configurations véhicules réalistes et cohérentes (Sedan, Sport, etc.) à partir d'un identifiant de preset. |
| **Builder** | `RigidBodyConfig.Builder`, `WheelConfig.Builder`, `VehicleConfig.Builder` | Construire de manière lisible et immuable des configurations automobiles à nombreux paramètres physiques avec validation des invariants. |
| **Facade** | `VehicleSimulation` | Exposer une API simple (`step`, `setInput`, `getTelemetry`, `getChassisState`) masquant l'orchestration interne à 120 Hz (corps rigide, suspensions, pneus, moteur, transmission, collisions). |
| **Adapter** | `MinecraftCollisionWorld`, `BlockSurfaceMapper` | Traduire les `VoxelShape` et `BlockState` Minecraft 1.21.1 vers le domaine physique pur (`CollisionWorld`, `SurfaceProperties`). |
| **Observer / Event Bus** | `EventBus`, `SimpleEventBus`, `VehicleCollisionEvent` | Découpler la détection d'impact physique des consommateurs futurs (dégâts mécaniques, déformation, audio, particules) sans singleton global. |
| **Command** | `VehicleInput` / `VehicleInputCommand` | Encapsuler de façon immuable et sérialisable les intentions de conduite (accélérateur, frein, direction, frein à main) entre client et serveur. |
| **Repository** | `VehicleStateRepository` | Isoler la sérialisation/désérialisation persistante NBT de `VehicleEntity` et de `VehicleSimulation`. |
| **Dependency Injection** | Constructeurs de `VehicleSimulation`, `VehiclePhysicsStep`, `VehicleEntity` | Assembler les dépendances explicitement (constructeur) pour rendre chaque composant 100 % testable isolément sans singletons globaux. |
| **State** *(Phases 5–6)* | `VehicleAnimationStateMachine`, `CameraModeState` | Gérer proprement les transitions et interruptions d'entrée/sortie et de caméras. |
| **Composite** *(Phase 4)* | `VehiclePartNode`, `VehicleModelHierarchy` | Représenter l'arbre des pièces d'un véhicule GLB (carrosserie, portes, roues, volant). |
| **Object Pool** *(Phase 11)* | Pools de vecteurs/contextes si mesurés chauds au profilage | Éviter l'allocation excessive uniquement après mesure dans `BENCHMARKS.md`. |

## 4. Règle des 200 lignes

Chaque fichier source (`.java`, `.kt`, `.py`, `.groovy`) est limité à **200 lignes strictement**.
Toute violation fait échouer `python tools/check-file-length.py` et la tâche Gradle `check`.
