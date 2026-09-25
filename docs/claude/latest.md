# Laatste sessie — 2026-09-25 (avond, 10)

- **Vraag:** Air Strike (The Plane): crash-explosie 5x groter, miniguns 65% sneller, kogels moeten echt schade doen. Keuze gebruiker: beeld + schadebereik 5x, krater blijft 8.
- **Oorzaak kogels:** raak-check bij aankomst eindigde precies op de rand van de mob (`AABB.clip` telt het eindpunt niet mee) + lopende mobs waren al weg.
- **Gedaan:** `AirStrike.CRASH_SIZE` 5 (beeld `PlaneCrash`, deeltjes `AirStrikeBlasts.crash`, schudbereik `TrackedConstructs`); `crashRadius` 16 → 80 (max 200), `gunTicks` 4.6 → 2.79, `.was(...)` + `DEFAULTS_VERSION` 15; `AirStrikeGuns`: voorhouden (`ahead`, niet bij teleport), check loopt 2 blokken door het lichaam.
- **Docs:** `POWERS.md`, `GREEN_LANTERN.md`, `CHANGELOG.md` (0.1.1-alpha), `gradle.properties` (prepare gedaan).
- **Getest:** 2 in-game runs (oud vs nieuw). Kogels 168 → 278 (+65%); gun-treffers 2 → 31 (31 van 33 raak bij afvuren); crash raakt probes op 30/60/75, niet 85. Beelden: explosie ~5x groter. Build schoon; testklasse/wereld/shots weg.
- **Open:** commit + push + `release.ps1 publish` wacht op ja van gebruiker. Geen open bugs.
