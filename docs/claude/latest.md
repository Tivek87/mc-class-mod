# Laatste sessie — 2026-09-27

- **Vraag (1-5):** eerdere vragen (whip, handen, release v0.2.9-alpha, config-namen, lijst nieuwe handen) afgerond.
- **Vraag (6):** nieuwe Giant Hand "Tear apart": hand uit portaal boven grijpt de handen, hand uit portaal in de grond de voeten; wezen opgetild, uitgerekt in I-pose, 5 rukken (0,7 hart), scheurt bij het midden (18 harten). Helften = het echte model in tweeën, groen dicht snijvlak, gaan mee met de handen, vallen en kiepen om. Overlever wordt omlaag en weg gesmeten.
  - Server: `GiantHandTears` (nieuw), `HandGroup` (tear-geometrie), `GiantHandSpots` (ruimte + grond), `GiantHandBase.push`.
  - Client: `HandVictimTears` (nieuw: I-pose, stretch, helften), `HandGroupPainter.tear` (licht), engine `ClippedBuffers` (nieuw: model knippen op een vlak).
  - Bug gefixt: `ConstructPayload` stuurde `variant` als byte → ragdoll-slam en bijl (13/16 richtingen) kwamen als verkeerde hand aan. Nu VarInt.
  - Getest in-game (husk zij/speler-zicht, creeper): klopt. `gradlew build` OK. Testklasse, wereld en shots weg.
  - Docs: POWERS, GREEN_LANTERN, PROJECT, CHANGELOG `[0.3.0-alpha]`; CLAUDE.md: 3 regels aangescherpt.
- **Vraag (7):** "commit en push alles": versie 0.3.0-alpha, alles gecommit (ook `docs/reference/mech-robot-ultimate.mp4`), gepusht, release v0.3.0-alpha.
- **Open voorstellen:** CLAUDE.md-zin "singleplayer encodeert niet" klopt niet; `GameCharacter.java` 597 regels (bijna splitsen); settings-namen "Portal drag"/"Pair with an axe".
