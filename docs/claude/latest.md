# Laatste sessie

- Datum: 2026-10-12 17:30
- Tweede ronde Wizard: ogen flikkerden (plat iris/ooglid tekende voor- en achterkant in één vlak, nu alleen voorkant), baard hoort nu bij het hoofd en hangt recht (geen eigen fysica), nieuwe aanval `STAFF_PLANT` (staf in de grond, schok rondom), staf in de grond bij stilstaan, gewoontes rustiger (twirl gaf de enige echte schok), teleport max 10 blokken (`Wizard.TELEPORT_RANGE`, gemeten: 10.00), effecten zachter (dust max 1.0, glow x0.6, rifts niet).
- Open: diepere vanilla-plus restyle van alle effecten (vraag aan user), twirl-fix alleen gecompileerd.
- Verzoek: 0.9.2 releasen (gedaan), daarna Wizard baard/staf/gezicht, Doc Ock crosshair (vierkant, geen woord), tentakels dikker + 12 blokken vast.
- Doc Ock: tentakels altijd exact 12 blokken (`TentacleReach.LENGTH`, boog-solver in `TentacleShapes`), grijp-bereik 12 vast (setting weg), dikker, stalen ringen, grotere klauwen; crosshair = hoekhaken in een vierkant.
- Wizard: baard springt niet meer (per-wizard volgen, teleport-sprongen genegeerd in `WizardPoses`), gezicht levendiger (vaker knipperen, dubbel, één wenkbrauw omhoog, mompelen, chanten), staf vloeiend (geen sprongen tussen modes, terugslag, slingert na).
- CHANGELOG 0.9.3 + tour-stap `doc_ock_reach`, `mod_version` 0.9.3-alpha. `./gradlew build` en `release.ps1 check` groen.
- In game getest (scratchpad copy, `OctoTest7` + `WizardTest2`): bogen 12.00 lang, koe op 15 niet gepakt, varken op 10.5 wel; crosshair goed; gezicht beweegt; geen errors.
- Open: user-ja voor commit + push, dan `release.ps1 publish` vanuit worktree.
- Niet committen: `CONTENT.txt`, `arena/`, `models/`, `__pycache__/`, `wizard_phases_viewer.html`.
