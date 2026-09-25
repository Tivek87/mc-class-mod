# Laatste sessie — 2026-09-26 (nacht)

- **Vraag:** Plasma Flamethrower (construct wheel slot 16) helemaal maken: eigen opzet-animatie, linksklik, rechtsklik, 2-sec-hold; alles hoge kwaliteit geanimeerd.
- **Keuzes gebruiker:** linksklik = vlammenzwaai, rechtsklik = vlammenmuur, links 2 s = inferno-straal, rechts 2 s = vlammenwervel, groen nabranden (geen blokbrand).
- **Server:** `FlameMove` (tijdlijnen), `Flamethrower` (vasthouden, knoppen, power, bescherming wervel), `FlameHits` (schade zwaai/straal/wervel, schoten verbranden), `FlameWall` (muur 3 s, duwt vijanden terug), `FlameBurn` (groen nabranden 3 s). Payloads FLAME, FLAME_WALL, BURN. Instellingen onder construct_wheel (groepen flame_*).
- **Client:** `Flame*` in `client/body` (houdingen via nieuwe `engine/math/Keyframes`, eerste/derde persoon, camera, geluid), `FlamePainter` (wapen groeit stuk voor stuk), `FirePainter` + `FireStream` (vlammen, muur, wervel, nabrand), `FlameSound`. Engine: `PainterLight` kreeg glowTaper/lightTaper/glowDisc/lightDisc. `SwordSpot` hernoemd naar `HandSpot` (gedeeld). Model kreeg voorgreep + klepwiel.
- **Getest in-game (4 rondes, screenshots):** opzetten, zwaai, inferno, muur, wervel, uitbarsting, breken, snel opnieuw pakken, zwaard -> vlammenwerper; eerste + derde persoon; schade klopt (4 / 3 per 4 ticks / muur 4,5 + 3 / wervel 3 / nabrand 2 per s). `gradlew build` OK.
- **Review (6 punten, zelf nagelopen en gefixt):** eigen servertrack volgen bij snel opnieuw pakken, hand-tekenaars zwaard/vlam tekenen samen bij wissel, nabrand = plasma (ook op vuurbestendige wezens), schoten alleen van vijanden en alleen vliegend verbranden (drietand afremmen), wervelhoogte schoten, trage verbinding kapt straal niet af, muur-power terug zonder grond.
- **Docs:** POWERS.md, GREEN_LANTERN.md, CHANGELOG (0.1.7-alpha).
- **Vervolgvraag:** "jetpack gedaan?" -> nee: idee #9 is niet gebouwd (spelersidee, wacht op ja van gebruiker).
- **Release:** op ja van de gebruiker: `release.ps1 prepare` (0.1.6 -> 0.1.7-alpha), commit, push, `release.ps1 publish` (v0.1.7-alpha).
- **Open:** `docs/reference/green-lantern-spawn-speech.ogg` staat nog los (niet van deze taak, niet meegecommit). Suggestie: zelfde snel-opnieuw-pakken-fout bestaat in `SwordArms`. Ideeën #9 en #10 wachten op ja/nee.
