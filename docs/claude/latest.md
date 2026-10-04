# Laatste sessie

## Animatie-rework teruggedraaid (2026-10-03, andere sessie dan hieronder)
- Verzoek: alles van de animatie-rework weghalen. Gedaan: mijn 42 bestanden terug naar HEAD, 3 nieuwe weg
  (`BodyMix`, `BodyKey`, `RingArms`), eigen regels uit `CLAUDE.md` en rule-candidates weg. Niets gecommit.
- Gecheckt: hashes = HEAD, `compileJava` + `compileTestJava` groen. Backup-patch (45 bestanden) in temp-scratchpad;
  weg op verzoek. Werk van de sessie hieronder (knockdowns, paneel, stamina) niet aangeraakt.

## 0.7.3-alpha: manager, tour, HUD, knockdowns, mech (2026-10-04)
- Verzoek: manager-pagina's samenvoegen, kill-geluid, paneel altijd in de hoek, Grab-Dash Dive op spelers, ragdoll
  speler 1,5 s, Mjolnir-pixels, dome, geen UI-overlap, LOCKED, vlammenwerper-ring, tour, riemhamer, mech na klimmen
  en benen bij slaan. Daarna: "zo snel mogelijk afronden, alles committen en pushen, met ALLE niet-uitgebrachte
  wijzigingen in de changelog".
- Gedaan: alles hierboven, in game getest; `./gradlew build` groen (alle tests). 0.7.2 (nooit uitgebracht) zit nu in
  de 0.7.3-sectie van `CHANGELOG.md`. Review-fixes: `Downed.LONGEST` terug op 300, ongebruikt `PixelIcons` VERSIONS weg.
- "Flikker" bij loslaten gedragen speler: geen bug, lichaam blijft slap (log + shots).
- Gecommit en gepusht (0.7.2 + 0.7.3), release v0.7.3-alpha via `scripts/release.ps1 publish`.
- Open (morgen): lokale `CLAUDE.md`-notities (HudSpace, HeldPlayers, ModelAssetsTest, DomePainter, NavScreen-tabs,
  manager-pagina's, tour alleen eigen versie, knockdown 1,5 s); bugs #59-#63 (high); ideeën #25, #31, #32, #33, #24.
- `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM*.md` (andere sessie) niet meegecommit.

## Thor-ontwerp (andere sessie, 2026-10-02)
- Ontwerp `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM.md` + `_SHORT.md` (niet gecommit, van die sessie). Besloten:
  Throw and Follow = getimede dash, gewone worp max 24. Open: 10 beslissingen in §9; daarna fase 0 bouwen.

