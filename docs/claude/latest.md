# Laatste sessie — 2026-09-25 (avond, 11)

- **Eerder vandaag:** ideeën-knop (v0.1.3), wind van de plane weg + categorie weg uit ideeën (v0.1.4).
- **Ideeën auto-sync:** de Windows-taak draait `bugs.ps1 sync` elke 5 min; die synct ook `bugs/ideas/` (log: "0 open, 0 ideas"). Niets aan veranderd.
- **Giant Hands per hand:** `HandMoves.HANDS`; per hand Chance / Most / Damage / Knockback (`GameCharacter.hand`), `GiantHands` kiest gewogen, telt per druk (`made`), stopt als niets meer mag; `GiantHandBase.hit` vermenigvuldigt schade/terugslag. Labels en groepen in `en_us.json`.
- **G-wiel:** linksboven "Faction: <naam>" (groen) of "none" (`PowerWheelScreen.faction`).
- **Balans:** schild-charge 16 → 12 b/s; crash ×0.25 (`CRASH_SIZE` 1.5, bereik 24.1, krater 2.4, puin 40); miniguns `gunTicks` 3.72, `gunDamage` 1.575; jets `jetMissileTicks` 48, `jetMissileDamage` 2.5. `.was` + `DEFAULTS_VERSION` 17.
- **Getest in-game:** handen: limiet pound 2 + slam 1 → precies zo en klaar; schade x1/x0.5/x0 → 11.8/5.9/0. Wiel: "none" en "Lanterns". Air Strike: 10.78 schoten/s (verwacht 10.75), kogel ~1.55, jet-raket ~2.45, krater 23 blokken. Jet-tempo niet gemeten (setting direct gebruikt). Build schoon, testklassen/werelden weg.
- **Docs:** POWERS.md, GREEN_LANTERN.md, PROJECT.md, CHANGELOG 0.1.5-alpha.
- **Open:** crash-geluiden niet verkleind (alleen beeld/bereik/krater); crash-schade (24 harten) ongewijzigd.
