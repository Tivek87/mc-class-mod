# Laatste sessie

## Animatie-rework teruggedraaid (2026-10-03, andere sessie dan hieronder)
- Verzoek: alles van de animatie-rework weghalen. Gedaan: mijn 42 bestanden terug naar HEAD, 3 nieuwe weg
  (`BodyMix`, `BodyKey`, `RingArms`), eigen regels uit `CLAUDE.md` en rule-candidates weg. Niets gecommit.
- Gecheckt: hashes = HEAD, `compileJava` + `compileTestJava` groen. Backup-patch (45 bestanden) in temp-scratchpad;
  weg op verzoek. Werk van de sessie hieronder (knockdowns, paneel, stamina) niet aangeraakt.

## 0.7.2-alpha: manager, tour, Thor, Ock, testgevecht (2026-10-04)
- Verzoek: pauzemenu-knop; tour fixen + opnieuw te starten; manager-UI + QOL; GL-benen bij arrival; Thor: Mjolnir
  groter en goed in de hand, geen spin, vuisten trager zonder clipping, grab niet slap + einde kiezen in 1,5 s; Ock:
  vrije tentakels; spelers bijna altijd ragdoll; testgevecht (hurken + scrollwiel 3 s); polsen/enkels nooit
  onmogelijk. Daarna: "commit 0.7.1 en 0.7.2, niet pushen".
- Gedaan: alles hierboven + eerder verzoek (gids-groepen, overzicht, tour). In game getest, `./gradlew build` groen.
- Lokaal gecommit als 0.7.2-alpha (`Manager pages, tour, Thor grab picks and test fight`), niet gepusht.
  0.7.1-alpha stond al gecommit, gepusht en gereleased (Latest); daar viel niets te committen.
- Open: push + `scripts/release.ps1 publish` (v0.7.2-alpha) wachten op ja. Bugs #59-#63 bewust niet aangeraakt.
  `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM*.md` (andere sessie) niet meegecommit.

## Thor-ontwerp (andere sessie, 2026-10-02)
- Ontwerp `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM.md` + `_SHORT.md` (niet gecommit, van die sessie). Besloten:
  Throw and Follow = getimede dash, gewone worp max 24. Open: 10 beslissingen in §9; daarna fase 0 bouwen.

