# Laatste sessie

- Datum: 2026-10-12 22:55
- Verzoek: drie volledig eigen dieren (Brown Bear, Deer, Raccoon): echt uiterlijk, eigen animaties, natuurlijke spawns, spawn eggs, geen ragdolls, nog geen slimme AI.
- Gedeelde basis `creature/animal/` (`WildAnimal`: acts via entity events, `IdleActs`, `client/Channels`).
- Beer (`creature/bear/`): neutraal als ijsbeer (aanval na slag of bij welp), zit, gaat rechtop, rust, schudt droog, brult, klauwt; taiga/bos.
- Hert (`creature/deer/`): bok met gewei / hinde / gevlekt kalf, schuw, graast, alert + stampen, poetst, ligt; bos/taiga/weide.
- Wasbeer (`creature/raccoon/`): zit rechtop, "wast" pootjes, scharrelt, krabt; bos/moeras.
- Vlees: venison + bear meat (rauw/gaar, 6 recepten), loot, fokvoer-tags, biome modifiers, geluiden, `en_us.json`, eieren in mod-tab + vanilla tab, `ragdoll_none`.
- Texturen geschilderd door `scripts/models/animals/` (Java 25).
- Getest in game (scratchpad-kopie): modellen, alle acts, loop/galop, beer valt echt aan, hert vlucht, spawns per biome, drops, Mobs-pagina (beer Neutral). `gradlew build` slaagt.
- Versie 0.9.7-alpha (CHANGELOG + tourstap `wild_animals`); tour in game gelopen (1 kaart, 1/1).
- Niet van deze sessie (niet committen): `CONTENT.txt`, `arena/`, `models/`, `__pycache__/`.
- Gecommit, gepusht en uitgebracht als v0.9.7-alpha. Volgende: bugs #76, #77; ideeën #78, #24 alleen na ja.
