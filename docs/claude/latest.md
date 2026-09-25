# Laatste sessie — 2026-09-25 (avond, 3)

- **Eerder vandaag:** theme alleen in hoofdmenu, release v0.0.4-alpha (`7c018d0`).
- **Vraag:** in-game updatesysteem: elke 5 min checken, pling + popup, "Update later" (= installeren bij afsluiten) of "Update & restart", mooie changelog; daarna: manager altijd via aanpasbare toets, icoon weg, alles minimaler.
- **Gedaan:** `update/client/`: `UpdateChecker` (302 van `releases/latest`, API alleen bij nieuwe tag), `UpdatePopup` (kaartje + toets U in Controls), `UpdateManagerScreen` (Installed/Newest/Status, Check now), `ChangelogScreen`/`ChangelogLayout` (markdown per versie), `UpdateInstaller` (download, SHA-256 + jar-check, plan-bestand), `UpdateHelper` (los Java-programma: wacht op exit, wisselt jar, start opnieuw via `java @argfile`), `Relaunch` (startcommando herbouwen; Prism/MultiMC → "Update & close").
- **Getest (dev, 3 eind-tot-eind-runs):** popup + 2-tonige pling, klik, manager, changelog (3 versies, scroll), echte download v0.0.4 + checksum, jar-wissel in nep-modsmap, game herstart zichzelf; in de herstarte game: in-game popup, U zonder/met update, Check now, Update later → READY. Bugs gevonden en opgelost: relatief update-pad, `updater.jar` op slot door vorige helper (nu `updater-<pid>.jar`).
- **Niet getest:** echte launchers (Minecraft Launcher, Modrinth, CurseForge); alleen dev-omgeving.
- **Docs:** README, `docs/PROJECT.md`, CLAUDE.md (code layout `update/client/`).
- **Git:** gecommit en gepusht als release v0.0.5-alpha. Pas vanaf v0.0.5 werkt het updaten; echte test = v0.0.5 → v0.0.6. Eerder open: blokkeren tijdens uitrusten Sword & Shield t46-t71.
