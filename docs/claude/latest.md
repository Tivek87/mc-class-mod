# Laatste sessie — 2026-09-25 (avond, 11)

- **Vraag 1:** ideeën/suggesties naast "Report a bug". Gedaan + getest, commit 105d591, release v0.1.3-alpha.
- **Vraag 2:** windgeluiden van de plane (Air Strike) weg (plane-wind, jets, duik-whoosh; motorbrom blijft). `PlaneSound` + `AirStrike`. Getest in-game via `PlaySoundEvent`-log: geen elytra-geluid meer.
- **Vraag 3:** categorie weg uit Suggest an idea. `BugReporter.Category` weg, prioriteit weer volle breedte, lang-keys weg; `worker.js` negeert categorie (0.1.3-clients sturen hem nog, worden gewoon aangenomen), labels `idea` + `priority: …`; `bugs.ps1` zonder categorie. Relay gedeployd (5b3631cf). Docs: CLAUDE.md, README, PROJECT.md, CHANGELOG 0.1.4-alpha.
- **Getest 3:** relay lokaal (4 gevallen incl. oud 0.1.3-idee met categorie) goed, bug-tekst byte-gelijk; in-game 427x240: één knop (Priority), idee verstuurd met labels idea + priority: high. Build schoon, testklasse/shots weg.
- **Open:** GitHub-labels `category: …` bestaan nog (ongebruikt); weghalen alleen na ja.
