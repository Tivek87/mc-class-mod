# Laatste sessie — 2026-09-25 (avond, 4)

- **Eerder vandaag:** in-game updater, release v0.0.6-alpha.
- **Vraag:** comments weg (alleen kort waar echt nodig) + regel in CLAUDE.md; changelog-regel (kort, alles benoemd); vliegen: optrekken 75% sneller, top -35%, top na 7.5 s, niet meer vallen bij een hit; beam/bolt bruikbaar zolang 1 hand vrij is.
- **Comments:** script strips alles (bytecode identiek, javap over 416 classes), 13 agents zetten ~200 korte comments terug; ook build.gradle, gradle.properties, release.ps1, shader, toml, cfg, build.yml.
- **Vliegen:** `topSpeed` 6.25625, `startSpeed` 4.16, `cruiseSpeed` 5.2, `speedUpSeconds` 7.0 (+ `.was`, `DEFAULTS_VERSION` 14), `SPEED_UP` 0.20825. Hit-bug: serversnelheid van een vlieger stapelde zwaartekracht op (y -3.6/tick), een hit stuurde die naar de client -> neerstorten + slam. Fix: `Flight.tick` zet de serverbeweging gelijk aan de echte vlucht (behalve als er een knock wacht).
- **Beam/bolt:** `LightBeam.handsFull` (server) + `ClientCharacter.handBusy` (client): alleen geblokkeerd als beide handen bezig zijn (opladen, opstijgen, vuist/zwaai/call + schild/dome). Giant Fist/Hands/Air Strike/Bubble stoppen de beam niet meer.
- **Getest (in-game, 4 runs):** snelheid 4.25 -> 5.2 -> 6.2 op 7.5 s -> 6.256; hit met en zonder aanvaller: doorvliegen; beam met vuist ja, schild+vuist nee, schild ja (grond en lucht). Testklasse/wereld/shots verwijderd.
- **Docs:** CLAUDE.md (Comments + changelog-regel), docs/POWERS.md, CHANGELOG 0.0.7-alpha, versie 0.0.7-alpha (`release.ps1 prepare` gedaan).
- **Git:** gecommit, gepusht en gepubliceerd als release v0.0.7-alpha. Comment-agent voor de laatste 13 bestanden op verzoek gestopt (wat hij al had gezet blijft). Eerder open: blokkeren tijdens uitrusten Sword & Shield t46-t71.
