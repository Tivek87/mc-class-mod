# Laatste sessie

- Datum: 2026-09-28.
- Vraag: verder met de mech-aanvallen (v0.4.3-alpha was mid-coding gereleased): in-game testen en bugs fixen.
- In-game getest (tijdelijke testklasse, logs per tik server+client, screenshots): linkerklik start slag; sweep raakt
  3 zombies op volgorde en slingert ze weg; stomp en slam vlakschade; throw pakt, 2x smash, worp vooruit; wezen dat
  sterft bij smash wordt losgelaten.
- Gefixt: worp kapte arm af na loslaten (nu doorzwaai tot tik 66, `MechAttack.carry`); losgelaten wezen sprong op
  scherm terug naar oude serverpositie (`ClientConstructs.walk`: `lerpTo` op vuist).
- Docs: POWERS.md (slagen, "Walking is all it does" weg, instellingen), CLAUDE.md layout (`MechAttacks`).
- Testklasse, wereld, screenshots weg. Build + unit-tests groen.
- Werkkopie had oude versies van CHANGELOG/latest.md/MechBlows (spaties weg): teruggezet naar commit `b7db0ad`.
- Niet van mij, niet aangeraakt: `docs/CHARACTERS.md`, `Roster.java` (gewijzigd in werkkopie).
- Commit + push + release v0.4.4-alpha (CHANGELOG-sectie 0.4.4-alpha).
- Open: ideeën #32 (mech-ogen/borst-straal) en #33 (mech-uiterlijk) wachten op ja/nee van user.
