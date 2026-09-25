# Laatste sessie — 2026-09-25 (avond, 11)

- **Vraag:** naast "Report a bug" ook ideeën/suggesties van spelers. Keuzes gebruiker: eigen knop, categorie + prioriteit, Claude deployt de relay.
- **Gedaan:** `BugReportScreen.bug/idea` (één scherm, `BugReporter.Kind` + `Category`), knop "Suggest an idea" in `UpdateManagerScreen` (What's new eigen rij, paneel 180 hoog), teksten in `en_us.json`.
- **Relay:** `worker.js` kent `kind: idea` + `category`; labels `idea`, `category: …`, `priority: …`; oude versies (geen kind) blijven bug, bug-tekst byte-gelijk. Gedeployd (versie df0c47ae), labels op GitHub gemaakt.
- **Script:** `bugs.ps1` sync ook naar `bugs/ideas/`, `fixed` werkt voor ideeën ("Added in v…"), nieuw `decline` (sluit als not planned). Bug gevonden+gefixt: `$bugs` overschreef `$Bugs` (PowerShell hoofdletterongevoelig) → log in map `0/`, map weg.
- **Docs:** CLAUDE.md (Bug reports + layout), README, docs/PROJECT.md, CHANGELOG 0.1.3-alpha (prepare gedaan).
- **Getest:** relay lokaal (8 gevallen, fake GitHub) goed; in-game op 427x240: beide schermen, verzenden idee + bug naar lokale relay, labels/tekst goed; live test-idee #6 → sync → fixed → decline (gesloten, not planned). `gradlew build` schoon. Testklasse/shots weg.
- **Open:** commit + push + `release.ps1 publish` wachten op ja van gebruiker.
