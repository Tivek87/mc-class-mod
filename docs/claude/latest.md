# Laatste sessie — 2026-09-25 (avond, 13)

- **Vraag:** vliegen 50% trager, na 4 sec volle snelheid; commit + push.
- **Gedaan:** `topSpeed` 18 → 9, `startSpeed` 12 → 6, `cruiseSpeed` 15 → 7.5, `speedUpSeconds` 7 → 3 (volle snelheid voelbaar na 4 s: snelheid bouwt pas op ~0,5 s na vertrek), `.was` + `DEFAULTS_VERSION` 18; fallbacks in `FlightSteering`; duik `DIVE_SPEED` 19.25 → 9.625. Landing-slam-drempel volgt topsnelheid vanzelf. Ram-uitleg 0.45.
- **Docs:** POWERS.md, GREEN_LANTERN.md (ook charge 16 → 12 daar nog gemist), CHANGELOG 0.1.6-alpha.
- **Getest in-game:** vlucht op vlakke wereld x 30000: 6,1 → 7,4 (1 s) → 8,98 (4 s) → 9,00 b/s stabiel. Build schoon, testklasse/wereld weg.
- **Open idee:** #9 (medium) jetpack van hard licht bij versnellen in de vlucht: ja of nee van gebruiker nodig.
- **Open vragen:** charge −25% goed? crash-schade (24 harten) en crash-geluid ongewijzigd: ook kleiner? Ram-schade daalt mee met de lagere vliegsnelheid.
