# Laatste sessie

## Paneel rechtsonder: alleen bruikbare toetsen, korte muisnamen (2026-10-03)
- Verzoek: paneel toont alleen toetsen/knoppen met een ability; muisknoppen kort (RMB).
- `AbilityPanel`: vrije slots (spare) en ongebonden toetsen weg; "free"-regel en lang-key weg.
- `PowerInputs`: `bound()` + `keyName()` (LMB/RMB/MMB/M4, lang `input.welcomescreen.mouse*`), ook in instellingen.
- Doc Ock: `placeholder`/`placeholder_2` nu `.spare()` (geen rij, geen sectie in instellingen, geen melding);
  dode case in `OctopusArms` + `octopus.welcomescreen.placeholder` weg. `docs/POWERS.md` bijgewerkt.
- Getest: build + tests groen; in-game: Doc Ock zonder Placeholder-rijen, GL toont `[Hold MMB] Mech`.
- Open: niet gecommit (vraag ja). Thor-paneel toont bewust geen gebaren (`ThunderGauge` regel), dus leeg.
  Bugs #59-#63 (high) en 5 ideeën nog open.

## Thor-ontwerp (andere sessie, 2026-10-02)
- Ontwerp `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM.md` + `_SHORT.md` (niet gecommit, van die sessie). Besloten:
  Throw and Follow = getimede dash, gewone worp max 24. Open: 10 beslissingen in §9; daarna fase 0 bouwen.

