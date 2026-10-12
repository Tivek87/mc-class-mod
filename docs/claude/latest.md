# Laatste sessie

- Datum: 2026-10-13 01:40
- Verzoek: zes nieuwe dieren (Lynx, Monkey, Elephant, Giraffe, Duck, Crocodile), zelfde kwaliteit en regels als de vorige; de vorige zes (commit `46ceba0`) zijn gecommit, niet gepusht.
- Nieuw in `creature/animal/`: `lynx/`, `monkey/`, `elephant/`, `giraffe/`, `duck/`, `crocodile/` (elk: dier, `client/` model, poses, renderer), `Afloat` (drijven, peddelen, water zoeken), `IdleActs.afloat`.
- Lynx: schuw, sist en klauwt na een slag; tawny of bleek (sneeuw). Aap: kapucijn, wit-gezicht of bruin, onschuldig; zit, krabt, eet, roept.
- Olifant: stier/koe/kalf; trompettert, valt aan en gooit je op na slag of bij kalf; slurf 5 delen. Giraffe: trapt één keer terug en vlucht; drinkt met gespreide voorpoten.
- Eend: woerd/eend/kuiken; drijft, grondelt, vliegt kort weg bij schrik. Krokodil: drijft of ligt op de oever; spert bek en sist, bijt wie blijft (1 beet per 1,8 s).
- Nieuw eten: Raw/Cooked Duck (3 recepten); leer van olifant, giraffe, krokodil. Loot, voer-tags, biome modifiers, `en_us.json`, eieren, `ragdoll_none`, krokodil ademt onder water.
- Texturen: `scripts/models/animals/*Texture.java` (Java 25). Eigen geluiden (kwaak, apenroep, trompet): `scripts/sounds/animal_calls.mjs`.
- Getest in game (scratchpad-kopie): modellen, alle acts, varianten, jongen, spawns per biome, drops, recepten, krokodil waarschuwt + bijt, eenden drijven + vliegen, lynx sist + klauwt, giraffe trapt, olifant trompettert + gooit, Mobs-pagina. `gradlew build` slaagt.
- Tijdens test gevonden en opgelost: krokodil waarschuwde nooit (toets op `tickCount % 4` in een goal dat om de 2 ticks wordt gevraagd); krokodil beet te snel; lynx-oren te zwart; apenstaart te stijf; olifantstof was groen; slurf te dun.
- Versie blijft 0.9.8-alpha (CHANGELOG-regels + tourstap `safari_animals`); tour in game gelopen (2 kaarten).
- Niet van deze sessie (niet committen): `CONTENT.txt`, `arena/`, `models/`, `__pycache__/`.
- Nog NIET gecommit: wacht op ja van de gebruiker (push/release alleen als gevraagd). Geluid op gehoor, fokken en natuurlijk spawnen in een echte wereld niet gecontroleerd.
- Daarna: bugs #76, #77; ideeën #78, #24 alleen na ja.
