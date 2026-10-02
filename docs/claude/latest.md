# Laatste sessie

- Datum: 2026-10-02. Verzoek: opstaan na ragdoll vloeiender/dynamischer, betere overgang limp -> opstaan, limp vanuit
  elke pose (ook tijdens opstaan, ook bij gewone klap), nieuwe botten goed gebruiken. Bugs/ideeën NIET aangeraakt.
- `getup/`: `PersonRise` herschreven op `RiseMoments` (per ledemaat eigen timing, handen plat, voeten plat/tenen,
  hand op knie, schouderbladen volgen armen, variatie per keer); `GetUp.facing` (opstaan langs het lichaam: fix
  zij-ligging-sprong); `CreatureRise` op de grond gehouden, poten gevouwen, voorpoten eerst; kortste draai-richting.
- Klap tijdens opstaan: server `Knockdowns` (RISING-venster) + client `Knocked.again` -> `RagdollBody.knockedDown` +
  `RagdollFalls.knockBack`. `Facings`: na opstaan blijft hij zo staan tot hij zelf beweegt (geen draai op de plek).
- Tests: build groen (135 tests; nieuw `RiseFacingTest`, `KnockedTest` +1). ~9 in-game runs met screenshots:
  voor/na, zij, zwaard, koe, varken, villager, vindicator, klap tijdens opstaan, worp tijdens lopen, vermoeid.
  Testklasse, film-tool, werelden en shots verwijderd. Versie 0.5.7-alpha (prepare gedaan) + CHANGELOG.
- CLAUDE.md: regel "game altijd starten om in-game te testen", summon-NBT-regel, getup-beschrijving bijgewerkt.
- Gebruiker: "commit and push all" -> 3 commits: Ultron (werk andere sessie), `CLAUDE.md` lokaal (untracked,
  `.gitignore`), get-up + CHANGELOG 0.5.7-alpha. Daarna push naar origin/master en `release.ps1 publish`.
- Open: niets van deze taak; bug #35 en ideeën blijven onaangeroerd (opdracht gebruiker).
