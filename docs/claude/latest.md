# Laatste sessie

- Datum: 2026-10-12 20:19
- Verzoek: Melkweg niet op de plek van de maan, kring om de zon weg, Galactus niet over de maan, Aarde weg uit de hemel, Wizard gevarieerder en sterker (+30% HP, +30% armor, +35% schade), Galactus trouwer aan de plaatjes (huid, masker, blauw-paarse ogen), en naar de maan/planeten kunnen vliegen (maan 7000 blokken hoog).
- Hemel: Melkweg ligt op de grote cirkel die het verst van de maan blijft (`SkyBodies.BAND`); gloed om de zon weg (`SkyCubes`); Aarde uit `SkyBodies.PLANETS` (11 planeten; `Kind.EARTH` staat er nog voor een latere Aarde-kubus).
- Galactus: de maankubus wordt na hem nog eens met diepte getekend en verbergt wat achter hem staat (`CosmicSky.moonOver`, `SkyCubes.moonAlone`); gezicht van huid, masker over ogen en wangen, ogen blauw-violet, hoge kroon, bredere zijplaten, staalblauw juk (`GalactusShape`, `GalactusFx.EYES`).
- Wizard: 910 HP, 4,55 armor, 14,58 aanval (`WizardEntities`); 10 spreuken van fase 2 naar fase 1 en 4 latere een fase eerder (`WizardMove`); tussen twee grote spreuken meestal alleen bewegen, lichte spreuk om de beurt (`WizardPicks`), kortere adempauze (`WizardMoves.BREATH`).
- Getest in game (scratchpad-kopie): Galactus achter de maan, Melkweg ~45 graden van de maan, geen kring om de zon; Wizard 38 soorten zetten in fase 3 en 43 in fase 5; tour van 0.9.5 (4 kaarten). `gradlew build` slaagt (in de kopie, want het eigen spel van de gebruiker draait vanuit de hoofdmap).
- Klaar voor release 0.9.5 (`mod_version`, `CHANGELOG.md`, `TourSteps`, `en_us.json`): wacht op ja voor commit + push; daarna `release.ps1 publish` vanuit een losse worktree.
- Niet committen: `CONTENT.txt`, `arena/`, `models/`, `__pycache__/`.
- Niet gebouwd: naar de maan en planeten vliegen. Echte grond op 7000 hoog kan niet zonder wissel van dimensie (blokken bestaan alleen tot y 2031); vragen daarover gesteld.
- Open bugs: #76 (minigun/arm blaster first person), #77 (dubbele stage HUD); ideeën #78, #24.
