# REALISTIC VEHICLES — TEST RESULTS

> Ce document enregistre uniquement des résultats de tests réellement exécutés. Aucun résultat n'est fabriqué.

## Historique des campagnes de tests

### 2026-11-21 — Validation Phase 0 (Build & Chargement NeoForge 1.21.1)
| Contrôle / Test | Commande | Résultat | Détails |
|---|---|---|---|
| Limite 200 lignes | `python tools/check-file-length.py` | **PASS** | 3 fichiers contrôlés, 0 violation |
| Isolation architecture | `python tools/check-architecture.py` | **PASS** | 1 fichier Java contrôlé, 0 violation |
| Build & JAR | `.\gradlew.bat check build` | **PASS** | `build/libs/RealisticVehicles-1.21.1.jar` généré (3 350 octets) |
| Démarrage NeoForge 1.21.1 | `.\gradlew.bat runGameTestServer` | **PASS** | `Realistic Vehicles 0.1.0 (realisticvehicles)` chargé + `FMLCommonSetupEvent` OK |

### 2026-11-21 — Validation Phase 1 (Physique 120 Hz + Intégration Minecraft 1.21.1)
| Contrôle / Suite | Commande | Résultat | Détails |
|---|---|---|---|
| Limite 200 lignes | `python tools/check-file-length.py` | **PASS** | 68 fichiers sources contrôlés, 0 violation |
| Isolation SOLID / DIP | `python tools/check-architecture.py` | **PASS** | 61 fichiers Java contrôlés, 0 import Minecraft dans `core`/`physics` |
| Compilation Java 21 | `.\gradlew.bat compileJava` | **PASS** | 0 erreur, 0 warning |
| `MathAndEventTest` | `.\gradlew.bat test` | **PASS (6/6)** | `Vec3d`, `Quat4d`, `Mat3d`, `MathUtil`, `SimpleEventBus` |
| `VehicleSubsystemsTest` | `.\gradlew.bat test` | **PASS (4/4)** | Masse `F=ma`, Ackermann `FL > FR`, Brush Tire asphalte/glace, Boîte + Marche arrière |
| `VehicleSimulationIntegrationTest` | `.\gradlew.bat test` | **PASS (5/5)** | Équilibre au repos, accélération/frein/frein à main/recul, virages G/D, masse & surfaces, pente 10° & mur solide |
| `PhysicsBenchmarkTest` | `.\gradlew.bat test` | **PASS (1/1)** | 1 à 20 véhicules sous 0.50 ms / tick 20 Hz |
| `VehicleGameTests` (Minecraft 1.21.1) | `.\gradlew.bat runGameTestServer` | **PASS (4/4)** | 4/4 GameTests réussis en 1.018 s sur blocs réels (`black_concrete`, mur `stone`, persistance NBT) |
| Packaging JAR unique | `.\gradlew.bat build` | **PASS** | `build/libs/RealisticVehicles-1.21.1.jar` |

### 2026-11-22 — Validation Phase 2 (Physique avancée) + Noyau Vulkan + Chargeur GLB Mercedes SLK 55 AMG
| Contrôle / Suite | Commande | Résultat | Détails |
|---|---|---|---|
| Limite 200 lignes | `python tools/check-file-length.py` | **PASS** | 92 fichiers sources contrôlés, 0 violation |
| Isolation SOLID / DIP | `python tools/check-architecture.py` | **PASS** | 83 fichiers Java contrôlés, 0 violation |
| `AdvancedPhysicsTest` | `.\gradlew.bat test` | **PASS (5/5)** | Pacejka, LSD vs Open, ABS, transfert de charge/roulis SLK 55, collision véhicule-véhicule |
| `VulkanAndGlbTest` | `.\gradlew.bat test` | **PASS (2/2)** | Initialisation GPU Vulkan réelle + `VkDeviceMemory` (0 fuite VRAM) + parsing complet du GLB Mercedes SLK 55 AMG (47,2 Mo, 4 roues séparées) |
| Suite JUnit complète | `.\gradlew.bat check build` | **PASS (23/23)** | 23/23 tests au vert, `RealisticVehicles-1.21.1.jar` généré |

### 2026-11-22 — Validation Phase 5 (Caméras/HUD) + Domaines Phases 6–9 (Animations, Dégâts, Interactions, Audio)
| Contrôle / Suite | Commande | Résultat | Détails |
|---|---|---|---|
| Limite 200 lignes | `python tools/check-file-length.py` | **PASS** | 110 fichiers sources contrôlés, 0 violation |
| Isolation SOLID / DIP | `python tools/check-architecture.py` | **PASS** | 98 fichiers Java contrôlés, 0 violation |
| `AnimationStateMachineTest` | `.\gradlew.bat test` | **PASS (3/3)** | Transitions entrée/sortie, volant, porte bloquée, interruption |
| `DamageSystemTest` | `.\gradlew.bat test` | **PASS (2/2)** | Impact frontal/latéral, perte puissance, déformation vertices, réparation |
| `InteractionsAndAudioTest` | `.\gradlew.bat test` | **PASS (3/3)** | Carburant/pompe, clés/alarme, surchauffe radiateur, pitch moteur/crissement |
| Suite JUnit complète | `.\gradlew.bat check build` | **PASS (31/31)** | 31/31 tests au vert, `RealisticVehicles-1.21.1.jar` généré |

## Procédure de validation manuelle client (P1-014, P1-016, P1-017, P1-018)
1. Lancer `.\gradlew.bat runClient` (avec `JAVA_HOME` pointant sur JDK 21) ou copier `build/libs/RealisticVehicles-1.21.1.jar` dans `mods/` d'un profil Minecraft 1.21.1 + NeoForge 21.1.x.
2. Créer ou ouvrir un monde plat (commandes activées).
3. Exécuter `/rv spawn` (Berline bleue) ou `/rv spawn sport` (Coupé rouge).
4. Vérifier visuellement le châssis, les phares, les feux arrière et les 4 roues posées au sol sans tremblement.
5. Clic droit sur la voiture pour monter ; vérifier dans *Options -> Contrôles -> Realistic Vehicles* que `Z`, `S`, `Q`, `D`, `Espace` sont reconfigurables.
6. Conduire (`Z` accélérer, `S` freiner puis marche arrière, `Q`/`D` tourner, `Espace` frein à main) et vérifier la rotation/braquage/suspension des 4 roues, les feux stop/recul, les collisions contre un mur et `/rv info`.
7. Appuyer sur `Shift` pour descendre côté conducteur.
