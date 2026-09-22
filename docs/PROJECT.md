# Classes & Stamina - overview

## 1. What it is
A Minecraft mod that adds classes and a stamina bar. You pick a class before you start playing.

- [Classes and skill trees](CLASSES.md)
- [Callings and Crowns](CALLINGS.md)
- [Characters and their powers](POWERS.md)
- [Spells](SPELLS.md)
- [Multiverse Characters (Lore)](CHARACTERS.md)

---

## 2. Entering a world for the first time
1. **Out of the world while you choose**
   - You are not in the game yet. Nobody can see you, nothing can hit you, you take no damage.
2. **Welcome screen**
   - **Background**: the dark dirt texture of the Minecraft loading and options screens.
   - **Text**: your player name and the welcome text *"Welkom bij deze test mod"*, in the Minecraft font.
   - **Buttons**:
     - **Leave game**: back to the main menu. The next time you join, you get the screen again.
     - **Continue game**: on to choosing your class.
3. **Choose your group, then your class**
   - All six groups and The Forsaken, every class available. See [the start screen](CLASSES.md#the-start-screen).
4. **Start**
   - You enter the world in your normal game mode, with your starting items. For now that is one bread.
   - A ceremony of up to ten seconds plays around you, different for every class, with your class name on screen.
   - You choose once. Joining again later skips all of this.

---

## 3. Animations
- **Start ceremony**: its own animation for every class. Big the first time, a short one of its own on every respawn.
- **Death**: a one-second animation where you die, one per group.
- **Level up**: the same short animation for everyone when you gain an experience level.
- Players nearby see them too. Everything happens around you, never just in front of you.
- Details per class and group: [Classes and skill trees](CLASSES.md#the-start-screen).
- Operators (or a singleplayer world with cheats on) can play any of them with `/classfx`.

---

## 4. Stamina
- A bar above the hunger bar.
- Sprinting and jumping use stamina. Standing still or walking refills it after a short rest.
- Empty: you cannot sprint or jump until a little has come back. Walking always works.
- All values can be changed in-game from the mod's settings screen, or in `config/welcomescreen/stamina.toml`.
- Not in creative or spectator: no bar, and sprinting and jumping cost nothing.

---

## 5. The wheel: characters and spells
- Hold **G** to open it. The **characters** you can turn into stand side by side at the top; under a
  dividing line sits the table of the ten **schools of magic**, five cards across. Rest the mouse on a
  school for half a second and that school opens as a page of its own with its spells on it. Move the mouse
  onto a character or a spell and let go of G. Letting go over nothing closes the screen; right-click
  or Escape goes back a page.
- The key can be changed in Options > Controls, under "Classes & Stamina".
- Full details: **[Characters and their powers](POWERS.md)** and **[Spells](SPELLS.md)**.

### Characters
- Picking a character turns you into them. Picking the one you already are turns you back.
  Dying or logging out turns you back too.
- **Ability keys are the same for everyone.** There are eleven keys, and they are only numbered: ability
  1 up to ability 11. Each one does whatever the character you are has on that number, so no key is a
  kind of ability. Default: R, V, Z, B, H, N, Y, X, C, Left Alt and K. X is free at the moment.
- A panel in the bottom right shows who you are and what each key does right now, with its cooldown.
- Every character's numbers can be changed in the game: Mods > Classes & Stamina > Config.
- **Crouching + a key** is up to the character: with some abilities it undoes them (let go, put down),
  with others it is a second version of the same ability, and with the rest it changes nothing. The
  panel and [Characters and their powers](POWERS.md) say which is which.

| Character | What they are |
|---|---|
| Doctor Octopus | Four robot tentacles from your back. Walk on your own feet or on 2, 3 or 4 tentacles; every tentacle you do not walk on is free to grab and fight. Always: long reach, tentacle strikes, wall climbing around corners and under ceilings. On keys: grabbing and smashing, Multi-Tentacle, dash, block, ground slam, a ground strike on creatures you mark, portals, and the ultimate Octopus Rampage. |
| Green Lantern | A power ring that shapes green hard light, and puts his uniform on you over your own clothes. The ring holds 100 power and glows as bright as it is full. With empty hands: left click fires energy bolts (3 hearts, 0.3 s auto-fire, 1 power), right click holds a small shield (65% damage reduction, 0.5 power/s). On keys so far: Giant Fist, a fist of light that charges beside you while you hold the key (1 up to 16 blocks across, finding room around you by itself), costs ring power, and rams everything in its way, straight through walls and the ground; Recharge, where you raise your lantern and punch it to fill the ring; and the Construct Wheel, twelve slots kept free for hard-light weapons (the picking works, every slot is still a placeholder; left click will attack, right click defend). More powers are on the way. |

- Every cooldown and damage number can be changed per character and per ability: every character has
  their own settings file in the `config/welcomescreen` folder. See
  [Characters and their powers](POWERS.md#changing-the-numbers).

### Spells
- Anyone can cast every spell, whatever their class or who they are. Spells cost nothing, but each has a
  cooldown; the wheel shows how long is left.

| School | Spell | What it does | Range | Cooldown |
|---|---|---|---|---|
| Fire | Fireball | A blazing comet that bursts in fire, burns what it hits and leaves a small pile of fire. | until it hits | 2 s |
| Fire | Fire Wall | A ring of fire around you for 2 seconds: it burns what touches it, sets it alight and throws it outward. You stay safe inside. | 3.5 blocks around you | 10 s |
| Lightning | Lightning Strike | A rune circle and storm cloud charge up for 0.7 s, then a branching bolt strikes. Never hits you. | 40 blocks | 8 s |
| Nature | Poison Area | A thrown vial that leaves a poison cloud for 8 seconds. Poisons everything inside except you. | 24 blocks | 12 s |
| Air | Wind Gust | A wall of wind rolls forward and throws creatures back and up. | 8 blocks | 5 s |
| Dark | Void Walk | 10 seconds invisible, silent and untargetable; 50% faster, 20% harder hits; your world turns black and nearby enemies are marked, even through walls. | around you | 30 s |

- The four empty schools (Water, Holy, Ice, Blood) and Earth are in the table already, ready for the
  spells that go in them later.
