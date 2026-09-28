# Laatste sessie

- Datum: 2026-09-28.
- Vraag: doorgaan na v0.4.0-alpha (half af): Thor-combo, klap, mech en de rest in de game nakijken en afmaken.
- Gedaan: testruns met screenshots. Gevonden en gerepareerd: stoten bleven bij het gezicht (handdoel draaide mee met
  de romp; nu gericht vooruit, stoten langer), vuisten en trap-been in eigen zicht te laag, lunge liep door het
  doel heen (superman punch sprong erop), ellebogen iets verder bereik, combo herhaalt minder.
- Nagekeken en goed: combo kiest steeds andere klappen en raakt, klap-opbouw + slag + vonken, mech lopen 0,2 en
  rennen (sprint) 0,76 blok/tick, benen stappen pas mee boven 55 graden, "Locked" in Controls, korte instellingen,
  klap raakt nu ook een dorpeling (bug #34, gemarkeerd als opgelost).
- Docs bijgewerkt: POWERS.md, PROJECT.md, SPELLS.md, CLAUDE.md (Thor-knoppen, geen lamp, instellingen, wie geraakt
  wordt); CHANGELOG sectie 0.4.1-alpha klaar.
- Bestanden: ThorPoses, ThorCombo, ThorBlowKeys, ThorBlow, docs, CHANGELOG. Testklasse, testwerelden en shots weg.
- Build + unit tests groen.
- Open: commit + push + release 0.4.1-alpha (wacht op ja). Niet getest: een getemd dier (groen) en een echte
  multiplayer-server met teams. Ideeen #24/#25/#31/#32/#33 later.
