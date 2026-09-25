# Laatste sessie — 2026-09-25 (avond)

- **Vraag:** (1) Giant Hands bijl-paar: vingers van de 2 handen glitchen door elkaar. (2) Uitrust-animatie Sword & Shield vloeiender, zwaard slaat op de voorkant van het schild; daarna: schild niet omhoog, blijft naar voren staan. Daarna: release.
- **Handen:** keys in `HandDuoScript` (posities, vingers, duim) en `HandDuo.grip` (vuist blijft even los van de steel, zakt en schuift eraf bij loslaten). Offline botsingscheck: nergens overlap; in-game close-up van grijpen en loslaten: schoon.
- **Zwaard:** `SwordEquip` omgebouwd: schild "braced" (vóór je, gezicht naar voren, ~7° naar het zwaard), zwaard zwaait rechts schuin omhoog uit en slaat 2× met de snede vlak over de bovenband van de voorkant; tussen-keys met juiste draaisnelheid (geen schokken), terug naar guard in één zwaai. Derde persoon: lichaam draait schild-kant naar voren, zodat de kling vlak op de voorkant ligt. `SwordPainter.faceOut` (hoogte voorkant), vonken op de voorkant.
- **Getest:** offline clearance (raakt precies, nergens erdoor, beide aanzichten) en snelheden; in-game shots eerste persoon + zijaanzicht OK; `gradlew build` groen.
- **Git:** commit "Release-naam zonder titel" (CLAUDE.md, release.ps1, CHANGELOG-kop) en commit "Handen zonder botsing, zwaardslag op schildvoorkant"; release `v0.0.2-alpha` (prepare, CHANGELOG-sectie, push, publish).
- **Open:** `docs/reference/multiverse-theme.ogg` ongetrackt (niet van Claude): in `.gitignore` zoals de mp4-clips? Oude testwereld `run/saves/claude_split_test` mag weg? Blokkeren tijdens uitrusten nu uit van t46 tot t71.
