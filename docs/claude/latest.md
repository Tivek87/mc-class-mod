# Laatste sessie

- Datum: 2026-10-02. Verzoek: eigen Thor/Mjolnir-werk committen, pushen en releasen, alleen eigen bestanden.
- Release v0.6.0-alpha: Mjolnir in vanilla pixel-stijl (35 blokjes, 64px), runen gloeien blauw als de hamer geladen
  is, Thor 30% groter (`BodySize`, cape mee), as-stand-in weg, generator `scripts/models/mjolnir/`, 4 referentiebeelden.
- Werkwijze: commit gebouwd in losse worktree vanaf `6676328`: alleen eigen bestanden, in `MultiversePowers*.java`
  en `en_us.json` alleen eigen regels; daar gebouwd, getest, gepusht en gepubliceerd.
- Getest: in-game test (29 shots), `gradlew build` + tests groen op precies deze inhoud.
- Andere sessie: revert + mob-fixes (gestaged/ongestaged) bleven buiten de commit; hun klaargezette release heet
  na deze nu v0.6.1-alpha (CHANGELOG-kop en `mod_version` in index en werkmap omgezet, inhoud ongemoeid).
- Open: niets voor Thor/Mjolnir. Bugs/ideeën bewust niet aangeraakt.
