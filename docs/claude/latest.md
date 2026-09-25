# Laatste sessie — 2026-09-25 (avond)

- **Vraag:** (1) Giant Hands bijl-paar: vingers van de 2 handen glitchen door elkaar. (2) Uitrust-animatie Sword & Shield vloeiender, zwaard op de voorkant van het schild, schild blijft naar voren. (3) Release. (4) Audio en video (o.a. uit docs) voortaan meecommitten, opnieuw committen en pushen.
- **Handen:** keys in `HandDuoScript` en `HandDuo.grip`; offline botsingscheck en in-game close-up schoon.
- **Zwaard:** `SwordEquip`: schild "braced" (gezicht naar voren), zwaard zwaait rechts schuin omhoog uit en slaat 2× vlak over de bovenband van de voorkant, vloeiende bogen; derde persoon: lichaam draait schild-kant naar voren. Getest offline, in-game en `gradlew build`.
- **Media:** ignore-regel `docs/reference/*.mp4` weg uit `.gitignore`; regel in CLAUDE.md (Repository): audio en video worden altijd meegecommit (GitHub weigert bestanden > 100 MB). `multiverse-theme.ogg` en `nanotech-ironman-suitup.mp4` in de repo (metadata gecheckt: niets persoonlijks). Gebruiker koos bewust: repo blijft openbaar, ook met de filmclip.
- **Git:** releases `v0.0.2-alpha` (handen + zwaard) en `v0.0.3-alpha` (media); elke push = release.
- **Open:** oude testwereld `run/saves/claude_split_test` mag weg? Blokkeren tijdens uitrusten uit van t46 tot t71.
