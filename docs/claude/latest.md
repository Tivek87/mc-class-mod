# Laatste sessie

- Datum: 2026-10-13 16:26
- Verzoek: eigen factiesysteem: elke mob en speler in een factie, vriendelijk/neutraal/vijandig per factie en per mob, reputatie met drempels, helpen, config-pagina, commando's, idee-formulier "New faction" met mob-lijst (3D-model + info), veel QOL.
- Gedaan: `faction/` uitgebreid (`rules/` = `factions.toml`, `FactionStands`, `MobFactions`, `Reputation`, `FactionHelp`, `FactionEdits`, `net/`, `command/`), Factions-pagina (`faction/client/page/`), mob-kiezer (`faction/client/pick/`), `FactionIdea` + veldtype `Field.mobs`, Mobs-pagina toont factie, 11 startfacties (alles "zoals het spel"), tourstappen, CHANGELOG 0.9.11, `docs/PROJECT.md` hoofdstuk 6.
- Bugs gevonden door de test in het spel en opgelost: jacht-cache ververste nooit (overflow); het spel-eigen doelzoeken bleef hangen op een geweigerd doel en blokkeerde elke andere jacht -> `mixin/TargetingConditionsMixin` (bevriend dier vlucht daardoor ook niet meer). Undead standaard niet "united" (skelet-pijl maakt zombie nog boos, zoals in het spel).
- Getest: in het spel 53 checks, 0 fouten (reputatie, eigen houding per mob, helpen, jacht tussen facties, hert vlucht niet voor vriend, undo/reset, join/leave, echte klikken op pagina, kiezer en formulier); tour gelopen (3 kaarten); `gradlew build` slaagt.
- Niet getest: pagina als niet-host, echte multiplayer-verbinding.
- Opgeruimd: testklasse, probe, testwereld `ftest`, screenshots; `run/options.txt`, `reports.json`, `tour.json` teruggezet.
- `CLAUDE.md`: code-indeling `faction/`, regel over geweigerde doelen (mixin), testregel over hergebruikte testwereld.
- Nog niet gecommit/gepusht: alles samen als v0.9.11-alpha, wacht op ja. Daarna `release.ps1 publish`.
- Open: bugs #76 (medium), #77 (low); ideeen #79 (testje van gebruiker), #78, #24 alleen na ja.
