# REALISTIC VEHICLES — ARCHITECTURE DECISION RECORDS (DECISIONS.md)

## ADR-001 — Toolchain Minecraft 1.21.1 + NeoForge 21.1.x via ModDevGradle 2.0
- **Date** : 2026-11-21
- **Statut** : Acceptée
- **Contexte** : Le cahier des charges impose Minecraft 1.21.1 strictement, NeoForge 21.1.x, Java 21 et Gradle, dans un JAR unique `RealisticVehicles-1.21.1.jar`.
- **Décision** : Utiliser Gradle 8.11.1 (wrapper), Java 21 (toolchain Corretto 21.0.7 disponible localement), plugin officiel `net.neoforged.moddev:2.0.148` et NeoForge `21.1.267` avec mappings Parchment `2024.11.17` pour `1.21.1`.
- **Conséquences** : Compilation rapide, support natif JUnit 5, configuration cache Gradle, aucune dépendance externe côté joueur.

## ADR-002 — Projet mono-module initial à frontières strictes vérifiées automatiquement
- **Date** : 2026-11-21
- **Statut** : Acceptée
- **Contexte** : Une architecture multi-modules Gradle dès le premier jour complique le packaging Jar-in-Jar avec ModDevGradle avant même d'avoir un prototype jouable.
- **Décision** : Structurer le code par packages strictement séparés (`core`, `physics`, `render.api`, `minecraft`, `client`) et imposer l'isolation via `tools/check-architecture.py` dans `./gradlew check`. L'extraction vers des sous-modules Gradle (`rv-physics`, `rv-vulkan`) restera triviale lorsque la couche Vulkan sera introduite en Phase 3.
- **Conséquences** : Un seul JAR `build/libs/RealisticVehicles-1.21.1.jar` dès la Phase 0 et 0 couplage physique/Minecraft.

## ADR-003 — Moteur physique automobile maison en Java 21 pur à pas fixe 120 Hz
- **Date** : 2026-11-21
- **Statut** : Acceptée
- **Contexte** : Les mods externes (Jolt/Bullet en tant que mods séparés) sont interdits. Une simulation de suspensions automobiles réalistes à 20 Hz (tick Minecraft) diverge numériquement.
- **Décision** : Implémenter notre propre moteur physique corps rigide 3D + suspensions 4 roues + modèle de pneu `BrushTireModel` (extensible via `TireModel` vers `PacejkaTireModel`) avec un pas fixe `1/120 s` (6 sous-pas par tick Minecraft) et amortissement visqueux basse vitesse.
- **Conséquences** : Stabilité complète à l'arrêt, pas de rebond parasite, 0 dépendance native pour la physique en Phase 1, tests unitaires JUnit 5 en < 1 seconde.

## ADR-004 — Rendu provisoire abstrait en Phase 1, Backend Vulkan complet en Phase 3
- **Date** : 2026-11-21
- **Statut** : Acceptée
- **Contexte** : La priorité absolue est de faire rouler une voiture physiquement dans Minecraft avant d'attaquer la réécriture du renderer Vulkan, tout en conservant Vulkan comme exigence finale non négociable.
- **Décision** : Définir l'abstraction `RenderBackend` + `VehicleRenderState` dès la Phase 1 et utiliser un `ProvisionalVehicleRenderer` articulé pour valider la physique en jeu. En Phase 3, développer le backend Vulkan complet (LWJGL Vulkan + VMA + SPIR-V + interception GLFW/RenderSystem) embarqué dans le JAR unique, sans supposer de partage implicite de frame OpenGL/Vulkan.
- **Conséquences** : Validation physique immédiate, aucune dette de couplage, environnement hôte déjà vérifié avec Vulkan 1.4.341.

## ADR-005 — Contrôles par défaut AZERTY (`Z/S/Q/D/Espace`) avec contexte de conflit dédié véhicule
- **Date** : 2026-11-21
- **Statut** : Acceptée
- **Contexte** : L'utilisateur demande explicitement de conduire avec `Z` (accélérer), `S` (freiner/reculer), `Q` (gauche), `D` (droite), `Espace` (frein à main) tout en gardant les commandes reconfigurables.
- **Décision** : Enregistrer 5 `KeyMapping` NeoForge dans la catégorie `Realistic Vehicles` avec un `IKeyConflictContext` personnalisé (`VehicleKeyConflictContext`) actif uniquement lorsque le joueur conduit un `VehicleEntity`.
- **Conséquences** : Les touches `Z/S/Q/D/Espace` fonctionnent immédiatement en voiture, sont 100 % reconfigurables dans Options -> Contrôles et n'entrent pas en conflit avec la marche à pied.

## ADR-006 — Politique stricte de licences d'assets (exclusion de l'archive FM7 locale)
- **Date** : 2026-11-21
- **Statut** : Acceptée
- **Contexte** : Le dossier racine contient `mercedes-benz-slk-55-amg-2012.zip` incluant `Mercedes_SLK55_AMG_2012_FM7_Sketchfab.glb` (rip Forza Motorsport 7 non redistribuable).
- **Décision** : Ne jamais committer ni embarquer cette archive dans le mod (ajoutée au `.gitignore` et tracée dans `ASSET_LICENSES.md`). En Phase 4, utiliser exclusivement un modèle original ou une licence ouverte vérifiée (CC0 / CC-BY).
- **Conséquences** : Conformité légale totale (règle 19).
