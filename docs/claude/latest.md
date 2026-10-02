# Laatste sessie

- Datum: 2026-10-02. Verzoek 1: ragdoll-/animatieglitches (botten/handen te ver gedraaid, volgorde), opstaan per
  mobtype, bone view onzichtbaar bij mech e.a. Verzoek 2: alleen bug #35 (vliegen te traag) fixen, dan commit/push/release.
- Bug #35: `LanternAbilities` flight topSpeed 1.8 -> 16 b/s, start 10.7, cruise 13.3 (`.was` + `DEFAULTS_VERSION` 25,
  fallbacks in `FlightSteering`). In-game: oude wereld (1.8) naar 16 gezet, vlucht haalt 16.00 b/s (server gelijk).
  Gesloten met `bugs.ps1 fixed 35`.
- Bone view: tekent nu bij `AFTER_LEVEL` (onder Fabulous verdwenen botten achter alles); heuplijnen bij gewaden vanaf de heup.
- Gewrichten: `engine/rig/Limits` + `ModelBends.Bend.keep` (pols/enkel: vouwen, kantelen, draaien binnen bereik),
  in `Limbs.turn`, `Skeleton.reach` (IK), Brace; `FoldChain`: onderarm/scheen neemt de twist, hand/voet blijft heel.
- Poses: gewrichten faden mee (`Poses`), schouders niet dubbel; FootPlanting (cirkel-oplossing, geen 46°-schop,
  harnasstandaard met rust); PoseGuard (geen X-benen, geen flip); Tired niet op vastgehouden wezens; Stance dubbel draaien.
- Opstaan: `GetUp.Kind` (PERSON, FOLDED, FRONT_FIRST, HIND_FIRST, EVEN, BIRD; profiel-veld `rise`); villager/heks/
  illager zonder handen (`RiseMoments.*_FOLDED`); `CreatureRise` stijlen + rol om de ruggengraat.
- Ragdoll: gekruiste armen vast op de borst, onderdelenboom (`ModelBends.parents`), anker op eigen box, platen/
  vervorming in `ModelParts.grow`, achterhoeven, ijzergolem niet meer stijf, `knockdown_none`-tag (server).
- Tests: build groen; nieuw `LimitsTest`, `RiseKindsTest`, + BentParts/ModelParts/FootPlanting. 6 in-game runs
  (16 mobs, mech Fancy/Fabulous/1e/3e persoon, tired-fade). Testklasse, werelden, shots verwijderd.
- Versie 0.5.8-alpha (prepare gedaan) + CHANGELOG. CLAUDE.md (lokaal) bijgewerkt.
- Commit + push + release v0.5.8-alpha met ja van gebruiker.
- Open: nieuwe hoge bugs #59-#62 (gooien tegen ijzergolem: `Knockdowns.bump` slaat wezens boven `HEAVY` over; Doc Ock
  grab) bewust niet aangeraakt. Niet gedaan: warden/kikker diepere boom, robe-knie-clipping.
