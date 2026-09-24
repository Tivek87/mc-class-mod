# Laatste sessie - 2026-09-24

Vraag: Green Lantern afmaken: Lichtbel (K), vliegtuig-ultimate (gunship), ring-gloed, animaties overal, zwaard & schild (wiel slot 1).

Gedaan:
- Alles gebouwd (vorige context) en nu in-game getest met automatische testrun (3 runs, screenshots bekeken).
- Fix: first-person armen met zwaard/schild waren gigantisch -> reiken nu vanaf de schouder buiten beeld, handen verder weg.
- Fix: gevangen wezen bleef hangen als de bel neergesmeten werd -> client zet het wezen elke tick in de bel.
- Fix: hold-label bij zwaard toonde "Koepel" -> nu "Steekregen" / "Stormloop".
- Fix: oude opgeslagen instellingen (10 s aanval, raket elke 10 ticks) -> DEFAULTS_VERSION 5, migreert naar nieuwe waarden.
- Docs: CHANGELOG, PROJECT.md, POWERS.md, GREEN_LANTERN.md.

Bestanden deze ronde: SwordArms, RechargeAnimation, ClientConstructs, LightBubble, ConstructPayload, ConstructHud, CharacterConfig, lang en/nl, docs.

Gecommit en gepusht: 7425202 naar origin/master. Open: niets; niet getest: multiplayer, echte muisklikken, geluid.
