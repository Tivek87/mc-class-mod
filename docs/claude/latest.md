# Laatste sessie

- Datum: 2026-10-14 11:50
- Verzoek: factie-tag boven hoofden weg; Wizard-staf vecht niet meer zelf (alleen gooien, pastimes blijven); Blaze-effecten vanilla-vriendelijk; Stone Heave trekt het echte blok onder/naast je uit de grond (gat blijft); daarna alle 15 wilde dieren opnieuw (modellen, animaties). Twee releases: 0.9.16 en 0.9.17.
- Release 1 (0.9.16): `FactionTags` en de instelling weg (`ClientFactions.of` voor het Faction Book); `StaffActs` weg, `WizardStaff` modes 0-5 (gooien, vast, terug, planten, leunen, zitten), geen eigen balk of breken; `Energy` shader nu in pixels (4 tinten, 10 fps), Additive-gloed weg uit Blaze, ray en vuurbal; `BlazeCasts.heave` breekt het echte blok (mobGriefing/`BlockRules.mayBreak`), `HeavedStone` schudt los en rijst eruit; staf zonder grond: melding, geen cooldown.
- Getest in het spel: gat in de vloer, geen grond = false, Wizard gooit en roept staf terug zonder crash, pixel-effecten op de beelden, tour 4 kaarten. `./gradlew build` groen. Niet getest: multiplayer, Blaze met mobGriefing uit, beschermde plek.
- v0.9.16-alpha gepubliceerd (commit `91830d3`, Latest).
- Release 2 (0.9.17): helpers `Gait`, `Strides`, `Channels.blink`/`springs`; alle 15 wilde dieren opnieuw (botten, knipperende ogen, voeten blijven staan). Aap met capucijnengezicht, olifant langer en groter (hitbox 2.4x3.4). Alle 15 in het spel bekeken (stil, lopen, rennen, acts). `./gradlew build` groen, tour 1 kaart (wild_animals) in het spel gelopen.
- Testklasse, testwereld en screenshots verwijderd; tour.json teruggezet.
- Eerder vandaag: v0.9.14-alpha en v0.9.15-alpha gepubliceerd (facties, target lock, Blaze).
- Open punten voor de gebruiker: lijst-regels in formulieren blijven 300 tekens (relay moet anders opnieuw uitgerold), factietekst blijft 300.
