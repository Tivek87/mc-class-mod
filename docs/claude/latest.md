# Laatste sessie

- Datum: 2026-10-12 00:34
- Release 0.8.9-alpha: commit + push + publish op verzoek; tour in game doorlopen (7 kaarten ok), CHANGELOG-datum 2026-10-12.
- Laatste vragen: korte lijst van alles wat nieuw is (gegeven); waar het ability-paneel aan te passen (Mods → Multiverse Powers → Config → Your game → Ability overlay → Change layout); Wizard in de Mobs-preview met staf op de grond (gedaan: `WizardPoses.planted`, alleen in preview via `MobPreview.drawing`, in game getest, build groen).
- Vraag: Wizard-ward 65% zwakker, daarna de lijst: Mobs-pagina (tooltip, draaien, typen/vermenigvuldigen, relaties klikken, Sentinel), paneel verplaatsen, GL hover-snelheid, black hole (echte blokken, burst), Wizard sterker + meerdere spelers, `/galactus`, escape-minigame, Doc Ock weg (WIP), idle-animaties in de mob-viewer.
- Commit `cb2fc36` gedaan (Wizard), niet gepusht/gereleased.
- Gedaan (code, build + unit tests groen): alles uit de lijst. Doc Ock: `docock/` weg op `OctoCrosshair` na, `GameCharacter.DOC_OCK` zonder abilities en `playable()` false, G-menu + server zeggen WIP, geen config/settings-pagina, docs ingekort. Idle: `MobPreview` acts (eigen moves + rondkijken), vloeiende partialTick.
- CHANGELOG 0.8.9-alpha + 3 tour-stappen (galactus, black_hole, doc_ock_rework) + SMALL-regels; TourCoverageTest groen.
- Paneel verplaatsen/schalen bestond al: Settings > Your game > Change layout.
- In-game getest (5 runs, tijdelijke `ClaudeAllTest`, nu verwijderd met wereld + shots): idle-moves alle 12 mobs, Sentinel-preview, typen/*2/÷2, Relations-naam klikt door, Galactus (stip -> por -> weg), zwart gat (echte blokken, burst), Doc Ock WIP, escape-UI, hover-snelheid, Wizard meerdere spelers.
- Fixes uit test: Galactus-arm 's nachts verlicht + gloed op vingertop; Relations-knoppen openen pas op het label (anders bedekten ze de naam).
- `./gradlew build` groen (incl. TourCoverageTest).
- Niet van mij, niet meecommitten: `arena/`, `CONTENT.txt`.
- Open: commit vragen (geen push/release). Niet getest: echte multiplayer-verbinding.
