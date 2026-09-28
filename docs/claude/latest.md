# Laatste sessie

- Datum: 2026-09-28.
- Vraag: Thor-moves (dash, super jump, vliegen + blink/dive/lightning), Mouse & Space-controls, ragdolls liggen en staan op,
  mech (torso, hoofd, rennen, duim, first person), handen raken/vingers, bugs #27-#30, whip op slot, test-hand, heranimatie.
- Gedaan: alles gebouwd en in-game getest (screenshots); build + 76 tests groen; bugs #27-#30 op fixed.
- Opgesplitst (>600 regels): `Ragdoll`+`RagdollBuild`, `Ragdolls`+`RagdollCauses`, `RigidWorld`->`RigidBlocks`->`RigidBodies`.
  Alleen verplaatst; compile + tests groen, niet apart in-game getest.
- Docs: POWERS/PROJECT/GREEN_LANTERN, CLAUDE.md layout, CHANGELOG 0.3.8-alpha (release.ps1 prepare gedaan).
- Test-harness, testwerelden en screenshots verwijderd; 3 voicelines via git rm weg.
- 5 referentie-audiobestanden verwijderd meegecommit (gebruiker: alles committen).
- Commit + push naar origin/master en release v0.3.8-alpha; ideeen #24/#25/#31 wachten op ja/nee.
