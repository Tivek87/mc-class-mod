# Laatste sessie — 2026-09-27

- **Vraag:** (1) Giant Fist vervangen door The Emerald Express (stoomtrein), (2) vliegen 150% trager (gekozen: snelheid ÷ 2,5), (3) Air Strike 2 sec vasthouden = Hard-Light Mech Assembly (volgens `docs/reference/mech-robot-ultimate.mp4`).
- **Keuzes gebruiker:** mech verlaten = Y opnieuw 2 sec; trein 12 s / 6 power; mech eigen cooldown 90 s / 25 power; geen blokken kapot.
- **Trein:** server `EmeraldExpress` + `ExpressRoute` (portaal, jagen op rode wezens, rammen, remmen, kantelen, schuiven, stoom, ketelexplosie); gedeeld `ExpressScript`; client `ExpressTrails`, `ExpressPainter/Shapes/Rails/Light`. Giant Fist-code, pending-kosten op de ringbalk en fist-teksten verwijderd.
- **Mech:** server `MechAssembly` (houden, tijdlijn, klap- en boorschade, uitstappen), gedeeld `MechScript`; client `MechPainter/Shapes/Light`, `client/body/MechPilot` (zithouding, hendels, first-person armen, speler vastzetten). Andere krachten en het wiel geblokkeerd tijdens de mech.
- **Vliegen:** top 1,8 / start 1,2 / kruis 1,5 b/s; land- en duikdrempels mee geschaald; ram-schade per snelheid ×2,5 (zelfde klap op topsnelheid). Bug #26 gefixt (`bugs/fixed/`).
- **Geluid:** eigen .ogg-bestanden (gesynthetiseerd) in `sounds/express` en `sounds/mech`, helper `engine/fx/Sounds`.
- **Getest in-game** (5 runs, screenshots bekeken): trein, rammen, ontsporen, schuiven (~11 blokken na kantelen), stoom, explosie, mech-opbouw, cockpit, uitstappen, vliegsnelheden gemeten, tik-Y = Air Strike, trein en constructwiel geweigerd in mech. Testbestanden weg, `gradlew build` OK.
- **Docs:** POWERS, GREEN_LANTERN, PROJECT, README, CHANGELOG `[0.3.1-alpha]`, CLAUDE.md (ontwerpregels: trein i.p.v. vuist, cockpitraam mag doorzichtig; testregel: statief zonder `Marker:1b`).
- **Release:** ja gekregen ("commit en push") → commit, push en release v0.3.1-alpha (sluit bug #26).
- **Open:** ideeën #24 (NPC-companions) en #25 (voicelijn luider) wachten op ja/nee; `GameCharacter.java` 598 regels (bijna splitsen); de eigen wissel `.ogg` → `.mp3` in `docs/reference/` bewust niet mee-gecommit.
