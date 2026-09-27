# Laatste sessie

- Datum: 2026-09-27 (avond).
- Vraag: developer-sectie in het power wheel met botten-weergave (alle wezens, jezelf, handen, mech), DreamWorks-tab
  (Megamind, General Kai, Po, Jack Frost, Merlin), ragdolls die op explosies/gewicht reageren; antwoorden: altijd
  raken (zelf kiezen), tentakel wikkelen ja, mech-voet geen schade en geen schokgolfjes, as-dood mooier; daarna
  commit, push en release.
- Gedaan: `BoneView` (host/operator), DreamWorks in `Roster`, `Ragdolls.blast` + `ClientPacketListenerMixin`,
  ragdoll-zwaartekracht -32, `TentacleWrap`, mech-stap-ringen weg, `Ashes` herbouwd met `ShadedBuffers`, Giant Hands
  volgen hun doel tijdens de slag (`GiantHandBase`). Release v0.3.6-alpha.
- Getest: `gradlew build` groen (68 unit tests); in-game: menu, botten, wikkel, TNT (lijken 0,000 diep na landing),
  as-reeks, Giant Hands 3/3 raak; mech-wijziging alleen gecompileerd (alleen effecten weggehaald).
- Open: bugs #30 (hoog), #28, #27, #29; ideeën #24/#25 wachten op ja/nee; zie §1.1 "Nog open" in
  `docs/claude/vervolg-2026-09-27.md`.
