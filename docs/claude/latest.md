# Laatste sessie — 2026-09-26 (avond, deel 3)

- **Vraag:** construct-balk boven hotbar weg; whip-fysica (teleport/rekken) fixen; whip 10 blokken; zweep tussen hand en ring zichtbaar bij wervelwind. User: "commit dit zo wel maar push nog niks".
- **Gedaan:** `ConstructHud.renderBar` weg. `whipLength` 10 (`.was(4.5)`, max 16, `DEFAULTS_VERSION` 20), `SPIN_REACH` 0.18, lasso zonder rek. Nieuw `client/body/WhipRope`: touw met vaste stuklengtes, volgt de vorm (`WhipLine.shape` + stevigheid), laat los waar de vorm steil de grond in duikt, ligt plat op de grond, terughalen max 3 blok/tick. `WhipLash`: curl = deel dat vanaf de greep opgerold is (geen zwiepende spiraal meer). Wervelwind: gloed + streep vanaf de hand.
- **Getest in-game (9 runs, testklasse weer weg):** run 9: 0 sprongen, 0 rek, lengte altijd 10; snelle punt alleen nog bij begin slag; rust-rol, slagen, beenslag, wervelwind, lasso en uitrusten op screenshots goed. Na run 9 alleen kleine refactor (`at`/`shape` split): compileert, niet opnieuw in-game gedraaid. `gradlew build` OK.
- **Docs:** `POWERS.md`, `GREEN_LANTERN.md` (rol, uitrollen, neervallen, binnenhalen), `CHANGELOG` 0.2.9-alpha (`release.ps1 prepare` gedaan).
- **Git:** lokaal gecommit, NIET gepusht (user). Na push: `scripts/release.ps1 publish`.
- **Open:** push + publish na ja; bug #18 (welke abilities?); #17 negeren; stash "thunder clap + ability 12" staat nog.
