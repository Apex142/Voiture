# REALISTIC VEHICLES — BENCHMARKS (CPU / GPU / MÉMOIRE)

> Mesures de référence enregistrées au fil des phases. Aucune valeur inventée.

## Matériel / Environnement de référence
- **OS** : Windows 10 (amd64)
- **JVM** : OpenJDK 21.0.7 (Amazon Corretto 21.0.7+6-LTS)
- **API Vulkan hôte** : Vulkan 1.4.341 (`vulkaninfo`)

## Benchmarks Physique CPU (Moteur pur Java 21 — `PhysicsBenchmarkTest`, 2026-11-21)

Conditions : 200 ticks mesurés après 40 ticks de warmup JIT, pas serveur `20 Hz` (`50 ms` budget) découpé en `6` sous-pas fixes à `120 Hz` (`dt = 1/120 s`), accélération + braquage actifs sur `TestCollisionWorld`.

| Nombre de véhicules | Temps moyen / tick 20 Hz | % du budget tick (50 ms) | Temps / véhicule / tick |
|---:|---:|---:|---:|
| 1 véhicule | **0.0692 ms** | 0.14 % | 0.0692 ms |
| 5 véhicules | **0.2351 ms** | 0.47 % | 0.0470 ms |
| 10 véhicules | **0.4021 ms** | 0.80 % | 0.0402 ms |
| 20 véhicules | **0.5265 ms** | 1.05 % | 0.0263 ms |

## Benchmarks GPU / Rendu Minecraft + Vulkan

*(Mesures ajoutées lors des Phases 3, 4 et 11 : FPS moyen, 1% low, frame time CPU/GPU, RAM, VRAM.)*
