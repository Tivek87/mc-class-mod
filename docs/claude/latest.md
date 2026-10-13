# Laatste sessie

- Datum: 2026-10-13 22:24
- Verzoek: takenlijst in twee releases. Release 1 (0.9.14): duidelijke factienamen, alleen uitgestoten bij lage reputatie, skeleton schiet op niets, factiehulp betrouwbaarder, wezens houden hun doelwit (wisselen wel naar wie hen slaat), joinen = alle reputatie kwijt bij de vijanden, mooiere en drukker geanimeerde Elemental Blaze met eigen shader/VFX, tekstlimiet 2500. Release 2 (0.9.15): Blaze minder fel, 20% sterker, meer wind-knockback.
- Facties: 15 nieuwe namen (`FactionDefaults`, `en_us.json`, `docs/PROJECT.md`), oude werelden krijgen ze via `FactionUpgrade` (VERSION 4, `retell`, `liftMembers`). `Reputation.joined`/`left`, `Faction.member`: lid start midden tussen de drempels, waarschuwing 10 boven de drempel, pas uitgestoten op de drempel; vijanden van je factie zetten je op -100.
- Doelwit: `mixin/NearestAttackableTargetGoalMixin` + `MobTargets.inFight` (geen nieuw doelwit zolang het gevecht loopt), `MobTargets.onDamaged` (draait naar wie slaat), `onTick` laat een dood of verdwenen doelwit los (skeleton-bug).
- Hulp: `FactionHelp` luistert nu op de inkomende klap (schild telt), ziet vriend of vijand, en helpt een lid dat zelf aanvalt.
- Blaze: nieuwe engine-shader `engine/client/fx/Energy` (`shaders/core/energy.*`), kroon en hart in het model, meer animatie (`RodOrbit`, model, leunen in vliegrichting), eigen effect per element, `ScorchBallRenderer`, energie over ray, bliksem en steen; textuur via het script.
- Formulieren: alle tekstvakken 2500 tekens. Niet aangepast: lijst-regels (300, de live relay weigert meer dan 400 per cel) en factietekst (300).
- Getest in het spel (3 runs): alle regelchecks kloppen (lock, wisselen, join -100, lid op 0, waarschuwing, uitgestoten bij -32, hulp, assist, dood doelwit losgelaten), Blaze-beelden van elk element en gevecht, tour (3 kaarten). `./gradlew build` groen.
- 22:25: commit `b259de0` gepusht, v0.9.14-alpha gepubliceerd (Latest).
- Release 2 (0.9.15): Blaze minder fel (textuur gedempt via het script, niet meer in vol licht: minstens licht 9, gloed/hart/effecten zwakker, half zoveel deeltjes), 20% sterker (72 HP, 9.6 schade in `ElementalBlaze`), wind harder (`BlazeCasts` GALE_PUSH 2.1, GALE_LIFT 0.6). Tour: `FROM` 0.9.14, dezelfde 3 kaarten, de 3 nieuwe regels in `SMALL`.
- Getest in het spel (2 runs): 72 HP, wind gooit een zombie ~10 blokken (was ~6.6 berekend), beelden rustiger, tour gelopen. `./gradlew build` groen.
- Niet getest: echte multiplayer; de allerlaatste verzwakking van de vuur-cast (minder vlammen) alleen gebouwd.
- Geen GitHub-rapport over de Blaze gevonden (`bugs.ps1 sync`): alleen de tekst van de gebruiker gevolgd. Bug #84 en ideeen #78, #81, #82, #85, #24 niet aangeraakt (alleen na ja).
- Open punten voor de gebruiker: lijst-regels in formulieren blijven 300 tekens (relay moet anders opnieuw uitgerold), factietekst blijft 300.
