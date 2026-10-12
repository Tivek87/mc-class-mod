# Laatste sessie

- Datum: 2026-10-13 00:39
- Verzoek: zes nieuwe dieren (Hedgehog, Owl, Squirrel, Snake, Crab, Moose), zelfde kwaliteit en regels als beer/hert/wasbeer.
- Alle dieren nu in `creature/animal/<soort>/` (beer, hert, wasbeer daarheen verplaatst); nieuw: `NeutralAnimal`, `WildAnimals`, `client/WildRenderer`, `client/WildAnimalsClient`, `WildAnimal.variant`, `IdleActs.inWater`.
- Egel: rolt op bij speler/wolf/vos, halve schade, prikt blote hand. Uil: slaapt overdag op tak, vliegt 's nachts tak naar tak, roept; bruin of sneeuwwit.
- Eekhoorn: rood (taiga) of grijs, schuw. Slang: ringslang of adder; kronkelt op en sist, bijt één keer, alleen adder is giftig.
- Krab: loopt zijwaarts, land en water, knijpt terug. Eland: stier/koe/kalf; dreigt, valt aan en stampt na slag of bij kalf.
- Nieuw eten: Raw/Cooked Crab (3 recepten); eland geeft venison + leer, uil veren. Loot, voer-tags, biome modifiers, geluiden (uil-roep door `scripts/sounds/owl_hoot.mjs`), `en_us.json`, eieren, `ragdoll_none`.
- Texturen geschilderd door `scripts/models/animals/*Texture.java` (Java 25).
- Getest in game (scratchpad-kopie): modellen, alle acts, varianten, jongen, spawns per biome, drops, recepten, krab ademt onder water, slangenbeet + gif, egel-prik, eland dreigt + stampt, krab knijpt, uil slaapt/vliegt, Mobs-pagina. `gradlew build` slaagt.
- Tijdens test gevonden en opgelost: stilzittende uil tekende met gespreide vleugels; uil vloog te traag; eland-schouderbult leek zadel; stier kon kalf krijgen bij nieuw land; baby-krab vluchtte niet.
- Versie 0.9.8-alpha (CHANGELOG + tourstap `more_animals`); tour in game gelopen (1 kaart, 1/1).
- Niet van deze sessie (niet committen): `CONTENT.txt`, `arena/`, `models/`, `__pycache__/`.
- Gecommit (op verzoek: NIET gepusht, NIET uitgebracht; 0.9.8-alpha blijft open). Geluid, fokken en natuurlijk spawnen niet gecontroleerd.
- Volgend verzoek: Lynx, Monkey, Elephant, Giraffe, Duck, Crocodile (zelfde regels; komen in dezelfde 0.9.8-alpha sectie). Daarna: bugs #76, #77; ideeën #78, #24 alleen na ja.
