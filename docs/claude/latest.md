# Laatste sessie — 2026-09-25 (avond, 8)

- **Vraag:** portal-handen (Giant Hands, axe-paar): thumbs-up wees schuin omhoog; moet recht naar voren.
- **Oorzaak:** gestrekte duim staat in het handmodel maar ~49° van de handas; de pose kantelde daarom de hele vuist 42° omhoog om de duim verticaal te krijgen.
- **Gedaan:** nieuwe pose-waarde `thumbOut` (extra uitklappen in het eerste duimgewricht, alleen door de thumbs-up gebruikt). Vuist nu horizontaal naar de speler, duim recht omhoog. Sparkles/geluid bij de duim nu precies op de duimtop (`HandDuo.THUMB_TIP`).
- **Bestanden:** `HandMotion`, `HandDuoMotion` (12e digit), `HandDuoScript` (THUMB_UP-vectoren, THUMB_STRAIGHT, THUMB_TIP), `GiantHandPair`, `client/render/HandPainter`.
- **Getest:** in-game voor/na-screenshots (eerste persoon, voor, zij, ver): vuist recht naar voren, duim verticaal, geen doorsteek. Build schoon. Testklasse, wereld en shots verwijderd.
- **Release:** 0.0.9-alpha (prepare, changelog, commit, push, publish).
- **Open:** bug #5 "test" (low): geen echte bug, gebruiker beslist of het issue dicht mag.
