# Laatste sessie

- Datum: 2026-10-13 19:24
- Verzoek: facties met lore (15 volken), factie-naam boven elk wezen/speler, live sliders, logische standaarden (undead/monsters vijandig), "game"-houding weg, Faction Book-item, alle gemelde bugs fixen.
- Gedaan: `FactionDefaults` herschreven (naam, beschrijving, lore per factie), `FactionUpgrade` zet oude werelden eenmalig om, `FactionTags` (naam boven hoofden, client setting `factionTags`), `FactionBookItem` + `client/book/FactionBookScreen` (recept: book + ink sac + gold nugget), pending-waarden in `FactionsState` (sliders direct bijgewerkt), spelers in Members/boek met hun skin.
- Bugs: #83 golem valt Covenant-speler aan, #77 beam-dial 55% zonder linkertekst (`BeamGauge`, `ArcGauge`), #76 minigun first person: geen armblok, kogels uit de loop (`HeavyArms`). Alle drie gesloten met `bugs.ps1 fixed`.
- Versie 0.9.12-alpha: `CHANGELOG.md`, 3 tour-stappen (`TourSteps`), `docs/PROJECT.md`.
- Getest in het spel: tags, boek, golem, dial, minigun, sliders, tour-wandeling. `./gradlew build` groen.
- Niet getest: `FactionUpgrade` op een echte oude wereld, echte multiplayer.
- Open: commit + push + `release.ps1 publish` wachten op ja; ideeen #78, #80, #81, #82, #24 alleen na ja.
