# Laatste sessie

- Datum: 2026-10-02. Verzoek: ideeën #45 (botten, ook de mech) en #58 (ragdoll/stumble) afmaken; andere bugs/ideeën
  NIET aangeraakt. Keuzes gebruiker: fatigue-knockdown en domino als wereldinstelling (aan), mech-wandhand overgeslagen.
- #58: wand-slump (dood en levend gegooid, `RagdollFalls`), opstaan vanuit zit (`PersonRise.SEATED`), leunen op
  zwaard/bijl (`Brace`), domino (`Knockdowns.bump`), vermoeidheid (`Fatigue`, `FatiguePayload`, `Tired`), dreunen +
  stof (`RigidWorld.impact`, `Thuds`), afstands-detail + 4 client-instellingen. Netwerkversie 21.
- #45: mech-pols (vorm gesplitst in FOREARM + HAND), schouders schuddend/rollend om het sleutelbeen, veren op lopen,
  explosies (`MechWalk.blast` via `ClientPacketListenerMixin`) en klappen op de piloot.
- `Ragdoll.java` (709 r.) gesplitst: `RagdollBody` (staat/beweging) + `Ragdoll` (tekenen). CLAUDE.md + CHANGELOG bij.
- Tests: build groen (132 tests; nieuw: 2 in `RigidWorldTest`, `FatiguePayloadTest`); ~12 in-game runs; testklassen,
  werelden en shots verwijderd. Versie 0.5.6-alpha (prepare gedaan).
- #45 en #58 op GitHub gesloten (completed, "Finished"-commentaar). `bugs.ps1` nu tweerichtings: `fixed` sluit
  meteen op GitHub; `sync` sluit lokale fixes die daar nog open staan, haalt heropende terug, zet op GitHub als
  klaar gesloten issues in `bugs/fixed/`; `close` (release) zet alleen de versie erbij. Getest met nep-`gh` (28
  checks). Sync-taak bestond niet (pwsh ontbreekt): `schedule` valt terug op Windows PowerShell, taak draait (0).
- Commits `315a356` (features) + bug-sync; daarna push + `release.ps1 publish` v0.5.6-alpha (gebruiker zei ja).
  Niet gebouwd: speer/drietand-leunen, grijp-/slipgeluiden (horen bij #46/#48).
