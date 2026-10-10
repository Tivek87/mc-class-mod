# Laatste sessie

## 0.8.0-alpha (2026-10-10 18:00, commit, niet gepusht)
- Gevraagd: GL-rework (vuisten links, bolt rechts, schild weg, koepel op scan, ramkegel op Express), strijdbijl,
  kettingzaag, mech-combo, escape-spel, wilde helpers, Thor C-dalen; gauntlet-knip met dunne ring + 2 zwakkere ringen;
  lantaarn 1-op-1 als referentie; recharge: ring tegen embleem, geleidelijk laden. Daarna tour + alles committen.
- Gedaan: alles hierboven; CHANGELOG 0.8.0 + tour (12 stappen); docs (GREEN_LANTERN, POWERS, KEYBINDS) en gids-teksten.
- Review-fixes: ramkegel in vlucht niet meer geweigerd na Express; koepel zakken kan tijdens scan-cooldown
  (`CharacterPowers.endsOnly`); handschoenen breken af als een wapen vormt (`LightFists.putAway`); oude client-views
  opgeruimd; ramkegel −70% schade in CHANGELOG.
- In game getest: gauntlet + ringen, vuisten, helpers, escape, recharge (1e/3e persoon), koepel/wapen-fixes.
  Niet getest: lantaarn van dichtbij in de arrival; tour niet in game gelopen.
- `./gradlew build` groen. Testklassen, testwereld en shots verwijderd.
- Open: dode SHIELD-constructcode (ClientConstructs/TrackedConstructs/LanternArms) opruimen; server checkt
  EscapePayload niet (cheat mogelijk); push + release pas na ja; idee #24 (NPC's) wacht op ja/nee.
