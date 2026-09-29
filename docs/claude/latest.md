# Laatste sessie

- Datum: 2026-09-29. Verzoek: alle hoognodige code-/structuurproblemen fixen, niets uit `bugs/`; vraag over worktree.
- Worktree: was alleen tijdelijk in de scratchpad voor `release.ps1 publish` (andere sessie had `MechScript.java`
  open), daarna verwijderd. Nu alleen `master`, geen extra branch of worktree.
- Gecontroleerd: mappen <= 12 bestanden, bestanden < 600 regels, geen compiler-waarschuwingen (-Xlint), opruimlijst in
  `MultiversePowers.onServerStopping` compleet, `hurt()` zonder faction-check alleen op al gekozen doelen.
- Gefixt: 14x `level.sendParticles` -> `ParticleFx.sendNear` / `ParticleBatch.add` (multiplayer-regel, zelfde bereik);
  16x server `level.clip` -> `LoadedWorld.clip` (geen chunk-laden, geen server-freeze), o.a. `engine/target/Targeting`.
- Getest: build + 111 unit-tests groen. Niet in-game getest.
- Niet gecommit: wacht op ja voor commit + push (= release 0.5.2-alpha). `MechScript.java` (andere sessie) blijft erbuiten.
- Open (niet urgent): dubbele code (joints `rejoin`, flame/whip `onCameraAngles`, `pay`/`drain`, `rim`).
