# Laatste sessie

## 0.7.6-alpha (2026-10-04)
- Verzoek: settings splitsen in Client/Server, alleen host/owner opent server-pagina's, kleur per character,
  update-check minimaal 5 min, Open all/Close all per pagina; daarna commit + push + release.
- `engine/client/gui/NavScreen`: `Item` met `heading`, `color`, dicht item (`open` null + `hint`), `accent()`,
  `subtitleRoom()`, `smallButton()`; `PixelIcons.LOCK`.
- `config/client/SettingsPages.serverOpen()`: server-pagina's alleen in wereld voor host/owner; anders regel
  "Host only". Kleuren: client violet, regels oranje, Doc Ock grijs, Green Lantern groen, Thor blauw.
- `SettingsScreen`: koppen Client/Server, Open all/Close all, titel en accent in paginakleur.
- `ClientSettings.UPDATE_CHECK` 5..120 min; tourstap `settings_split`; CHANGELOG, POWERS.md, PROJECT.md.
- Eerder deze sessie: Thor Thunderstorm + Lightning Bomb, bugs #59 #60 #61 gefixt (staan in 0.7.6).
- Getest in game: menu, eigen wereld, tourkaart, dedicated server (op zonder owner ziet alleen "Host only",
  server weigert edit; owner in bestand -> pagina's zichtbaar). Build groen (173 tests). Testcode weg.
- Open: #62 is verzoek (ja nodig); #63 niet te reproduceren; ideeën #24 #25 #31-#33 wachten op ja.
- `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM*.md` (andere sessie) niet meecommitten.
