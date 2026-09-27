# Laatste sessie

- Datum: 2026-09-28 (nacht).
- Vraag: meer botten (2 per arm/been, 1 hoofd, 1 romp), hand- en mech-botten zichtbaar, mech veel te zwaar, Giant Hands
  niet meer door blokken/wezens, mech-torso 360° mee met de crosshair (benen draaien bij).
- Gedaan: ragdolls hebben knie en elleboog (`HingeJoint`, `BentParts` + `ModelPartMixin`, AT-regels); bone view 2
  botten per ledemaat, breedte schaalt met afstand, constructs in magenta. Mech: vertices direct in native geheugen,
  geen fade-wortel ver van het oog, onderdelen parallel via `ShapeBatch` (CRC-bewezen identiek): tekentijd 8,0 → 1,5–2,4
  ms. Mech-bovenlijf draait op de heupen naar de blik, benen halen in, A/D stappen opzij. Giant Hands duwen wezens
  opzij (`GiantHandBase.shove`), `HandStop` houdt ook terug als volledig terughouden minder in blokken steekt.
- Getest: JUnit (HingeJoint), build groen, in-game runs (ragdolls, hands, mech meten/draaien) + rooktest.
- Bestanden: engine render/physics/model/ragdoll/rig, mech (Drive/Walk/Pose/Painter/Legs/Script/Assembly), hands
  (GiantHand/Base, HandStop), docs POWERS/GREEN_LANTERN/PROJECT, CLAUDE.md (codekaart + testregels).
- Open: niets gecommit (wacht op ja). Bugs #30, #28, #27, #29 en ideeën #24/#25/#31 staan nog open.
