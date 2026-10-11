# Laatste sessie

- Datum: 2026-10-11 16:11
- Vraag: Schematic Wand/blueprint-QOL: grid uit (0), mod-blokken melden + Modrinth, ability-overlay verslepen/schalen (Change layout), rood wat vervangen wordt, blueprint-box verbergen, hernoemen/kopiëren, duidelijke materialen, bouwkaart bovenin, gebied opslaan (2 hoeken), laag-voor-laag, gids-pijl, hologram-lagen, "at last spot".
- Gedaan: alles gebouwd. Nieuw: `SchematicSaves`, `AreaSelection`, `SchematicGuide`, `BlueprintViews`, `HologramReplaced`, `PanelLayoutScreen`, map `schematic/client/menu/` (menu-pagina's, incl. `SavePage`, `MissingBlocksScreen`, `BlueprintMaterials`).
- UI-fixes na screenshots: Place-pagina past op kleine GUI, Library-knoppen, rode mods-regel 1 regel, meervouden, Plan/Materials-tab overlap, layout-scherm uit de weg van paneel.
- Review-fixes: selectie stopt bij hologram tonen, layout-scherm vs onbewaarde instellingen, kopiëren buiten server-thread + groottegrens, laag-cache (volatile), overflow bij grootte-check, tooltips kleine knoppen, last-spot volgt hernoemen.
- Getest in game (854x480): opslaan 216 blokken, kopie+hernoem, mods-lijst, grid Off, 36 rode blokken, lagen, bouwkaart 100%, blueprint laag/gids, hernoemen blueprint, overlay verplaatst; tour 0.8.6 (7 kaarten) gelopen; 0 mod-fouten.
- Versie 0.8.6-alpha (prepare), CHANGELOG + TourSteps; build + tests groen. Testklasse, wereld, shots opgeruimd; tour.json teruggezet.
- Niet van mij, niet meecommitten: `arena/`, `CONTENT.txt`.
- Daarna: Bloater barst meteen bij klap/schot van voren of opzij, alleen van achter te verslijten (`Bloater.facing`). Vijfde zombie Rustbound (`creature/rustbound/`: magneet-kern trekt + rukt metaal uit hand, scherven-waaier met bloeding, vuist slaat schild weg, nat = kern dood); textures via `scripts/models/zombies/RustboundTexture.java`; ei, tags, loot, spawns, damage type `shrapnel`, tag `metal`; CHANGELOG + 2 tourstappen.
- Getest in game: Bloater voor = ontploft, achter = leeft; Rustbound pull (golem 6,9->1,6, speler 3,6 blokken), afpakken zwaard + schild, scherven + bloeding, klap slaat schild weg, animaties op beeld; tour 0.8.6 (10 kaarten) gelopen; build groen.
- Golem 'raakt zonder zwaai': niet na te bootsen (vanilla + hard-light golem, alle 5 zombies, ook na omver gaan) -> details nodig van gebruiker.
- Open: commit + push + release 0.8.6-alpha wacht op ja; idee #24 blijft open.
