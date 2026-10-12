# Laatste sessie

- Datum: 2026-10-12 19:05
- Verzoek deel 1: Wizard gooit staf vaker / laat hem liggen, staf vecht vaker en langer zelf, alle spreuken komen aan de beurt, pijlen normale schade; dan committen, pushen, releasen (0.9.4). Deel 2 daarna: Doc Ock (12 blokken overal, shift+rechtsklik, echte worp, crosshair) en de nachthemel (Melkweg kleiner en mooier, niets over de maan, zon/maan/planeten 3D, meer sterren, Green Lantern maan-hand en Galactus bijwerken).
- Wizard: grote spreuken gaan in rondes (`WizardPicks.spent`): geen komt terug voor alle andere aan de beurt waren; adempauze korter (`BREATH`).
- Staf: los van de rondes (`nextStaff`): 10-20 s na terugkomst weer gegooid of losgelaten, al vanaf fase 1; gegooide staf blijft 2-8 s steken, dan terug of hij wordt wakker en vecht zelf (`WizardStaff.wake`); zelf vechten tot 45 s (`FREE_LIFE` 900).
- Pijlen: zelfde aandeel op het schild als elke klap (`WizardWard`, geen `ARROW` meer).
- Getest in game (scratchpad-kopie, 9000 ticks): fase 1 elke grote spreuk 5-6 keer, fase 3 en 5 35-36 soorten; staf 63% van de tijd uit de hand (5 worpen, 6 keer zelf vechten); pijl 10 schade = 3.0 van het schild, zwaard ook 3.0.
- Bestanden: `creature/wizard/` (`WizardPicks`, `WizardMoves`, `WizardMove`, `WizardWard`, `staff/WizardStaff`), `TourSteps`, `en_us.json`, `CHANGELOG.md`, `gradle.properties`.
- Niet committen: `CONTENT.txt`, `arena/`, `models/`, `__pycache__/`; losse Doc Ock- en hemel-edits in de boom horen bij deel 2.
- Open bugs: #76 (minigun/arm blaster first person), #77 (dubbele stage HUD); ideeën #78, #24.
