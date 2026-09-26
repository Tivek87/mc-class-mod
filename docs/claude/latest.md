# Laatste sessie — 2026-09-26 (avond, deel 5)

- **Vraag:** bubbel 15% kleiner; oplaadmeter op de flamethrower; 8 nieuwe handen (ragdoll-slam met 50% vangvuist, ringblast met 5 handen, + ring beam, clap, finger gun, scissors, swallow, scoop), 65% portaal / 35% grond-of-muur (dynamisch); commit.
- **Gedaan:** `ClapFx` 1.275; `FlamePainter.gauge` (8 wit-hete balkjes, `Glow.charge`); nieuwe lagen `HandFeats`, `HandGroup` (samengestelde handen), `GiantHandFeats`, `GiantHandSpots` (afgesplitst), client `HandFeatLight`, `HandGroupPainter`. Variant-code: bit0 spiegel, bit1 muur, rest extra (ragdoll-slams). Muurhanden = zelfde pose, frame 90° gekanteld (`HandPose.place`). Catch-hand via `GiantHands.catchFalling`.
- **Getest in-game (6 runs, testklasse/wereld/shots weg):** alle 8 raken; ringblast gooit 37 blok; swallow valt uit de lucht; ragdoll uit muur, lancering + vangvuist 3 s; gauge zichtbaar; ring beam na herafstelling 5 treffers; wezen nooit meer onder de grond. Na laatste run alleen de splitsing van `GiantHands` (code verplaatst): build OK, niet opnieuw in-game.
- **Open:** push + `release.ps1 publish` na ja; bestaande grondhanden (smack/grab/…) komen nog niet uit muren.

# Eerder — 2026-09-26 (avond, deel 4)

- **Vraag:** Thunder Clap richten op crosshair + bubbel (70% kleiner, bij de handen, vergrootglas) + vonken op doelpunt, geen grondringen; flamethrower overheat na 15 s (2 s lockout, stoom) + vlam wilder met oplading; 7 nieuwe Giant Hands (flick, pinch 9 blok, snap, poke, hammer, rake, drag); jetpack hoger + 15% groter; 1 klap maakt neutraal vijandig. Commit, niet pushen.
- **Gedaan:** `ThunderClapSpell` (raycast richten, 3D-kegel), `ClapFx`, `Lens`/`lens.fsh` (Rupture = lichte waas). `FlameMove.OVERHEAT`, instelling `infernoOverheatSeconds`, `FireStream` STEAM + `charge`. Nieuwe lagen `HandTricks`, `GiantHandTricks`, `HandTrickLight`; portaalhanden via `HandPose.portal/portalOf/workOffset`. `BackSpot`/`Jetpacks`. `Standings.GRUDGE_HITS` 1. Beam schaalde al (40→64 blok, schade ×3): niets aangepast.
- **Getest in-game (7 runs, testklasse/wereld/shots weg):** clap raakt doel op pilaar, zijhusk niet; overheat exact na ingestelde tijd, lockout, opnieuw vuren; alle 7 handen raken (logs + screenshots); pinch tilt 9,5 blok; drag sleurt 26 blok over grond; koe na 1 klap vijandig; jetpack zichtbaar. `gradlew build` OK.
- **Docs:** SPELLS, POWERS, GREEN_LANTERN, PROJECT, CLAUDE.md (one-hit rule), CHANGELOG 0.2.9-alpha.
- **Git:** gecommit, NIET gepusht (samen met de 2 eerdere lokale commits). Na push: `scripts/release.ps1 publish`.
- **Open:** push + publish na ja; issues #18/#22 al door user gesloten.
