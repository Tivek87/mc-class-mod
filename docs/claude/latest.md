# Laatste sessie — 2026-09-24

- **Vraag:** alle 16 wapens in het Construct Wheel zetten, alleen met hun naam (geen uitleg) en met mooie iconen die op het wapen lijken.
- **Gedaan:**
  - 15 placeholders vervangen door de wapens uit de lijst (`Construct`-enum; volgorde en netwerk-nummers gelijk). Ze doen nog niets (net als lege handen).
  - Nieuwe `WeaponShapes`: 15 gedetailleerde hard-light 3D-modellen. `ConstructIcons` zet elk wapen in een eigen pose; paren als twee (vuistbots, gekruiste dolken/revolvers, SMG's rug aan rug). Armkanon en vlammenwerper hebben een klein lichtje (licht, geen construct).
  - Wiel: alleen de naam voor de nieuwe wapens; lange namen breken af na " / ". Sword & Shield houdt zijn uitleg.
  - Nederlands taalbestand verwijderd (mod alleen Engels); regel toegevoegd in project-CLAUDE.md.
  - Getest in het spel (3 runs, screenshots groot en in het wiel, ook klein scherm); testklasse, testwereld en screenshots weer verwijderd; build slaagt.
- **Bestanden:** Construct.java, ConstructIcons.java, WeaponShapes.java (nieuw), ConstructWheelScreen.java, ConstructHud.java, en_us.json, nl_nl.json (weg), CLAUDE.md, docs/POWERS.md, docs/GREEN_LANTERN.md, docs/PROJECT.md, docs/CHANGELOG.md.
- **Vervolgvraag:** zijn de 3D-iconen echte modellen? Antwoord: ja, echte meshes in `WeaponShapes`, maar nu alleen als icoon gebruikt (wiel + hotbar-balk), nog niet in de hand of wereld.
- **Commit + push:** gedaan op verzoek ("commit en push alles"), samen met het werk van de vorige sessie.
- **Open:** Op het kleinste scherm overlapt de balk boven de hotbar met lange namen het abilities-paneel (bestond al met de lange uitleg van Sword & Shield). Wapens echt laten werken vraagt per wapen toestemming.
