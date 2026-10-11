# Laatste sessie

- Datum: 2026-10-11 13:20
- Vraag: wand-grid (Alt tikken: 1..128), hologram weg na plaatsen, blueprints (doorzichtig neerzetten, tab in K-menu met materialen, betere preview), 3 zombies (Bloater, Lurker, Screamer + ferals), log-fixes (bord-spam 1.21.5, crash UpdateManagerScreen).
- Gedaan: alles gebouwd. Nieuw: `schematic/blueprint/`, `schematic/client/blueprint/`, `BlueprintPage`, `HologramModel`; `creature/` (MovingMonster, MonsterBlows, Bleeding, bloater/, lurker/, screamer/); textures via `scripts/models/zombies/` (Java 25).
- Getest in game: grid snapt goed, hologram weg na plaatsen, blueprint 5410 blokken + materiaallijst, pagina ok, 0 bord-fouten in log; Bloater spuugt/krijst/rent/knalt; Lurker sluipt/springt/dasht/klauwt (bloeding); Screamer screech + 3 ferals, burst ~5 blokken; tour 0.8.5 (7 kaarten) gelopen, tour.json teruggezet.
- Review-fixes: bordtekst met # niet afgekapt, blueprint-bestand niet te spammen, blueprint opnieuw vragen na 20 s, geheugenlek HologramModel, Bloater-boils crash, grid-sprong, preview-kleuren na reload, blueprint-limiet, check buiten hoofdthread.
- Versie 0.8.5-alpha (prepare), CHANGELOG + TourSteps bijgewerkt; build + tests groen.
- Niet van mij, niet meecommitten: `arena/`, `CONTENT.txt`.
- Hertest na review-fixes schoon (0 exceptions); testklasse, wereld claude_zombies en claude_*-shots opgeruimd; laatste build groen.
- Open: commit/push/release v0.8.5-alpha wacht op ja (zonder `arena/`, `CONTENT.txt`). Idee #24 blijft open.
- Open (later): HologramModel-scan bij heel grote blueprints nog op hoofdthread (kort haperen mogelijk).
