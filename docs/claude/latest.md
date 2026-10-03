# Laatste sessie

## Animatie-rework teruggedraaid (2026-10-03, andere sessie dan hieronder)
- Verzoek: alles van de animatie-rework weghalen. Gedaan: mijn 42 bestanden terug naar HEAD, 3 nieuwe weg
  (`BodyMix`, `BodyKey`, `RingArms`), eigen regels uit `CLAUDE.md` en rule-candidates weg. Niets gecommit.
- Gecheckt: hashes = HEAD, `compileJava` + `compileTestJava` groen. Backup-patch (45 bestanden) in temp-scratchpad;
  weg op verzoek. Werk van de sessie hieronder (knockdowns, paneel, stamina) niet aangeraakt.

## Versies wisselen, rustig paneel, gids met uitklap; release 0.7.0-alpha (2026-10-03)
- Verzoek: What's new met laatste 10 versies + wisselen (spel sluit) + 1 knop naar latest; joinen met andere
  modversie → kiezen: terug of wisselen; paneel rechtsonder rustiger, ruimer, GUI-schaal; gids met korte
  samenvatting + uitklap; bereikbaar vanuit update manager; daarna commit, push, release.
- Versies: `ChangelogScreen` ("What's new & versions"), `VersionProbe` (kanaalversie = modversie),
  `VersionMismatch` + `VersionMismatchScreen`. Echt getest: server 0.6.8 weigert client 0.6.9 → eigen scherm, juiste versies.
- Paneel: GUI-schaal, max ~60% schermhoogte (eerst krappere rijen, dan stap kleiner), groepen muis/toetsen/gebaren,
  "hold"/"2×" voor de toets, kleuren en status faden. Gesplitst: `AbilityPanelRows`; gids: `GuideAbout`.
- Gids: samenvatting (max 2 regels) + Read more-pagina, korte regel + "More about"-uitklap, ▶/▼ uit MC-font.
- Getest: 4 UI-runs (groot + 854x480), join-test, `./gradlew build` groen.
- Open: bugs #59-#63 (high) + ideeën: nieuwe ja nodig. Voorstel: `character/client/` (17 bestanden) opdelen in
  `guide/` en `panel/`. `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM*.md` (andere sessie) niet meegecommit.

## Thor-ontwerp (andere sessie, 2026-10-02)
- Ontwerp `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM.md` + `_SHORT.md` (niet gecommit, van die sessie). Besloten:
  Throw and Follow = getimede dash, gewone worp max 24. Open: 10 beslissingen in §9; daarna fase 0 bouwen.

