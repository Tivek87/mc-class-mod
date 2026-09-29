# Laatste sessie

- Datum: 2026-09-29. Verzoek: `client/mech/` (19 bestanden) in sub-mappen splitsen, alle mappen/bestanden herstructureren,
  optimaliseren (keuze: structuur), docs opruimen; daarna "rond af, commit en push" -> release 0.5.1-alpha.
- Mappen: `client/mech/` -> `shape/`, `walk/`, `touch/`; `engine/client/render/` -> `mesh/`, `entity/` (+`Lens` naar
  `engine/client/fx`); `engine/physics/joint/`; `thor/client/` -> `motion/`, `pose/`, `blow/`; `render/hand/light/`;
  `mixin/client/` + `mixin/client/render/` (`welcomescreen.mixins.json` bijgewerkt). Pakket-private leden die nu over
  pakketgrenzen gaan zijn `public` gemaakt.
- Bestanden >600 regels: `ModelParts` -> `ModelBends` (buigen), `TrackedConstructs` -> `ConstructShakes` (in de keten).
- Dode code weg (12 leden, 2 teksten in `en_us.json`); vector-hulpjes (`flat`, `unit`, `square`, `yawed`) naar `Vectors`.
- Docs: idee-/ontwerp-docs naar `docs/ideas/`; CLAUDE.md-indeling en README bijgewerkt.
- Getest: 111 unit-tests groen; game start zonder mixin-fouten (niet in een wereld gespeeld).
- Niet van mij: `MechScript.java` (`side`-helper, WIP andere sessie) bleef buiten de commit.
- Open: overige dubbele code (joints `rejoin`, flame/whip/sword pose-hulpjes, `pay`/`drain`) nog samen te voegen;
  bug #35 (GL-vliegen traag); ideeën wachten op ja/nee. Issue-teksten in `bugs/` noemen nog oude doc-paden.
