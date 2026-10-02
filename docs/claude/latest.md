# Laatste sessie

- Datum: 2026-10-02. Verzoek: betere ragdolls, RDR2/Euphoria-achtig struikelen en balans houden, randen en takken
  grijpen, realistische dood-impact, minder lag met 3-7 ragdolls, nieuwe botten gebruiken, "rare" mobs fixen.
- Gedaan: stagger/trip/fall (`engine/entity/impact/`, `engine/client/stagger/`), spieren in ragdolls (`Muscle`,
  `RagdollMuscles`, `RagdollMotor`), richel- en takgreep (`Ledges`), ragdolls parallel en slapend goedkoop
  (`RagdollSteps`; 7 lijken: client tick 4.7 ms -> ~0.7 ms), exact trefpunt van de dodelijke klap, yaw-fix bij
  stilgezette mobs (`Knockdowns.pause`), twee wereldinstellingen.
- Getest: 4 game-runs (buik-stagger, klif + richelgreep + loslaten + opstaan, worp door kracht); `./gradlew build`.
- Release v0.5.9-alpha: alleen eigen bestanden gecommit en gepusht. Thor/Mjolnir-werk (ook `CapeCloth`, `BodySize`)
  van de andere sessie bleef buiten de commit; in gedeelde bestanden (`MultiversePowers*.java`, `en_us.json`) alleen
  eigen regels.
- CLAUDE.md (lokaal): bot-vergrendel-voorbeeld, nieuwe klassen, hop-regel, yaw-valkuil.
- Open: "rare mob"-plaatje niet exact nagebootst (gevonden oorzaak, yaw, wel gefixt); takgreep niet in game getest.
- Bugs en ideeën (root/GitHub) bewust niet aangeraakt.
