# Laatste sessie

## Paneel, mech op scrollwiel, plattrappen (sessie "panel")
- Datum: 2026-10-02. Verzoek: A) Thunderclap-bel sneller weg, lichter; B) paneel: alleen bruikbaar, muis alleen naast
  max 2 andere, Thor 1-12 vrij, wapen = andere toetsen dicht; C) mech: alleen scrollwiel, aan/uit met scrollwiel 2 s;
  D) mech trapt kleine zwakke wezens plat; E) reuzenhanden door blokken; F) straal geen as, vliegen 20 b/s.
- Gedaan: `AbilityPanel` (nieuw) + `Rules` (`LanternPanel`, Thor in `ThunderGauge`), weigering `ClientCharacter.refusal`
  + server in `PowerRing`; mech = slot 13 (scrollwiel 2 s), instellingen verhuisd; `MechCrush` + `MechStepPayload`;
  mech-cooldown op paneel (`CharacterPowers.waitLeft`); `HandStop` weg, handen door blokken (nooit water); `ClapFx`;
  `LightBeam` zonder as; topSpeed 20; `Factions`: dodelijke klap geeft geen "hostile"-melding meer. Docs + CHANGELOG.
- Getest: build + tests groen; in-game: panelen Thor/GL/zwaard/mech, R geweigerd met zwaard, mech bouwen/verlaten met
  scrollwiel, 20 van 49 kippen platgetrapt, cooldown 61s zichtbaar. Niet in-game: handen, bel, vliegen, as.
- Release: `release.ps1 prepare` gedaan (0.6.3-alpha); commit, push en publish wachten op ja van de user.
- Open: bugs/ideeën niet aangeraakt (verzoek). Oude `air_strike.mech*`-instellingen gaan terug naar standaard.

## Thor-ontwerp (andere sessie)
- Datum: 2026-10-02. Verzoek: ontwerp voor Thor's vlucht, hamerworp, Throw and Follow (snel naar de hamer) en
  de hamer die in de wereld blijft (grond of lucht), als MD-bestand.
- Gedaan: Thor-code en -docs gelezen; ontwerp `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM.md` (Engels, met bouwplan)
  en korte simpele versie `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM_SHORT.md`. Bugs/ideeën niet aangeraakt (verzoek).
- Besloten (user): Throw and Follow = getimede dash: rechts vasthouden = verder (4-14 blokken), loslaten = gooien,
  0,5 s wachten (bliksemlijn hand-hamer), dash op vast tempo (6 blokken in 0,7 s). Gewone worp max 24 (was 40).
- Bestanden: beide ontwerpen (nieuw), dit bestand. Niets gebouwd, niets gecommit.
- Open: 10 beslissingen in §9 / "Still to decide"; daarna fase 0 bouwen. Mech-wijzigingen en `devtest/` in de
  werkboom zijn van een andere sessie.

