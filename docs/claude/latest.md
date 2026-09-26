# Laatste sessie — 2026-09-26 (nacht, deel 2)

- **Vraag:** (1) spawn-animatie exact 2,0 s trager + speech (`docs/reference/green-lantern-spawn-speech.ogg`) met 0,7 s fade in/uit; (2) jetpack bij snel vliegen (idee #9); (3) 12 vloeiende linksklik-aanvallen voor de Plasma Flamethrower.
- **Keuzes gebruiker:** hele spawn gelijkmatig trager; jetpack blijft tot landen/stoppen; 12 = linksklik-aanvallen zoals het zwaard.
- **Spawn:** één klok `Arrival.moment()` (159 → 199 ticks, ook de oplaad aan het eind via `Recharge` made/played + `ClientRing.recharge`). `ArrivalSpeech` (stereo ogg met ingebakken fades in `sounds/ring/`, VOICE, zachter op afstand, 0,7 s fade bij afbreken).
- **Jetpack:** `Jetpacks` (client-staat uit `FlightPose.lying`), `JetpackPainter` (bal uit ring, groeien, vlammen, breken), `BackSpot` (rug-frame uit de pak-laag), tekenen in `ClientConstructs`.
- **Flamethrower:** `FlameMove` kreeg 12 aanvallen met strokes/paden (server + client delen `way()`), `FlameHits.spray` (ovaal rond het pad), per-stroke geluid/kick; `FlameKeys.attacks()` + pad-override in `FlamePoses.along`; `orbit` in `Pose` voor het rondje (derde persoon `turnModel`, eerste persoon `orbited`); `ScreenSpot` achter-de-camera-fallback.
- **Getest in-game (5 rondes):** spawn 199 ticks, alle geluiden op juiste momenten, speech 10,4 s + fade bij afbreken; jetpack vormt/vlamt/breekt; alle 12 aanvallen raken goed (volgorde, schade ×power, rondje 6/6, kruis 2×), ketting vloeiend, eerste persoon ok. Run 3 verstoord door Alt+Tab (Giant Hands). `gradlew build` OK.
- **Docs:** POWERS.md, GREEN_LANTERN.md, CHANGELOG (0.1.8-alpha), en_us (hint + subtitle), instellingteksten. Regel toegevoegd (CLAUDE.md, Alt+Tab in tests).
- **Open:** commit + push + release wachten op ja. Idee #9 was al gesloten op GitHub (completed, zonder commentaar). Ideeën #10 en #11 wachten op ja/nee. Multiplayer niet apart getest (zelfde codepad als derde persoon).
