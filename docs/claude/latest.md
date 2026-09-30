# Laatste sessie

- Datum: 2026-09-30. Verzoek: betere get-up na ragdoll; nieuwe botten (bekken, schouderbladen, handen, voeten; geen
  nek); ragdolls gebruiken ze; regel "gebruik alle botten" in CLAUDE.md. Daarna: afronden, commit, push, release.
- Gedaan: `ModelBends` (chain/hang/shoulder), `BentParts` + nieuw `FoldChain` (2 knikken per deel), `Limbs`
  (pols/enkel/bekken/hips/shoulder), `BoneView`, ragdoll met 3 stukken per deel + spook-schouderbladen
  (`RigidWorld.ghost`, `LimbJoint.turnsOnly`), get-up herschreven in `ragdoll/getup/` (PersonRise, Skeleton,
  CreatureRise, PoseBlend, BodyPose, Hanging), 52 ticks; server `Knockdowns.DOWN` 110 -> 125. Versie 0.5.4-alpha.
- Tests: volledige build groen (123 tests), nieuw `PersonRiseTest`; `RagdollRestTest` crowd-grens 1.2 -> 1.5.
- NIET getest: in-game beeld van de get-up (alleen offline wiskunde-test). Keyframes waarschijnlijk nog tunen.
- Overlap-gemiddelde in RagdollRestTest 0.0699 (grens 0.07): krap.
- Niet meegenomen: `MechScript.java` (andere sessie), `run.txt`. Open: bug #35, ideeën (#45/#58 deels gedaan).
