# Laatste sessie

- Datum: 2026-09-29 (2e sessie). Verzoek: "stretch" bij ragdolls fixen (husk op screenshot); user: knik moet VIERKANT,
  niet rond, niet uitgerekt.
- Oorzaak: fysica houdt gewrichtslimieten (gemeten, max ~0.16 rad over); de tekening (`BentParts`) draaide het snijvlak
  maar half mee -> bij knie/elleboog ~2.3-2.6 rad een dunne punt (haakvorm), taille scheef uitgesmeerd.
- Gedaan: `BentParts` herschreven: helften blijven hele blokken, verstek-hoek tot 90 graden, daarboven platte blokeinden;
  twist verdeeld over beide helften (benen blijven aan heupen). Geldt voor alle knikken (ook taille, poses, GetUp).
- Test: nieuw `BentPartsTest` (volume dicht, vierkante hoeken, twist, romp); build + 116 tests groen; in-game oud/nieuw
  vergeleken + dode husks in geul/gat/muur/trede + taille-knikken. Testklassen, werelden, shots verwijderd.
- Ook nog niet gecommit (vorige sessie, getest): lijken zakken pas weg na volle stilte (`Corpses`, `RagdollFalls`,
  `Ragdolls`, `Ragdoll`, `ClientSettings`, `en_us.json`, `CLAUDE.md`). `MechScript.java` (andere sessie) blijft erbuiten.
- Open: commit + push (= release) wacht op ja; changelog-regels (beide wijzigingen) bij `release.ps1 prepare`.
