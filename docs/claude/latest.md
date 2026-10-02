# Laatste sessie

## Mech-klim (sessie "mech climb")
- Datum: 2026-10-02. Verzoek: Hard-Light Mech: armen/benen niet los/glitchend, niet vast in grotten en kuilen,
  overal klimmen, klimmen na 0,5 s, betere vingers, eigen rig/botten zonder clipping. Bugs/ideeën niet aangeraakt.
- Gedaan: klim tot 30 hoog (hand over hand, voeten tegen de muur, handen haken over de rand en draaien om, lijf
  leunt over de rand en zakt tot de handen op de top liggen, borst exact van de muur), start na 10 ticks, zijbanen;
  arm-rig (pols, elleboog uit blokken, schouder rekt de arm niet), knie uit muren, vingers soepel en nooit in
  blokken, zwaaiende voet zakt neer. Grot-bug: randzoeker vond het dak van de grot (`MechGround.ledge`).
  Eindreview: `Ik.swivel` (knie, klim-elleboog) probeert nu eerst naar buiten en zoekt het precieze vrije punt
  (geen sprongen van 0,13 rad meer); ongebruikte `MechMoves.Arm.turned` weg.
- Bestanden: mech `walk/`, `touch/` (nieuw: `MechArmRig`, `MechFingers`, `MechClimbHolds`), `MechLegs`,
  `MechPainter`, `MechMoves`, `engine/rig/Ik` + tests (`IkTest`, `MechClimbTest`, `MechArmRigTest`), `docs/GREEN_LANTERN.md`.
- Getest: `./gradlew build` groen; in-game rand 4/6/9, muur 14/20, kuil, grot, schuin, ongelijk: geklommen; na de
  IK-fix wall14/ledge6/cave4 opnieuw: zelfde klim, geen fouten, beelden gelijk aan de vorige run.
- Release: op ja van de user gecommit, gepusht en uitgebracht als v0.6.2-alpha (CHANGELOG-sectie erbij).
- Open: niet gedaan: bukken onder lage plafonds, overhangen. Een knie
  zwaait snel uit als een snelle stap hem tegen een muur zet (zo bedoeld: nooit erdoor), en zakt langzaam terug.

## Thor-ontwerp (andere sessie)
- Datum: 2026-10-02. Verzoek: ontwerp voor Thor's vlucht, hamerworp, Throw and Follow (snel naar de hamer) en
  de hamer die in de wereld blijft (grond of lucht), als MD-bestand.
- Gedaan: Thor-code en -docs gelezen; ontwerp `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM.md` (Engels, met bouwplan)
  en korte simpele versie `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM_SHORT.md`. Bugs/ideeën niet aangeraakt (verzoek).
- Besloten (user): Throw and Follow = getimede dash: rechts vasthouden = verder (4-14 blokken), loslaten = gooien,
  0,5 s wachten (bliksemlijn hand-hamer), dash op vast tempo (6 blokken in 0,7 s). Gewone worp max 24 (was 40).
- Bestanden: beide ontwerpen (nieuw), dit bestand. Niets gebouwd, niets gecommit.
- Open: 10 beslissingen in §9 / "Still to decide"; daarna fase 0 bouwen. Mech-wijzigingen en `devtest/` in de
  werkboom zijn van een andere sessie.

