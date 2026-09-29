# Laatste sessie

- Datum: 2026-09-29. Verzoek: mech zwaarder/trager rennen, benen/armen niet meer los, niet meer vast in gaten/grotten
  (klimmen/opstappen); daarna commit + push + release.
- Oorzaken: IK-knip in `MechLegs.knee`, rechtervoet wachtte hele pas bij start, hoogte uit 1 kolom onder het midden
  (viel in smalle gaten), grotdak telde als grond, geen klim boven 3,2 blok.
- Gedaan: `MechGround` (grond onder hele voet/beide voeten, muren, richels), `MechClimb` (richel tot 9,5 blok: handen op
  rand, optrekken, voet erop, eroverheen; via `MechDrivePayload` + mech-variant naar iedereen), `MechGait` (voeten, uit
  `MechWalk` gesplitst: eerste stap meteen, achterblijvende voet stapt meteen, voetsteun naast gaten, vallen + harde
  landing), `MechWalk` (lijf op voeten, hurkt voor bereik), `MechLegs`-vangnet. Rennen 0,75 -> 0,5 blok/tik, lange lage
  passen. Server: klim doorgeven, geen slag tijdens klim, pilootspeling 3 blok. Benen stappen max 2,6 blok op.
- Getest: 103 unit-tests groen (nieuw `MechClimbTest`, `MechDrivePayloadTest`); in-game baan (lopen, rennen, geulen,
  trede, putten 5/7 diep, grot met dak): alles gehaald, geen voet buiten bereik, handen op richel, pilotenzicht ok.
  Testklasse/wereld/shots weg. Regel aangescherpt: `CLAUDE.md` in-game tests (invoer van buiten tussen ticks).
- User: "commit and push" (+ 2 screenshots van een hand op de richel) -> release 0.4.9-alpha (commit, push, GitHub).
- Volgende: bug #35 (GL-vliegen "ultra traag", medium); ideeën #24, #25, #31, #32, #33 wachten op ja/nee.
