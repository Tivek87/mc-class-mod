# Laatste sessie

- Datum: 2026-10-12 12:45
- Verzoek: Wizard zwakker (700 hp, -40% schade, -70% knockback, -65% armor, ward -55%, pijlen 60% op ward zonder terugkaatsen, Supernova -35%), betere animaties, 14 nieuwe moves, Galactus 100x beter, kosmische nachthemel.
- Wizard: `creature/wizard/` (`close/`, `special/`, `WizardKnock`, `GlacialSpikes`, `MarkedStrike`, `RuneSentry`, keyframes); in game getest (15 moves).
- Nachthemel: `sky/client/`, `CosmicSkyMixin`, `Perlin`, setting `cosmicSky` (standaard aan). Galactus: `galactus/client/` (rig 46 botten, eigen shader). Samen in game getest.
- Bug #75 (sentinel-voeten omhoog): `MinionPose.ready` gefixt, in game getest, gesloten.
- Release 0.9.0-alpha voorbereid: `mod_version`, CHANGELOG, 6 tour-stappen (in game doorlopen), CLAUDE.md layout. Build + tests groen.
- Open: commit + push + publish wacht op ja; niet meecommitten: `CONTENT.txt`, `arena/`, `models/`, `__pycache__/`, `wizard_phases_viewer.html` (andere sessie). Agent-worktrees `.claude/worktrees/agent-*` opruimen na ja.
- Open idee: #24 huurbare companions (low, wacht op ja).
