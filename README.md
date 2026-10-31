# Debug Outline (Fabric 1.21.4)

Contours colorés à travers les murs pour les coffres, coffres piégés, barils et shulkers.
Tracers (lignes depuis la caméra) optionnels.

## Touches (modifiables dans Options > Contrôles)

- H : activer / désactiver le mod
- T : activer / désactiver les tracers
- J / K / L / U : filtrer coffres / coffres piégés / barils / shulkers

## Couleurs

- Coffres : orange
- Coffres piégés : rouge
- Barils : jaune
- Shulkers : violet

## Réglages

Dans `DebugOutlineClient.java` :
- `SCAN_RADIUS` : rayon de recherche (256 par défaut)
- `MAX_RENDER_DISTANCE` : distance maximale de dessin
- `SCAN_INTERVAL_TICKS` : fréquence de rescan

## Compiler

`./gradlew build` (JDK 21). Le jar se trouve dans `build/libs/`.
