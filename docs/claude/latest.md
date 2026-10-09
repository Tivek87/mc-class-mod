# Laatste sessie

## 0.7.9-alpha (2026-10-10, release)
- 0.7.9 klaar: Storm Throw lager + mikt op dichtstbijzijnde vijand, mech (raketten, vlammenwerper V, sprong,
  3 helpers Left Alt, GL-krachten uit), pak over armor, Thor (dash naar vliegende hamer, reach 128, arm na dood,
  hamer-bliksem keten 3, chain 4 blokken), Giant Hands max 5, Maw en Cosmic rift, zachte gloed.
- CHANGELOG, tour (9 stappen, in game gelopen), gids en docs bijgewerkt; volledige diff gereviewd.
- In game getest: alles hierboven behalve GL-krachten geweigerd in de mech (alleen code gelezen).
- Zachte gloed (2026-10-10): A/B-test liet grondringen bijna verdwijnen. Fix: shader vervaagt nu alleen richting de
  snijlijn met een blok, over een deel van de breedte van de gloed (max 1 blok); ringen (`circle`) eigen laag
  `ringGlow`, scherp zoals vroeger. In game getest: ringen gelijk aan/uit, straal en grote gloed zacht in de grond.
- Testklassen, testwerelden en shots verwijderd; `run/options.txt` goed. `./gradlew build` groen.
- Ja van gebruiker (2026-10-10): commit 'Maw, cosmic rift, mech helpers and hammer lightning', push naar
  origin/master, `scripts/release.ps1 publish` v0.7.9-alpha (Latest); CHANGELOG-kop op 2026-10-10.
- Open: idee #24 (NPC's) wacht op ja/nee.
