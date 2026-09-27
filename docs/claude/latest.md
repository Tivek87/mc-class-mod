# Laatste sessie — 2026-09-27

- **Vraag:** mech: hoofd massiever (zoals clip), raam weg → piloot dieper, gat met groen glas; mech kan alleen lopen (vloeiend, piloot bedient hendels/knoppen); derde persoon achter de mech; klap-armen niet meer door elkaar. Emerald Express beter + 3-5x langer + betere geluiden. Dracula en Odin in tab Other; Thor speelbaar met bliksem, Thunder Clap van spell naar Thor-ability (linkermuis 0,75 s + meter), tijdbel 20% kleiner en korter. Daarna committen en pushen.
- **Mech:** `MechWalk`/`MechPose`/`MechLegs` (stapcyclus, voeten vast, knie-IK, wiegend lijf, zwaaiende armen), `MechDrive` + `MechDrivePayload` (W/S lopen, A/D draaien, terrein tot 3 blokken, waadt door bomen), `MechCockpit` (stoel, console, hendels, knoppen, glas), nieuwe helm, ronde patrijspoort; `ChaseCamera` (engine) voor derde persoon; mixin laat piloot door bladeren bewegen.
- **Express:** 4 rijtuigen (`ExpressCoaches`, trein ~88 blokken), schommelen, zigzag-ontsporing, kettingexplosie, pufjes; geluiden fluit/toeter/bel/klik/rommel/crash/tsjoeks (ffmpeg-synthese), `ExpressSounds`; runBlocks 220.
- **Thor:** `character/thor/` (`ThorPowers`, `Thunderclap`, `ThunderGauge`); Dracula/Odin in `Roster`; paneel verbergt lege slots.
- **Getest in-game:** ~15 runs (vlak + normaal), screenshots van klap, cockpit, lopen, draaien, knoppen, express, Thor-meter, Other-tab; codec van `MechDrivePayload` ok; `gradlew build` ok. Testklasse, wereld, screenshots verwijderd.
- **Docs:** POWERS, GREEN_LANTERN, SPELLS, CHARACTERS (Dracula/Odin), PROJECT, CLAUDE.md, CHANGELOG `[0.3.3-alpha]`.
- **Niet getest:** echte multiplayer-server (alleen codec-roundtrip), geluid op het oor.
- **Open:** ideeën #24/#25 wachten op ja/nee; `docs/copilot chat bonesxragdoll.txt` blijft buiten git.
