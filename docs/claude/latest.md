# Laatste sessie

- Datum: 2026-10-11 05:17
- Vraag (2e batch): stille groene laag op summons, vrienden vechten niet door na per-ongeluk-klap, ALLES-knoppen (lijst/groep), Everyone hostile, Undo (25) en Reset all op Mobs-pagina, veel meer mob-getallen, meer config (fists/zuilen, storm, summons), operators-mogen-bewerken-instelling, Cosmos-hand om de maan, storm max 1 bliksem per 3 s.
- Gedaan: alles gebouwd. Nieuw: MobStats (15 getallen per mob), MobRules wildcards (`actor>*`, `*>*`) + undo-stappen + reset, 4 payloads, Mobs-pagina gesplitst (MobRulesState + MobRulesScreen).
- Bug gevonden in test: wissen van lastHurtByMob in target-event crashte vanilla HurtByTargetGoal; nu in tick (MobTargets.onTick).
- Cosmos: palm plat op kubusvlak, hand 1.0x maan, vingers stoppen op gedraaide kubus (HandRig.Held met assen).
- Getest in game: vriendelijk (geen doelwit/boosheid), storm 3 calls in 140 ticks, regels/undo/bulk/reset, Mobs-pagina shots, Cosmos-greep shots.
- Changelog 0.8.3 aangevuld + 3 tourstappen (mob_all, mob_undo, mob_numbers).
- Niet van mij: src/.../arena/ArenaCommand.java (ongetrackt), niet meecommitten.
- Laatste run: groene laag, mob-getallen (hp x2, armor +5, grootte 1.5, reset) en Mobs-pagina na splitsing ok. Testklasse/wereld/shots weg, tour.json terug. build + release check groen.
- Open: 3 nieuwe tourstappen nog niet in game gelopen (voor push doen); commit/push/release op ja; idee #24 wacht op ja/nee.
