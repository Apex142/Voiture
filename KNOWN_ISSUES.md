# REALISTIC VEHICLES — KNOWN ISSUES & BLOCKERS

## Problèmes ouverts

| ID | Phase | Sévérité | Description | Statut / Piste |
|---|---|---|---|---|
| KI-001 | P0 | Faible | `java` par défaut sur le PATH système est Zulu 17 (`JAVA_HOME` non fixé à JDK 21). | Contourné proprement par `gradle.properties` (`org.gradle.java.home`) et `java.toolchain` version 21 ciblant Corretto 21.0.7. |
| KI-002 | P4 | Info | L'archive `mercedes-benz-slk-55-amg-2012.zip` dans le répertoire local contient un rip Forza Motorsport 7 (`FM7`) non redistribuable. | Exclue du dépôt et du JAR (`ASSET_LICENSES.md`). Un modèle original ou CC0/CC-BY sera utilisé en Phase 4. |
| KI-003 | P3 | Info | Plusieurs couches Vulkan tierces système (OBS, RTSS, GalaxyOverlay) émettent des warnings dans `vulkaninfo`. | À surveiller lors de l'activation des Vulkan Validation Layers en Phase 3 (désactiver implicit layers si besoin en dev). |
