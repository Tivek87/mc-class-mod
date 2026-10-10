# Laatste sessie

**Datum:** 2026-10-10, 22:12

**Vraag:** red dot zonder strepen, mech-straal 1 stage, crosshairs veel levendiger, Mjolnir zwart in muur, bliksem elke 5 s, rechts vasthouden in de lucht hing op 25%, munitie + herladen voor shotgun/RPG; daarna volledige tour, commit, push en release.

**Gedaan:**
- Munitie: shotgun 2 hulzen, RPG 1 raket; leeg = automatisch herladen, volledig geanimeerd (shotgun klapt open, hulzen eruit/erin; RPG nieuwe raket). Zichtbaar onder de crosshair en in het paneel (`ammo x/y`, `reloading`).
- Crosshairs reageren op alles: geraakt, landen, vallen, lage health, wisselen, hurken, geweigerde move, water, vuur, elk ability een eigen kick, health-boog van doelwit; Thor-pijl naar zijn hamer.
- Mjolnir in muur belicht, bliksem elke 5 s, Thor in creative flight rechts vasthouden werkt.
- Changelog-regels + tourstappen (`gun_ammo`, `thor_arc`, fixes, crosshairs); POWERS.md bijgewerkt; 2 testlessen in CLAUDE.md.
- In game getest: herladen (zij-camera), crosshair, Thor-worp, Mjolnir in muur, hele tour (30 kaarten). `./gradlew build` groen.

**Niet getest in game:** mech-straal met 1 ring (kleine voorwaarde-wijziging).

**Open:** idee #24 wacht op ja/nee van de gebruiker.
