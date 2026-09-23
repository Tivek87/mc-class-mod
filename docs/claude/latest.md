# Laatste sessie (2026-09-23)

**Vraag:** Giant Fist-glitch fixen, alle 32 klap-constructs beter en geanimeerd, alles 35% groter en trager, mooier schild, schrapen met de ramkegel (meer power + schudden), `/constructshockwave` met keuzescherm, overzichtelijker config-scherm; daarna alle docs bijwerken en alles committen en pushen.

**Gedaan:**
- Giant Fist stuurbaar: blijft de hele vlucht onder je crosshair en volgt je blik.
- Alle 32 constructs opnieuw gebouwd met veel meer detail en elk een eigen animatie (SlamHands/SlamDrops/SlamCartoon/SlamStrikes/SlamRisers); standaard 1,35× groter en 1,5× trager (instelbaar: `constructScale`, `slowMotion`).
- Nieuw schild, koepel met stenenpatroon, ramkegel met boorribbels, rond lichtschot, ring aan de vuist.
- Schrapen met de ramkegel: +2 power/s, vonken, geschuur, beeldschudden (3 instellingen).
- `/constructshockwave` (alleen met cheats) en een nieuw instellingenscherm (tabbladen, inklappen, zoeken, uitlegbalk).
- Docs bijgewerkt: GREEN_LANTERN, POWERS, PROJECT, CHANGELOG; CLAUDE.md-regels aangevuld.
- Getest in-game en `gradlew build` geslaagd; gecommit in twee commits en gepusht naar origin/master.

**Open:** niets; niet getest: een echte server zonder cheats, en het geluid.
