# Laatste sessie

- Datum: 2026-09-27 (avond).
- Vraag: "commit en push alles, doe daarna verder niks".
- Gedaan: alles van de botten/ragdoll/physics-bouw (fase 1 t/m 12 + ronde 2) in één commit
  'Bones, ragdolls and block-safe hands', gepusht naar origin/master en uitgebracht als v0.3.5-alpha
  (`scripts/release.ps1 prepare` + `CHANGELOG.md`-blok vooraf, `publish` na de push).
- Vooraf gecontroleerd: `gradlew build` groen (68 unit tests), geen namen/e-mails/lokale paden/tokens in de bestanden,
  privé chat-bestand staat in `.gitignore`, geen gemengde regeleinden, geen testklassen of testwerelden meer.
- Bestanden: heel `src/` van deze bouw, `build.gradle`, `gradle.properties`, `CHANGELOG.md`, `CLAUDE.md`, `.gitignore`,
  `docs/claude/vervolg-2026-09-27.md` (overdracht, §1.1 is leidend), dit bestand.
- Open (user wilde nu niets meer): vragen 1-4 uit de vorige beurt (altijd raken / hand-plek, tentakel om doelwit
  wikkelen, mech-stamp, as-dood krachten); daarna bugs #30 (hoog), #28, #27, #29; ideeën #24/#25 wachten op ja/nee.
