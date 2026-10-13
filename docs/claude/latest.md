# Laatste sessie

- Datum: 2026-10-13 22:24
- Verzoek: takenlijst in twee releases. Release 1 (0.9.14): duidelijke factienamen, alleen uitgestoten bij lage reputatie, skeleton schiet op niets, factiehulp betrouwbaarder, wezens houden hun doelwit (wisselen wel naar wie hen slaat), joinen = alle reputatie kwijt bij de vijanden, mooiere en drukker geanimeerde Elemental Blaze met eigen shader/VFX, tekstlimiet 2500. Release 2 (0.9.15): Blaze minder fel, 20% sterker, meer wind-knockback.
- Facties: 15 nieuwe namen (`FactionDefaults`, `en_us.json`, `docs/PROJECT.md`), oude werelden krijgen ze via `FactionUpgrade` (VERSION 4, `retell`, `liftMembers`). `Reputation.joined`/`left`, `Faction.member`: lid start midden tussen de drempels, waarschuwing 10 boven de drempel, pas uitgestoten op de drempel; vijanden van je factie zetten je op -100.
- Doelwit: `mixin/NearestAttackableTargetGoalMixin` + `MobTargets.inFight` (geen nieuw doelwit zolang het gevecht loopt), `MobTargets.onDamaged` (draait naar wie slaat), `onTick` laat een dood of verdwenen doelwit los (skeleton-bug).
- Hulp: `FactionHelp` luistert nu op de inkomende klap (schild telt), ziet vriend of vijand, en helpt een lid dat zelf aanvalt.
- Blaze: nieuwe engine-shader `engine/client/fx/Energy` (`shaders/core/energy.*`), kroon en hart in het model, meer animatie (`RodOrbit`, model, leunen in vliegrichting), eigen effect per element, `ScorchBallRenderer`, energie over ray, bliksem en steen; textuur via het script.
- Formulieren: alle tekstvakken 2500 tekens. Niet aangepast: lijst-regels (300, de live relay weigert meer dan 400 per cel) en factietekst (300).
- Getest in het spel (3 runs): alle regelchecks kloppen (lock, wisselen, join -100, lid op 0, waarschuwing, uitgestoten bij -32, hulp, assist, dood doelwit losgelaten), Blaze-beelden van elk element en gevecht, tour (3 kaarten). `./gradlew build` groen.
- Niet getest: echte multiplayer; de laatste dimming van de Blaze-effecten alleen gebouwd, niet opnieuw bekeken (komt in release 2).
- Open: release 2; bug #84 en ideeen #78, #81, #82, #85, #24 niet aangeraakt (alleen na ja).
