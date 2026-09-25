# Laatste sessie — 2026-09-25 (avond, 6)

- **Vraag:** in-game bugrapport via de update manager (naam, beschrijving, prioriteit, MC-naam erbij) -> `bugs/` in de root, Claude leest en fixt op prioriteit. VPS-idee geschrapt: Cloudflare Worker.
- **Gebouwd:** `bugreport/client/` (`BugReportScreen`, `BugReporter`, leest relay-adres uit `relay.txt`), knop "Report a bug" naast "What's new"; `scripts/bug-relay/` (worker.js + wrangler.jsonc, rate limit 3/min per IP); `scripts/bugs.ps1` (sync/fixed/close/schedule/setup); `release.ps1 publish` sluit gefixte issues; CLAUDE.md "Bug reports"; `.gitignore` `bugs/`; README, docs/PROJECT.md.
- **Getest:** compile ok; sync met nep-GitHub ok; relay lokaal (wrangler dev + nep-GitHub): 201/400/404/405/429, @-pings onschadelijk; in-game test: alle schermstaten goed, ook klein venster. Testklasse/shots verwijderd.
- **Let op:** Node 20 op deze PC -> wrangler vast op 4.86.0 (nieuwere willen Node 22), compatibility_date 2026-05-01.
- **Setup run 1:** login ok, token staat als secret bij Cloudflare, deploy faalde: account had nog geen workers.dev-subdomein. Setup nu herstartbaar (slaat token over als secret bestaat, opent onboarding-pagina, vraagt y/n of het adres niks persoonlijks bevat, checkt labels van testmelding). Relay voegt labels achteraf toe als GitHub ze laat vallen (lokaal getest).
- **Setup klaar:** subdomein `tivek87` (Cloudflare koos eerst iets van de e-mail, afgewezen), relay live op `https://mc-class-mod-bugs.tivek87.workers.dev/report`, `relay.txt` geschreven en in de jar; live test issue #3 met labels, gesloten; taak "mc-class-mod bug sync" elke 5 min (eerste run ok). `preview_urls: false`.
- **Release:** 0.0.8-alpha, CHANGELOG-sectie; privacy-scan schoon; gecommit, gepusht en gepubliceerd op verzoek van de gebruiker.
