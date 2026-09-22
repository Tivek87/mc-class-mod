# mc class mod

## Design rules
- Hands, as in vanilla: attacks use the right hand and left click, defence uses the left hand and right click. Green Lantern: ring on the right middle finger, fist charges on the right, lantern in the left hand; every construct follows this.

## In-game tests
- Automated test: temporary client `@EventBusSubscriber` that opens a copy of a world (`createWorldOpenFlows().openWorld(...)`), sends commands, holds keys with `KeyMapping.setDown`, takes `Screenshot.grab` shots and ends with `mc.stop()`. Afterwards delete the class, the world copy and the screenshots.
- GUI scale is capped by the window size (320x240 px per step, a higher `guiScale` is refused): resize the window first, then set `guiScale`, then `resizeDisplay()`. Smallest GUI: window 854x480 + guiScale 2 (427x240).
- Before every run, tell the user in one line not to click or type in the game window. Check the screenshots for interference (open screen, moved camera, F3/F1) before trusting them.
