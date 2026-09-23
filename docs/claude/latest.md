# Laatste sessie

- Datum: 2026-09-23
- Vraag (1): Green Lantern 60% minder power-verbruik; de landingsklap als eigen toets. Daarna (2): power-balk beter (niet knipperen), commit + push; dan krachten-UI: hover 25% langer, characters per franchise (Marvel/DC/Disney/Warner Bros) met alle 27 uit CHARACTERS.md (niet beschikbaar = coming soon), meer QOL, mod hernoemen (jar), commit + push.
- Gedaan (batch 1, getest in-game): alle power-kosten x0,4 met config-migratie (versie 3); nieuwe ability Schokgolf op X (grond: meteen; vliegend: duik; springend/vallend: recht omlaag, geen valschade), 1,6 power, 5 s cooldown; slam-instellingen verhuisd van Vliegen naar Schokgolf.
- Fix: server-`onGround` loopt een tick voor, daardoor startte de klap in de lucht; nu echte grondcheck + klap-pauze laat je niet meer zweven.
- Power-balk: geen knipperen meer; gouden spoor van wat betaald is, glijdt omhoog bij opladen, gestreepte vuistkosten met eindstreepje, kwartstreepjes, verbruik in goud.
- Docs bijgewerkt: GREEN_LANTERN, POWERS, PROJECT, CHANGELOG (ook oude vliegwaarden rechtgezet).
- Nieuw bestand: character/lantern/Shockwave.java; verder GameCharacter, Flight, LandingSlam, PowerRing, ClientFlight, FlightPose, SuitGlow, ConstructHud, RingPayload, CharacterConfig, lang-bestanden.
- Webcheck: Terminator (StudioCanal/Skydance) en Spawn (Image Comics) vallen buiten Marvel/DC/Disney/WB.
- Open: akkoord commit/push batch 1; keuzes voor batch 2 (tab voor Terminator/Spawn, nieuwe modnaam, welke QOL).
