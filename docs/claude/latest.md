# Laatste sessie

- Datum: 2026-09-28.
- Vraag: ragdoll ligt nog op de grond terwijl het wezen al loopt/aanvalt; fixen, dan commit+push; daarna betere
  speler-animatie bij het bouwen van de mech (met de nieuwe rig).
- Oorzaken: server gaf AI terug los van de ragdoll-timing (75 tikken vs 30-142 op client); `HeldMobs.release` gaf AI
  meteen terug (hand/tentakel/mech/Thor) terwijl de client het lichaam liet liggen; lichaam gleed los van het wezen.
- Fix: `Knockdowns` zet AI uit vanaf de worp (ook blast via `ExplosionKnockbackEvent`, en na loslaten), beweegt het
  wezen zelf (vlucht, glijden), stuurt `KnockdownPayload` (vliegt / nog X tikken / vrij). Client `Knocked` laat het
  lichaam opstaan zodat het 5 tikken vóór de AI terugkomt staat; `Ragdoll.keepNear` houdt het lichaam binnen 0,7
  blok van het wezen; `EntityShadowMixin` tekent een lichaam dat half buiten beeld ligt.
- In-game getest (worp, vasthouden+loslaten, explosie; logs + screenshots): lichaam ligt bij wezen, staat op, wezen
  loopt pas daarna. Payload-codec round-trip getest. Build + unit-tests groen. Testklasse, wereld, shots weg.
- Gecommit `515a7de`, gepusht, release v0.4.5-alpha (Latest), gepubliceerd vanuit schone worktree.
- Deel 2: piloot-animatie tijdens de mech-bouw. Nieuw `PilotBody` (hele lichaam via `Stance`, keyframes): schrap
  met ringvuist + hand om pols, schok per voet, hurk, sprong met knie op en vuist hoog, hurk in de lucht, armen
  wijd, echte klap voorover, zitten met voeten op steun, naar voren naar de hendels (IK vanuit wereldpositie).
  Oude `MechMoves.PILOT`/`pilotArm` weg. Gefilmd in-game (voor/na, voor- en zijaanzicht). POWERS.md en
  GREEN_LANTERN.md bijgewerkt, CHANGELOG 0.4.6-alpha. Build + tests groen. User: "commit and push" -> gecommit,
  gepusht, release v0.4.6-alpha vanuit schone worktree.
- Regels: release-vanuit-worktree in project-CLAUDE.md, `gh ... isLatest` in globale CLAUDE.md.
- Niet van mij, niet aangeraakt: `docs/CHARACTERS.md`, `Roster.java` (andere sessie), nog ongecommit.
- Open: ideeën #24, #25, #31, #32, #33 wachten op ja/nee van user.
