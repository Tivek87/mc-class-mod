# Laatste sessie

## 0.7.5-alpha (2026-10-04)
- Verzoek: tag alleen waar nodig, tour korter en alleen grote wijzigingen, tour direct na update, nieuwe config-UI.
- Tags alleen in versielijst, statuskaart, popup en serverversie; elders `v0.7.5` zonder einde (`Stage`).
- Instellingen: `SettingsScreen` op `NavScreen` (pagina's, zoeken, schakelaar = 1 knop, Defaults/Undo/Save,
  sluiten bewaart); `ConfigChoiceScreen` weg; tourplek `Place.SETTINGS`.
- Tour: `TitleScreenMixin` + `Tour.underOverlay` (vraag al tijdens uitfaden laadscherm), vraag-pil in game,
  `TourSteps.SMALL`, woordlimiet in `TourCoverageTest`; regel in CLAUDE.md.
- Review-fix: `NavScreen.typing()` kijkt ook in lijstrijen (cijfer in getalveld wisselde pagina).
- Getest: build groen; 3 game-runs (menu + wereld, klein/groot, opslaan/sluiten, /stamina, popup, mismatch).
- Daarna: Thor-abilities storm + bliksembom. Open: bugs #59 #60 #61 #63 (high), #62 verzoek, ideeën #24 #25 #31-#33.
- `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM*.md` (andere sessie) niet meecommitten.

