# Laatste sessie

- Datum: 2026-10-14 00:01
- Verzoek: factie-tag boven hoofden weg; Wizard-staf vecht niet meer zelf (alleen gooien, pastimes blijven); Blaze-effecten vanilla-vriendelijk; Stone Heave trekt het echte blok onder/naast je uit de grond (gat blijft); daarna alle 15 wilde dieren opnieuw (modellen, animaties). Twee releases: 0.9.16 en 0.9.17.
- Release 1 (0.9.16): `FactionTags` en de instelling weg (`ClientFactions.of` voor het Faction Book); `StaffActs` weg, `WizardStaff` modes 0-5 (gooien, vast, terug, planten, leunen, zitten), geen eigen balk of breken; `Energy` shader nu in pixels (4 tinten, 10 fps), Additive-gloed weg uit Blaze, ray en vuurbal; `BlazeCasts.heave` breekt het echte blok (mobGriefing/`BlockRules.mayBreak`), `HeavedStone` schudt los en rijst eruit; staf zonder grond: melding, geen cooldown.
- Getest in het spel: gat in de vloer, geen grond = false, Wizard gooit en roept staf terug zonder crash, pixel-effecten op de beelden, tour 4 kaarten. `./gradlew build` groen. Niet getest: multiplayer, Blaze met mobGriefing uit, beschermde plek.
- Volgende: commit + push + publish v0.9.16-alpha, daarna release 2: de 15 dieren.
- Eerder vandaag: v0.9.14-alpha en v0.9.15-alpha gepubliceerd (facties, target lock, Blaze).
- Open punten voor de gebruiker: lijst-regels in formulieren blijven 300 tekens (relay moet anders opnieuw uitgerold), factietekst blijft 300.
