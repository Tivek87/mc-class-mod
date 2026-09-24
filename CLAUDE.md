# mc class mod

## Design rules
- Hands, as in vanilla: attacks use the right hand and left click, defence uses the left hand and right click. Green Lantern: ring on the right middle finger, fist charges on the right, lantern in the left hand; every construct follows this.
- Green Lantern's constructs are ALWAYS solid, like the Giant Fist (R): an opaque green mass with bright edges, never see-through or transparent. They appear by growing out of the ring's light and disappear by breaking into solid pieces or sinking away, never by fading. Only exception: your own shield, dome and ram cone stay see-through in your own first-person view (and a dome for anyone looking from inside it), because they would block your sight. Light effects (the beam from the ring, flares, cracks, the flight streak) are not constructs.
- Landing-slam constructs must make a logical shockwave: something that drops, claps shut, bursts out of the ground or is swung down onto it. Nothing that sweeps in from the side.
- The 32 slam constructs are only ever for the shockwave: the dive from the air and the normal ground slam. Never use them for anything else (another ability, an ultimate, a HUD).
- Every aircraft, jet and machine Green Lantern makes is always a pure hard-light construct, shaped out of the ring's green light, never a real vehicle.

## Language
- The mod is English only: every in-game text lives in `en_us.json`, never add another language file.

## Engine
- The engine (everything in `engine/`: `ConstructPainter`, `Mesh`, `Material`, `Effects`, `Cooldowns` and the rest) may always be updated and extended on your own. Extensions or additions around the character itself (new abilities, new constructs, changes to how Green Lantern plays) always need the user's permission first.
- Anything a second power could use belongs in `engine/`, and `engine/` never names a character, spell or class: content brings only what is its own. Colours go through a `Material`; a character's own shapes go in a painter subclass (Green Lantern: `LanternPainter`).
- Lasting server work is an `Effect` started with `Effects.start` (never a tick loop of its own); cooldowns use `Cooldowns`; a character plugs in through `CharacterPowers`, a spell through its entry in `Spell`, so nothing switches on who is who. State kept per server is dropped in the one list in `MultiversePowers.onServerStopping`.
- An engine optimisation must leave what is drawn exactly the same; check it with a checksum over the painter's layers before and after, not only by eye.

## Code layout
- Package `nl.tivek.multiversepowers`; the mod id stays `welcomescreen` (worlds, settings files, keys and saved data carry it).
- `engine/`: `effect/` (Effect, Effects), `ability/` (Cooldowns), `entity/` (HeldMobs), `fx/` (ParticleFx), `target/` (Targeting), `math/` (Ease, Noise, Colors, Vectors), `client/render/` (ConstructPainter, Mesh, Material), `client/gui/`.
- `character/`: the roster (`GameCharacter`), keys, cooldowns and the panel (`Characters`, `client/`), and one folder per character. `greenlantern/` holds the ring and its payloads, `ability/` (server), `client/` (state), `client/render/` (LanternPainter and every hard-light painter), `client/slam/` (the 32 slam constructs), `client/body/` (suit, ring, poses), `client/hud/`.
- `spell/`, `classes/` (`ceremony/`: one file per group), `stamina/`, `config/`, `network/` (`ModNetwork` registers every payload; each payload class lives with its feature), `registry/`, `mixin/`.
- Client-only code always sits in a package named `client`, so a dedicated server never loads it.

## Construct drawing
- Player model arms: a `ModelPart` turns about x first, then about z, so an arm raised overhead (xRot near -π) spreads outward with the opposite zRot sign from a hanging arm (the raised right arm goes out with a negative zRot).
- A `ConstructPainter.Frame` built as (right = forward × UP, UP, forward) is left-handed; `Frame.turned()` corrects the angle for that, so reason in the model's own axes: about +x by +φ lifts the -z edge up, about +y by +θ moves +x towards -z. Round parts are a `Mesh` (lathe, torus, tube), angular parts boxes. Timelines count in ticks at the pace the constructs were made for (`SlamPainter.pace`), sizes at scale 1 times `Moment.size`.

## In-game tests
- Automated test: temporary client `@EventBusSubscriber` that opens a copy of a world (`createWorldOpenFlows().openWorld(...)`), sends commands, holds keys with `KeyMapping.setDown`, takes `Screenshot.grab` shots and ends with `mc.stop()`. Afterwards delete the class, the world copy and the screenshots.
- GUI scale is capped by the window size (320x240 px per step, a higher `guiScale` is refused): resize the window first, then set `guiScale`, then `resizeDisplay()`. Smallest GUI: window 854x480 + guiScale 2 (427x240).
- No world on disk: make one with `createFreshLevel` (flat, SURVIVAL: a creative world leaves creative flying on after the welcome screen's spectator mode, which bypasses ring flight). Set `options.pauseOnLostFocus = false` or the pause menu covers the screenshots. Compare many screenshots on one contact sheet (PowerShell System.Drawing).
- `options.hideGui` (F1) also hides your own first-person hand: keep the GUI on for first-person hand shots. Log the camera type with every shot; it can change during a run (F5), so check it before trusting a shot.
- Before every run, tell the user in one line not to click or type in the game window. Outside input still happens: the test class sets the camera type and the look direction again every tick, and closes (and logs) any screen that opens by itself. Check the screenshots for interference (open screen, moved camera, F3/F1, a character switched off) before trusting them.
- Test tools: run commands on the server thread with `server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), ...)` (needs no cheats); a single press is `KeyMapping.click(key)` (`setDown` gives no click); point the mouse in a screen by setting `MouseHandler` `xpos`/`ypos` through reflection (the real cursor stays put); a `ServerTickEvent.Post` handler in the test class traces the server side per tick; `ClientCommandHandler.runCommand("name")` runs a client-only command; loop over a copy of `screen.children()` when a test changes a screen's widgets. Only one temporary test class may start at the title screen: move the others out before a run.
- The server's `onGround()` of a player runs a tick ahead: every tick it moves him one step by itself, puts him back where his client says, but keeps whether that step hit ground. For "is he really standing" check for a collision just below his bounding box.
- A temporary test class only runs while a flag file exists (e.g. `run/claude_engine_test.flag`) and deletes it at its start, so it never takes over a game the user starts himself.
- The ability key Left Alt also fires on Alt+Tab (the game still sees the Alt): before trusting a shot of the Lantern Flare, check on the panel that it was not already on cooldown.
- A refactor that must keep the game the same: run the same test on the original code too (`git archive HEAD` into the scratchpad, copy `run/options.txt` and `run/config` into its `run`, `gradlew runClient` there) and compare the screenshots pair by pair. The painter cannot start outside the game (NeoForge wants its loader), so checksum or time it inside a dev client, loading the old classes through a `URLClassLoader`.
