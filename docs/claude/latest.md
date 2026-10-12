# Laatste sessie

- Datum: 2026-10-12 22:30
- Verzoek: echt naar de maan vliegen (7000 blokken hoog, ~1 minuut, verborgen wissel van dimensie), rond de maan en de Aarde kunnen vliegen, daarna commit, push en release.
- Maan: eigen wereld `welcomescreen:moon` (`space/MoonGenerator`, `MoonGround`: stof, kraters, keien, zeeën); een vijfde van het gewicht, vallen doet veel minder pijn (`SpaceTravel`).
- Wissel op de helft (3508 boven de grond): wereld omgedraaid om de x-as, vlucht gaat door (`Flight`, `ThorMoves`, `WorldTurn`), geen laadscherm (`SpaceView.Unseen`), beeld rolt een kwartslag aan elke kant.
- Vlucht tot 8x sneller hoog in de lucht, kost niet meer ringkracht (`FlightLanes`, `Space.haste`).
- Baan: vanaf 1000 (Aarde) / 500 (maan) hoog brengt zijwaarts vliegen je om de wereld heen; de hele hemel draait mee (`Space.orbit`, `SkyWay.orbit`). Recht omhoog klimmen telt niet als rondgaan (`Space.climbs`), anders kwam de wissel nooit.
- Hemel onderweg: lucht wordt zwart, land vervaagt in nevel, Aarde-kubus onder, maan groeit boven (`SkyWay`, `SkyCubes`, `CosmicSky`).
- Wereldinstelling `spaceTravel` (Fly to the moon), aan.
- Getest in game (scratchpad-kopie): echte vlucht als Green Lantern, wissel op 3510, landing op de maan na 70 s, zwaartekracht 0,016, maanstof eronder; baan-shots op Aarde en maan; tour van 0.9.6 (2 kaarten). `gradlew build` + TourCoverageTest slagen.
- Release 0.9.6-alpha: `mod_version`, `CHANGELOG.md`, `TourSteps`, `en_us.json`.
- Niet van deze sessie (niet committen): beer/hert/wasbeer van een andere sessie (`creature/bear|deer|raccoon|animal`, `ModItems`, `ragdoll_none`), `CONTENT.txt`, `arena/`, `models/`, `__pycache__/`.
- Volgende: bugs #76 (minigun/arm blaster first person), #77 (dubbele stage HUD); ideeën #78, #24 alleen na ja.
