# Laatste sessie

- Datum: 2026-09-23
- Vraag: Green Lantern 60% goedkoper + schokgolf als toets; power-balk beter; krachten-UI met franchise-tabs (hover 25% langer), alle 27 characters (niet beschikbaar = coming soon); mod hernoemen (jar); commit + push per stap. Extra QOL: gebruiker koos "niks".
- Batch 1 (commit 150e853, gepusht): schokgolf op X (grond/duik/val, geen valschade), alle ringkosten x0,4 met config-migratie, power-balk zonder knipperen (gouden spoor, gestreepte vuistkosten), fix klap-in-de-lucht door server-onGround.
- Batch 2: krachtenscherm herbouwd: hoofdpagina met franchisekaarten (Marvel, DC, Disney, Warner Bros., Overig) en de 15 magiescholen; stilhouden opent na 381 ms (was 305); een kaart wacht pas na een muisbeweging (voorkomt heen-en-weer); coming-soon-kaarten gedimd en niet hoverbaar; lange namen met "…"; donkerdere achtergrond; ability-paneel verborgen onder het scherm.
- Webcheck (Wikipedia): Terminator = StudioCanal/Skydance, Spawn = Image Comics -> tab Overig; Rick and Morty = Adult Swim (Warner Bros. Discovery).
- Mod heet nu Multiverse Powers, jar multiverse-powers-1.0.0.jar; interne id blijft welcomescreen (werelden, config, toetsen blijven werken).
- Bestanden: nieuw client/character/Roster.java; PowerWheelScreen (herschreven), ClientCharacter, lang en_us/nl_nl, build.gradle, gradle.properties, docs (POWERS, SPELLS, PROJECT, CHANGELOG).
- Getest in-game met screenshots (ook kleinste GUI 427x240); build slaagt.
- Open: akkoord voor commit + push van batch 2.
