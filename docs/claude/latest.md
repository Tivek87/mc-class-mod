# Laatste sessie

- Datum: 2026-09-29. Verzoek: ragdolls (bevroren lijken, ≥5 s liggen, door vloer/stuiteren, klap-richting, opstaan),
  kill-bevestiging (RDR2), mech-handen/armen tegen terrein + grijpen, sneller draaien/torso verder, ideeën-doc;
  daarna klimmen pas na 2 s tegen muur lopen+kijken; "commit en push met release" -> 0.5.0-alpha.
- Oorzaken: botsing pakte max 32/96 blokken van onderaf (snelle delen vielen door de grond); hoek-inslag gaf ~80 rad/s
  spin (stuiteren); slapend lijk werd nooit wakker als grond wegviel; mech-hand is 1 stijf stuk (knokkels in richel).
- Gedaan: `LevelBlocks` (nu `engine/client/world`, begraven blokken eruit), `RigidBlocks` (dichtstbijzijnde eerst,
  rustig uitduwen), `RigidWorld` (draai-limiet, contact-demping, `probe`), `RagdollFalls`, `Corpses`, `DeathBlows`
  (+payload), opstaan (kijkrichting, overlap), `killconfirm/` (+`sounds/ui/kill_confirm.ogg`), `MechTouch`,
  `MechHandRig` (vingers op blokken, grip), `MechClimb.laid`, `MechDrive` (90°/5,5°/tick, klim 40 ticks), docs.
- Getest: 111 unit-tests groen; in-game 3 ragdoll-runs + mech-runs (geen afwijkingen; greep 0,02 diep; klim na ~2 s).
- Open: bug #35 (GL-vliegen "ultra traag", medium); ideeën #24,#25,#31,#32,#33 wachten op ja/nee. Map
  `client/mech/` heeft 19 bestanden: opsplitsen in sub-mappen voorstellen.
