# Laatste sessie

- Datum: 2026-10-12 16:44
- Verzoek: grote lijst voor 0.9.2 (Doc Ock model/crosshair, Thor hamer terugroepen/wisselen/tuimelen, gids, controls, hemel, Wizard, Galactus).
- Gedaan: alles gebouwd. Wizard: grond/lucht, WATCH, staf (speer, los vechtend, 10 moves), 9 bezigheden + 12 gewoontes, gezicht (ogen/wenkbrauwen/mond), flicker/rift-step/veil, 6 soorten rifts, rifts bij komst en dood, ward +35%, cooldowns, kloon-bereik, netherite-slijtage.
- CHANGELOG 0.9.2 + 3 Wizard tour-stappen, CLAUDE.md layout bijgewerkt. `./gradlew build` groen.
- In game getest (`WizardTest`, scratchpad copy): komst met rifts, bezigheden, gezicht, gevecht (grond/lucht, staf gooien/terugroepen/los), dood met void-rift. Geen errors. Fix: drain blijft 20 s in elke fase.
- Schone worktree-build zonder `arena/` groen.
- Open: user-ja voor commit + push, dan `release.ps1 publish` vanuit worktree (anders komt `arena/` in de jar).
- Niet committen: `CONTENT.txt`, `arena/`, `models/`, `__pycache__/`, `wizard_phases_viewer.html`.
