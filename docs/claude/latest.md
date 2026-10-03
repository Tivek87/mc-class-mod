# Laatste sessie

## Animatie-rework teruggedraaid (2026-10-03, andere sessie dan hieronder)
- Verzoek: alles van de animatie-rework weghalen. Gedaan: mijn 42 bestanden terug naar HEAD, 3 nieuwe weg
  (`BodyMix`, `BodyKey`, `RingArms`), eigen regels uit `CLAUDE.md` en rule-candidates weg. Niets gecommit.
- Gecheckt: hashes = HEAD, `compileJava` + `compileTestJava` groen. Backup-patch (45 bestanden) in temp-scratchpad;
  weg op verzoek. Werk van de sessie hieronder (knockdowns, paneel, stamina) niet aangeraakt.

## Kosten, gids per mode, compact paneel; release 0.6.9-alpha (2026-10-03)
- Verzoek: alles van Green Lantern kost power; P-gids per mode met alleen binds die daar iets doen; paneel
  rechtsonder veel compacter, fade in bij gebruik/gevecht, fade uit daarna; dan commit, push, release.
- Kosten (`PowerRing.pay`/`upkeep`, alle als setting): wapen vormen 1, vasthouden 0.05/s, slag/zweepslag 0.1,
  opstijgen 0.8, beam lock 0.2, kooi 0.2/s + pound 1, mech 0.2/s + klap 0.5. Client weigert al zonder power.
- Gids: tabs (GL Ground/Flight/Mech/Sword/Flamethrower, Whip pas na ontgrendelen; Thor Fists/Hammer/Flight; Ock
  Ground/Climbing/Holding), koppen, kosten-chips, status; lijst zo breed als de langste rij met korte toetsnamen.
- Paneel: hele pixels, max halve schermhoogte, vrij van hotbar; fade in/uit (5 s); groene stip; flits per rij; 2×Space.
- Ook (vorige sessie): "ready" alleen met power/stamina; Sky Shockwave + spelers-knockdown 3 s.
- "112 bugs"-regel niet in changelog (niet waar). In-game getest (4 runs) + `./gradlew build` groen.
- Open: bugs #59-#63 (high) + ideeën: nieuwe ja nodig. `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM*.md` (andere sessie)
  niet meegecommit; noemt nog `SkyMoves.ground`, die bestaat niet meer.

## Thor-ontwerp (andere sessie, 2026-10-02)
- Ontwerp `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM.md` + `_SHORT.md` (niet gecommit, van die sessie). Besloten:
  Throw and Follow = getimede dash, gewone worp max 24. Open: 10 beslissingen in §9; daarna fase 0 bouwen.

