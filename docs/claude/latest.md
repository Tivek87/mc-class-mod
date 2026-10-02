# Laatste sessie

- Datum: 2026-10-02. Verzoek: botten afmaken (animaties en ragdolls gebruiken alle botten, regel erbij) en
  Enderman-benen die altijd gespreid stonden. Bugs/ideeën bewust NIET aangeraakt (opdracht gebruiker).
- Enderman: oorzaak `FootPlanting` (benen 1 px onder de vloer gebouwd -> been steeds tot 0.8 rad weggedraaid). Nu
  vanaf de zool zoals het model gebouwd is (`FootPlanting.sole`); door een pose gebogen benen niet meer geplant.
  Voor/na in-game bevestigd (oude voetpunten via reflectie in een testrun).
- `Stance`: enkels (voet houdt zijn stand, zool plat), schouderbladen (`Shoulders`), bekken (60% van de lean),
  polsen (`arm(..., palm, weight)`, klap: handpalm op handpalm), `flat()`. `Poses` draait schouderbladen mee voor
  alle andere houdingen. Knielen/hurken (slam, vlammenwerper, zweep) via echte knieën/enkels; `KneelLegs` weg.
  Vlucht: knieën licht gebogen, voeten gestrekt. Slachtoffers: oren (ellebogen), marionet, gespreid, uitgerekt.
- `Limbs`: rechtzetten bij 0, `shrugged`, `bent(part)`, `most`. CLAUDE.md-regels aangescherpt. Versie 0.5.5-alpha.
- Tests: build groen (129 tests; `StanceTest` uitgebreid, `FootPlantingTest` nieuw); 3 in-game runs met shots,
  testklasse, wereld en shots daarna verwijderd.
- Open: zwaard-uitval (`SwordSeen` step) nog stijve benen (vraagt ontwerpkeuze). Commit/push/release wacht op ja.
