# Laatste sessie

- Datum: 2026-10-11 19:40
- Nieuwe vraag: Rustbound laat item vallen (niet kapot); Wizard = echte boss (3 fases, 8 extra spreuken, 16 spiegelbeelden, eigen boss bar, betere animaties, geen spam, decoy herkenbaar).
- Gedaan: server (WizardMove/Moves/Casts/Mirrors, spell/, field/), client (WizardKeys/Poses/Model, StaffLayer, WizardRenderer, WizardBar, fx/, field/), data + en_us, CHANGELOG, tour-tekst, CLAUDE.md wizard-regel. Build + tests groen.
- Getest in game (losse projectkopie, eigen `run/`): Rustbound-zwaard valt als item (onbreekbaar, gloeit); 3 fases + boss bar; spreuken 1-16; spiegelcirkel + echte raken → STUNNED; staf-slag, blink-ghost doorzichtig. Fix: na UNBIND komt spiegelcirkel eerst, Time Warp vanaf afstand < 10.
- Testklasse + kopie opgeruimd. Open: vuurbal/meteoor zet gras in brand (zoals Ghast); commit/push/release-ja.
- Hieronder: stand van 18:33.
- Vraag: black hole (Giant Hands) veel cinematischer; nieuwe vijand Stalker (verstoppen, vermommen als dier, teleport, toeslaan); miniboss Wizard met staf en 5 spreuken.
- Black hole: eigen shader `engine/client/fx/Singularity` (licht buigt, schijf, jets, aanzuigen, schokgolf), `HandHoleLight`, grond-stof naar binnen, langer, instabiel voor instorting; geen camera-overname.
- Stalker (`creature/stalker/`): boom/bosje verstoppen als bekeken, sluipen van dekking naar dekking, vermomming als dier in de buurt, sprong + klauwen + bloeding, blink weg. Spawnt in bossen.
- Wizard (`creature/wizard/`): miniboss met bossbar, alleen ei; arcane bolt (zoekt), vuurbal, sterren-rift (Cosmos) met vallende sterren, blizzard (bevriest + witte mist), blink met decoy. XP + loot.
- Getest in game: alle poses/spreuken, blizzard-mist, Stalker-gedrag, tour 0.8.7 (3 kaarten) gelopen; build + tests groen.
- Niet getest: echte black hole-cast via Giant Hands-toets (alleen client-tekening getest; server = stof + zelfde timings).
- Versie 0.8.7-alpha (prepare), CHANGELOG + TourSteps. Testklassen, werelden, shots opgeruimd; tour.json teruggezet.
- Niet van mij, niet meecommitten: `arena/`, `CONTENT.txt`.
- Open: commit/push/release-ja van gebruiker; golem-details van gebruiker; idee #24.
