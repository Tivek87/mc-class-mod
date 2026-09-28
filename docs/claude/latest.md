# Laatste sessie

- Datum: 2026-09-28.
- Vraag: config client+server veel groter (effecten, particles, meer), server-config alleen voor eigenaar/host (geen ops),
  mod optimaliseren zonder kwaliteitsverlies (mech), mech-QOL (zaklamp op torso), mech-handen clippen bij klap.
- Gedaan: eigenaar-lock (`owners` in general.toml), 45 client-instellingen (7 secties, zelfbouwende pagina),
  wereldregels (kosten, PvP, effectbereik, knockdown, personages/spreuken aan-uit), Thor/Doc Ock/mech-instellingen,
  spells.toml; klap-fix (vingers stoppen in het midden, MechClapTest); zaklamp (Spotlight); painter sneller.
- FPS mech 1e persoon (1280x720): 300 -> 490 gem., 99e pct 115 -> 285; A/B: 540 checks byte-identiek.
- Build + 77 tests groen; in-game getest: klap, lamp, FPS, instellingenschermen. Niet getest: multiplayer-eigenaar.
- Rooktest na de laatste refactors in orde; commit + push naar origin/master en release v0.3.9-alpha (op ja).
- Open: ideeen #24/#25/#31 wachten op ja/nee; eigenaar-lock nog niet op een echte server getest.
