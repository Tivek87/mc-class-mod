# mc class mod

## Design rules
- Hands, as in vanilla: attacks use the right hand and left click, defence uses the left hand and right click. Green Lantern: ring on the right middle finger, fist charges on the right, lantern in the left hand; every construct follows this.
- Green Lantern's constructs are ALWAYS solid, like the Giant Fist (R): an opaque green mass with bright edges, never see-through or transparent. They appear by growing out of the ring's light and disappear by breaking into solid pieces or sinking away, never by fading. Only exception: your own shield, dome and ram cone stay see-through in your own first-person view (and a dome for anyone looking from inside it), because they would block your sight. Light effects (the beam from the ring, flares, cracks, the flight streak) are not constructs.
- Landing-slam constructs must make a logical shockwave: something that drops, claps shut, bursts out of the ground or is swung down onto it. Nothing that sweeps in from the side.

## In-game tests
- Automated test: temporary client `@EventBusSubscriber` that opens a copy of a world (`createWorldOpenFlows().openWorld(...)`), sends commands, holds keys with `KeyMapping.setDown`, takes `Screenshot.grab` shots and ends with `mc.stop()`. Afterwards delete the class, the world copy and the screenshots.
- GUI scale is capped by the window size (320x240 px per step, a higher `guiScale` is refused): resize the window first, then set `guiScale`, then `resizeDisplay()`. Smallest GUI: window 854x480 + guiScale 2 (427x240).
- No world on disk: make one with `createFreshLevel` (flat, SURVIVAL: a creative world leaves creative flying on after the welcome screen's spectator mode, which bypasses ring flight). Set `options.pauseOnLostFocus = false` or the pause menu covers the screenshots. Compare many screenshots on one contact sheet (PowerShell System.Drawing).
- `options.hideGui` (F1) also hides your own first-person hand: keep the GUI on for first-person hand shots. Log the camera type with every shot; it can change during a run (F5), so check it before trusting a shot.
- Before every run, tell the user in one line not to click or type in the game window. Check the screenshots for interference (open screen, moved camera, F3/F1) before trusting them.
