# Laatste sessie

- Datum: 2026-10-13 14:15
- Verzoek: feedbackpagina herbouwen (bug + idee in inklapbare secties, tooltips, idee-categorieën, volledig New mob-formulier), uitbrengen als v0.9.10-alpha.
- Gedaan: formulier-engine `bugreport/client/form/` (Form, Section, Field, Values, FormBoxes, FormView, FormDraw, Block); `FeedbackForms` (bug, categorieën, No category), `MobIdea`, `ReportPreview` (controle voor Submit); dubbel versturen geweigerd.
- Categorieën: alleen New mob en No category open; 9 andere op slot ("Coming soon").
- Relay (`scripts/bug-relay/worker.js`) maakt nette tabellen/secties; oude modversies werken nog. Gedeployed naar Cloudflare (met ja gebruiker).
- Versie: `release.ps1` telt nu 0.9.9 -> 0.9.10 -> 0.9.11 (geen 1.0.0); regel in `CLAUDE.md` aangepast.
- Tour: stappen `feedback_forms` en `idea_categories`; in game doorgelopen.
- Getest: in game (formulier, tooltips, preview, versturen naar lokale relay, Submitted, kleine GUI), `gradlew build` slaagt.
- Open: bugs #76 (medium), #77 (low); ideeën #78, #24 alleen na ja. Andere idee-categorieën bouwen als gebruiker wil.
