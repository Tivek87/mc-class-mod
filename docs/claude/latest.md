# Laatste sessie — 2026-09-27

- **Vraag:** mech nog te blokkerig en onnauwkeurig (vooral het hoofd); ronder, veel meer detail, animaties 1-op-1 met `docs/reference/mech-robot-ultimate.mp4` (draaiend hoofd, draaiende armen).
- **Engine:** `Surface` + `SurfacePath` (gladde lofts/lathes) en `Mesh.tiled`/`tiles`/`sheet` (`MeshTiles`): honderden verhoogde tegels met gloeiende randen, ronde kapjes op polen; skins tekenen alleen zijden naar je toe; `Mesh.Tiles`, per-zijde `glow`.
- **Model:** alle delen opnieuw (`client/mech/*Shapes`): slanke hoge schenen, klauwvoeten, hoekige dijen, tonborst met hoog smal boograam, koepelschouders met hoorns, gauntlets, eivormig hoofd met gezichtsplaat, kam en hoorns; eigen groen `LanternPainter.MECH_LIGHT`.
- **Animatie:** onderarmen draaien in, klap van opzij, hoofd vormt draaiend uit scherven, tuimelt kop over kont terug naar de nek; lichtring achter de borst; armen wijd bij het eind; camerashots dichter op de clip; wijdere stand.
- **Opwarmen:** mech-onderdelen worden bij start op een achtergronddraad gebouwd (was 139 ms hapering, nu 0).
- **Getest in-game:** 11 runs, schermafdrukken naast de clip; ~103-110 fps; `gradlew build` OK; testklasse, testwereld en screenshots weg.
- **Docs:** CHANGELOG `[0.3.2-alpha]`, POWERS, GREEN_LANTERN, PROJECT, CLAUDE.md (Surface/tiled skins).
- **Release:** op verzoek alles gecommit (eigen `.ogg` → `.mp3`-wissel apart) en gepusht, release v0.3.2-alpha.
- **Open:** ideeën #24/#25 wachten op ja/nee; `docs/copilot chat bonesxragdoll.txt` blijft buiten git (projectregel: geen chats in de repo).
