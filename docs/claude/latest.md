# Laatste sessie

## Paneel rechtsonder: alleen bruikbare toetsen, korte muisnamen (2026-10-03)
- Verzoek: paneel toont alleen toetsen/knoppen met een ability; muisknoppen kort (RMB).
- `AbilityPanel`: vrije slots (spare) en ongebonden toetsen weg; "free"-regel en lang-key weg.
- `PowerInputs`: `bound()` + `keyName()` (LMB/RMB/MMB/M4, lang `input.welcomescreen.mouse*`), ook in instellingen.
- Doc Ock: `placeholder`/`placeholder_2` nu `.spare()` (geen rij, geen sectie in instellingen, geen melding);
  dode case in `OctopusArms` + `octopus.welcomescreen.placeholder` weg. `docs/POWERS.md` bijgewerkt.
- Getest: build + tests groen; in-game: Doc Ock zonder Placeholder-rijen, GL toont `[Hold MMB] Mech`.
- Gecommit + gepusht (`54ba3af`), release v0.6.5-alpha (Latest).
- Daarna: alle bruikbare gebaren tonen. `MOUSE_BESIDE`-regel uit `AbilityPanel` weg, Thor-regel (`!onGesture`) uit
  `ThunderGauge` weg; `docs/POWERS.md`, `docs/PROJECT.md`. Getest in worktree (andere sessie had main kapot:
  `FlightPose` e.a., niet van mij): Thor 8 rijen (LMB/RMB/MMB/Space), GL met LMB/RMB-rijen.
- Gecommit + gepusht (`f97a8be`), release v0.6.6-alpha (Latest), gepubliceerd via worktree; jar in `releases/`.
- Daarna QoL: namen volgen staat (`ThorPanel` hamer, `OctoPanel` stance, `LanternPanel` Stop Flying;
  `ClientCharacter.flies`/`legs` public); toets P `AbilityGuide` + `AbilityGuideScreen` (P deelt met vanilla Social
  Interactions, `sharedOnPurpose`); settings-tooltip op ability-titel (`Section`/`Block.about`); 39 `.desc` teksten
  in `en_us.json`; "[P] guide" op paneel. Docs `POWERS.md`, `PROJECT.md`, `CLAUDE.md`.
- Getest in worktree: build + tests groen; in-game P opent gids (social-klik gewist), Thor-namen wisselen met hamer,
  gids Thor/GL scrollt, settings-hover toont uitleg. Main compileert weer.
- Gecommit + gepusht (`b80e83e`), release v0.6.7-alpha (Latest), via worktree.
- Daarna UI-redesign (gids + paneel): `AbilityGuideScreen` twee panelen (lijst met statusstip, pijltjes/W/S/Home/End,
  klik), `GuideDetail` (keycaps, tags, uitleg, status + reden), `KeyCap` (pixel-keycaps, chips, `Layer`),
  paneel als afgeronde kaart met keycaps + cooldownbalk; `Rules.unavailable` (Thor: hamer/rennen).
  Getest in worktree (main kapot door andere sessie, `RechargeAnimation`): kleinste + grote GUI, cooldown zichtbaar.
- Daarna modes in de gids: `GuideMode` (+ `AbilityGuide.modes`), `LanternGuide` (10 modes), `ThorGuide` (5),
  `OctoGuide` (7); 120 bedieningsregels in `en_us.json` (`guide.welcomescreen.*`), onderzocht door subagent.
  Getest in worktree: geen ontbrekende teksten, mech/zwaard/vlucht/vuisten/vasthouden zichtbaar, actieve mode groen.
- Gevonden, niet gefixt: `ConstructWheel` stuurt `mech_busy` zonder `%s` (rauwe %s); paneel toont mech-LMB niet.
- Gecommit + gepusht, release v0.6.8-alpha (via worktree). Bugs #59-#63 (high) + 5 ideeën open.

## Thor-ontwerp (andere sessie, 2026-10-02)
- Ontwerp `docs/ideas/THOR_MJOLNIR_FLIGHT_SYSTEM.md` + `_SHORT.md` (niet gecommit, van die sessie). Besloten:
  Throw and Follow = getimede dash, gewone worp max 24. Open: 10 beslissingen in §9; daarna fase 0 bouwen.

