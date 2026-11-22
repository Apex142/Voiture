# Realistic Vehicles — Mod Automobile Ultra-Réaliste (Minecraft 1.21.1 / NeoForge)

Mod autonome (un seul fichier `RealisticVehicles-1.21.1.jar`, aucun mod externe requis) visant une simulation automobile réaliste sur **Minecraft 1.21.1** avec **NeoForge 21.1.x** et **Java 21**.

## Prérequis

- **Minecraft** : `1.21.1` strictement
- **Mod Loader** : NeoForge `21.1.x` (testé avec `21.1.267`)
- **Java** : JDK 21 (compilation & exécution)
- **Aucun autre mod requis**

## Compilation et vérifications qualité

```powershell
# Vérifier la limite stricte des 200 lignes et l'isolation architecturale
python tools/check-file-length.py
python tools/check-architecture.py

# Compiler, exécuter les tests unitaires, les scripts qualité et générer le JAR
./gradlew.bat check build
```

Le mod compilé est produit dans :

`build/libs/RealisticVehicles-1.21.1.jar`

## Lancer Minecraft en développement

```powershell
./gradlew.bat runClient
```

## Utilisation (Prototype Phase 1)

1. Ouvrir un monde Minecraft 1.21.1 (créatif ou commandes activées).
2. Faire apparaître une voiture devant le joueur :
   - `/rv spawn` (Berline 1 450 kg par défaut)
   - `/rv spawn sport` (Coupé sportif 1 320 kg)
   - `/rv info` (Affiche vitesse, RPM, rapport, suspensions et contact roues)
   - `/rv remove` (Supprime les véhicules dans un rayon de 32 blocs)
3. **Clic droit** sur la voiture pour monter au poste de conduite (`Shift` pour descendre).
4. **Commandes par défaut** (reconfigurables dans *Options -> Contrôles -> Realistic Vehicles*) :
   - `Z` : Accélérer
   - `S` : Freiner / Marche arrière
   - `Q` : Tourner à gauche
   - `D` : Tourner à droite
   - `Espace` : Frein à main

## Documentation du projet

- [TASKS.md](TASKS.md) : Liste exhaustive et suivi d'avancement de toutes les phases (0 à 12)
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) : Stratégie technique
- [ARCHITECTURE.md](ARCHITECTURE.md) : Principes SOLID et design patterns
- [DECISIONS.md](DECISIONS.md) : Décisions architecturales (ADR)
- [PROGRESS.md](PROGRESS.md) : Journal chronologique
- [TEST_RESULTS.md](TEST_RESULTS.md) : Rapports de tests
- [BENCHMARKS.md](BENCHMARKS.md) : Mesures CPU/GPU
- [KNOWN_ISSUES.md](KNOWN_ISSUES.md) : Problèmes ouverts
- [ASSET_LICENSES.md](ASSET_LICENSES.md) : Audit des licences
