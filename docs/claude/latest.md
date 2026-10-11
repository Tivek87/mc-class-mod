# Laatste sessie

- Datum: 2026-10-11 10:09
- Vraag (batch 0.8.4, deel 2): Abyss langer/25 schade, Commands/Mobs rustiger, 4 nieuwe handen, Sentinels sterker, /lightshow, Zombie Brute, lamp-instellingen per soort, import/export van wereld-instellingen, Schematic Wand, bugs #73 en #74.
- Gedaan: alles gebouwd en in game getest. Schematic Wand: `schematic/` (formaten .schem v1-3, .litematic, .nbt, oude .schematic; hologram 1 op 1; plaatsen, undo, stoppen; wachten op niet-geladen chunks met popup; importeren/downloaden/mappen; paneel + gids (P); Schematics-instellingenpagina).
- Getest: 5 unit tests (`SchematicMathTest`), alle 43 testsuites groen. In game: 132/132 blokken goed na draaien+spiegelen, kist-inhoud en armor stand mee, undo zet alles terug, grote stad (160x48x160) zonder lag (ergste frame 17 ms), oude MCEdit-kist via datafixer goed.
- Review-fixes: undo blijft in eigen dimensie, Stop werkt ook op undo, import leest buiten game-thread, HUD-tekst breekt af, kapot bestand toont geen oude gegevens.
- Bugs #73 (klimsnelheid 8 b/s, nieuwe setting) en #74 (benen bij sneaken) gesloten.
- CHANGELOG 0.8.4 en tour (13 stappen) bijgewerkt; tour in game gelopen, alle kaarten ok; tour.json teruggezet.
- Niet van mij, niet meecommitten: `arena/`, `CONTENT.txt`.
- Opgeruimd: testklasse `uitest/`, wereld claude_ui, ui_*-shots, `run/schematics`, `run/world_settings`. Volledige build + `release.ps1 check` groen.
- Open: idee #24 ja/nee; commit + push + release 0.8.4-alpha wacht op ja.
