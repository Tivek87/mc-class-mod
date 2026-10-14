# Laatste sessie

- Datum: 2026-10-14 12:58
- Verzoek: nieuwe mob flamingo, 1 op 1 naar `flamingo_demo.html` (model, animaties, kleuren), groter, echte mob; daarna release 0.9.18 en demo naar `docs/reference/`.
- Gedaan: `creature/animal/flamingo/` (`Flamingo`, `FlamingoFlight`, `client/FlamingoModel`, `FlamingoPoses`, `FlamingoRenderer`): model doos voor doos uit de demo, Amerikaanse en grote flamingo, kuiken eigen model. Acts: op een poot, filteren, poetsen, vleugelgroet (zwerm doet mee), toeteren, strekken; vliegen na aanloop.
- Textuur: `scripts/models/animals/FlamingoTexture.java`, schilders van de demo, 2x fijn. Kleuren door lineair->sRGB, zoals three.js ze toont (anders te donker).
- Spawn op strand, mangrove en warme kust; zwerm 4-8; fokken met kabeljauw, zalm, kelp; veren 0-2. Eigen geluiden (`animal_calls.mjs`).
- Getest in het spel: stil, lopen, rennen, alle acts, opstijgen en vliegen, waden, zwerm met kuiken; demo headless gerenderd ter vergelijking. `./gradlew build` groen, tour (flamingo + wild_animals) gelopen.
- Release 0.9.18: CHANGELOG, tour-stap `flamingo`, FROM = 0.9.17 (binnen uren na 0.9.17).
- Eerder vandaag: v0.9.14 t/m v0.9.17 gepubliceerd.
- Open punten voor de gebruiker: lijst-regels in formulieren blijven 300 tekens (relay moet anders opnieuw uitgerold), factietekst blijft 300.
