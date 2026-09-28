# Laatste sessie

- Datum: 2026-09-28.
- Vraag: config kleiner (client ~15, server terug naar ~0.3.8), mech-lamp weg, mech sneller rennen, torso verder
  draaien, "Locked" bij vaste keybinds, Thor: combo (24 hand + 4 trappen) op links klik, thunderclap op links vast
  0.75 s (achterover, armen wijd, klap, bliksemvonken, donder ver weg); bug #34 (iedereen raken behalve eigen team).
- Gedaan: alles gebouwd (ThorBlow/ThorBlows/ThorCombo/ThorBlowKeys/ThorBlowPoses/ThorFists, FirstPersonLeg,
  ClientClaps wind-up, ClapFx vonken, Factions.mayHit, Targeting.mayStrike); SpellRules, MechLamp, Spotlight,
  ModSounds weg. Gebruiker zette mech-rennen zelf op de sprint-toets (MechDrive).
- Build + unit tests groen. In-game testrun draaide (t7), maar screenshots en log NIET nagekeken: Thor-houdingen,
  eerste-persoon, klap en keyframes ongetest. Op verzoek meteen commit/push/release v0.4.0-alpha ("nog lang niet
  af, heel buggy").
- Open: screenshots-test opnieuw draaien en houdingen/timing afstellen; POWERS.md, PROJECT.md en CLAUDE.md
  beschrijven nog oude Thor-knoppen, mech-lamp, instellingen en de oude factie-regel (bijwerken); bug #34 nog open
  op GitHub (niet in-game getest); ideeen #24/#25/#31/#32/#33 later.
