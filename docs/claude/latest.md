# Laatste sessie

- Datum: 2026-09-28.
- Deel 2 (2026-09-29): lichaamsdelen door elkaar/verkeerd om. Nieuw `LimbJoint` (ellips om een midden-richting uit
  de rusthouding, echte grenzen schouder/heup/nek/poten, twist vanaf rust), meer botsparen (arm-arm, poten van vier,
  hele capsules), `RigidCrowd`/`RagdollCrowd` (lijven botsen onderling; diep-in-elkaar begonnen = negeren, geen
  wegslingeren), inslapen op netto verplaatsing. Tests: benen kruisen 0/24 (was 22/24), overlap gehalveerd, 98 groen.
  In-game: stapel van 4, gegooid paar, dieren+dorpeling ok. Release 0.4.8-alpha.
- Deel 1: romp in 2 delen (borst/buik) bij alle mobs, voor poses en ragdolls, glitch-vrij; daarna: liggende ragdolls
  glijden (fix) en na landen eerst 3 s op de grond liggen vóór opstaan; daarna commit + push + release.
- Gedaan: `SpineJoint` (taille: vouwen/zijwaarts/draaien binnen grenzen), `ModelParts.waist/far/core`, `BentParts`
  buigt ook wat de romp draagt (robe, zadel, staart), `Limbs.spine` voor elk dier, `GetUp` mengt per romphelft.
- Glijden: oorzaak in `RigidWorld`: wrijving zat vóór het opnieuw koppelen van gewrichten -> elk substapje schoof het
  lijf een beetje (tot 2,8 blok in 10 s). Nu wrijving als laatste stap. `keepNear` trekt stilliggend lijf niet meer.
- 3 s-regel: `Knocked.LIES` = 60 tikken op de grond (`touching`), server `Knockdowns.DOWN` 80 -> 110.
- Getest: 93 unit-tests groen (nieuw: `RagdollRestTest`, `KnockedTest`, `ModelPartsTest`); in-game 3 runs: geen
  NaN, lijken 0,000 blok kruip, opstaan bij lain 65-67, taille-knik zonder gaten. Testklasse/wereld/shots weg.
- Release 0.4.7-alpha (CHANGELOG + CLAUDE.md layout bijgewerkt).
- Niet van mij, niet aangeraakt: `docs/CHARACTERS.md`, `Roster.java` (andere sessie).
- Volgende (user): lichaamsdelen door elkaar/verkeerd draaien fixen; dan ren-animatie zwaarder; mech armen/benen
  los + vast in gaten (klimmen/springen).
- Open: ideeën #24, #25, #31, #32, #33 wachten op ja/nee.
