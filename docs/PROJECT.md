# Multiverse Powers - overview

## 1. What it is
A Minecraft mod that adds classes, a stamina bar, spells, and characters from across the multiverse you can
turn into. You pick a class before you start playing. The jar is `multiverse-powers-<version>.jar`; inside the
game the mod's id is still `welcomescreen`, so worlds, settings and keys carry over. Its own multiverse theme
plays over and over in the main menu, in place of Minecraft's menu music; in a world the normal music plays. The
music slider sets its volume.

The mod keeps itself up to date: a few seconds after the game starts, and then every five minutes, it looks on its
release page for a newer version. When there is one you hear a pling and a small card slides in at the top right. In
the game it shows for 15 seconds; on the title screen and in the pause menu it stays and you click it. The update key
(**U**, change it under Controls) opens the update manager at any time in the game: the installed and the newest
version, a status line, **What's new** (the notes of every newer version, laid out by section, and under them the notes
of the version you have, marked INSTALLED) and **Check now** when you are up to date. With an update: **Update later** (downloads now, and the new version is put in place when you close the
game) and **Update & restart** (closes the game, saving your world, and starts it again on the new version). Launchers
that cannot be restarted from the game (Prism Launcher, MultiMC) get **Update & close** instead: start the game again
yourself. Every download is checked against the release page's checksum before it is used. **Report a bug** (next to
**What's new**) sends a bug or glitch straight to the mod's makers: give it a short name, a description and a priority
(low, medium or high) and press **Send**; your Minecraft name and version go with it, and you get the report's number.
**Suggest an idea** (next to **Report a bug**) works the same way for your own ideas: a short name, a description, a
category (new power, new character, change or other) and a priority. Reports and ideas are public on the mod's GitHub
page.

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
- All values can be changed in-game from the mod's settings screen, or in the world's
  `serverconfig/welcomescreen/stamina.toml`: they are world settings, so every world keeps its own and on a
  server everyone runs on the server's.
- Not in creative or spectator: no bar, and sprinting and jumping cost nothing.

---

## 5. The wheel: characters and spells
- Hold **G** to open it. At the top are the **franchises** of the characters (Marvel, DC, Disney, Warner Bros.
  and Other); under them the fifteen **schools of magic**, as the same cards. Rest the mouse on a franchise or
  a school for 0.38 seconds and it opens as a page of its own with its characters or spells. Move the mouse
  onto a character or a spell and let go of G. Characters that are not in the game yet say "Coming soon" and
  cannot be picked. Letting go over nothing closes the screen; right-click or Escape goes back a page.
- The key can be changed in Options > Controls, under "Multiverse Powers".
- Full details: **[Characters and their powers](POWERS.md)** and **[Spells](SPELLS.md)**.

### Characters
- Picking a character turns you into them. Picking the one you already are turns you back.
  Dying or logging out turns you back too.
- **Ability keys are the same for everyone.** There are eleven keys, and they are only numbered: ability
  1 up to ability 11. Each one does whatever the character you are has on that number, so no key is a
  kind of ability. Default: R, V, Z, B, H, N, Y, X, C, Left Alt and K. Keys a character has nothing on yet
  say so when you press them. The one exception to "no key is a kind": Y is always the character's
  ultimate, its biggest power with a long cooldown; while it goes, the panel's title turns red and counts down.
- A panel in the bottom right shows who you are and what each key does right now, with its cooldown.
- Every character's numbers can be changed in the game: Mods > Multiverse Powers > Config, with a tab per
  character, a part per ability that folds open and shut, and a search box that looks through everything.
- **Crouching + a key** is up to the character: with some abilities it undoes them (let go, put down),
  with others it is a second version of the same ability, and with the rest it changes nothing. The
  panel and [Characters and their powers](POWERS.md) say which is which.

| Character | What they are |
|---|---|
| Doctor Octopus | Four robot tentacles from your back. Walk on your own feet or on 2, 3 or 4 tentacles; every tentacle you do not walk on is free to grab and fight. Always: long reach, tentacle strikes, wall climbing around corners and under ceilings. On keys: grabbing and smashing, Multi-Tentacle, dash, block, ground slam, a ground strike on creatures you mark, portals, and the ultimate Octopus Rampage. |
| Green Lantern | A power ring that shapes green hard light, and puts his uniform on you over your own clothes. The ring holds 100 power and glows as bright as it is full. With empty hands: left click fires energy bolts from your outstretched ring arm (3 hearts, 0.3 s auto-fire, 0.16 power), right click puts up a small round shield (70% damage reduction, 0.08 power/s); holding the button charges up a beam (the energy fills your suit first) or gives a dome. On keys so far: Giant Fist, a fist of light that charges beside you while you hold the key (1 up to 5.7 blocks across, finding room around you by itself), costs ring power, and once let go stays right under your crosshair, steered by where you look, ramming everything in its way, straight through walls and the ground; Recharge, where you raise your lantern and punch it to fill the ring; Flight (the flight key, or tap jump twice; in the air a double tap turns it off again), which picks up to a cruise within half a second and then keeps getting faster for a few seconds more, with the world made ready round you and far ahead so you never stop dead at the edge of new land, where the shield becomes a ram cone (scraping it along the ground costs extra power and shakes your view); the Shockwave, your ring fist smashed into the ground while one of 32 giant constructs strikes in front of you, each with its own animation (with cheats on, `/constructshockwave` lets you pick one to try); the Construct Wheel, sixteen slots for hard-light weapons, each shown by its picture, the first a sword and shield (left click: 12 sword moves that flow into each other; hold left for a guarded flurry of stabs; hold right to block, click right for a shield charge that rams everything aside and ends in a slam), the other fifteen slots the weapons still to come (name and picture only); the Ring Scan, your ring hand sweeping the area, which frames every creature around you and makes it glow through walls, red when it is out to hurt you and green otherwise; Giant Hands, giant hands of hard light that rise out of the ground one after another (four to eight at a press, one every half second, at most five at once) at the enemies nearest to you and smack them away, grab and throw them, slap them flat, pound them with a fist, launch them far away with a middle finger that bursts out of the ground, or come as two hands out of portals that chop a giant axe down on them; the Light Bubble, a cage of hard light that traps and lifts a creature for 6 seconds until you pound it into the ground three times or let it go; and his ultimate on Y, the Air Strike, a big propeller gunship of hard light flying high over the battlefield for 20 seconds with two jets racing round it, scanning for enemies and firing its two miniguns, homing missiles that drop out of a hatch in its belly and the jets' small homing missiles, then an engine fails, the jets break the sound barrier and vanish in a star of light, and it dives into the ground in a real explosion that blasts a crater. Becoming him, the ring streaks down out of the sky, circles you, scans you and dresses you in the uniform; changing back, it draws the uniform back in and flies off into the sky. More powers are on the way. |

- Every cooldown and damage number can be changed per character and per ability: every character has
  their own world settings file, kept by every world for itself in its `serverconfig/welcomescreen` folder and
  sent by a server to everyone who plays on it. What only you see and feel (how hard the powers shake your view)
  is in your own settings, `config/welcomescreen/client.toml`. See
  [Characters and their powers](POWERS.md#changing-the-numbers).

### Spells
- Anyone can cast every spell, whatever their class or who they are. Spells cost nothing, but each has a
  cooldown; the wheel shows how long is left.

| School | Spell | What it does | Range | Cooldown |
|---|---|---|---|---|
| Fire | Fireball | A blazing comet that bursts in fire, burns what it hits and leaves a small pile of fire. | until it hits | 2 s |
| Fire | Fire Wall | A ring of fire around you for 2 seconds: it burns what touches it, sets it alight and throws it outward. You stay safe inside. | 3.5 blocks around you | 10 s |
| Lightning | Lightning Strike | A rune circle and storm cloud charge up for 0.7 s, then a branching bolt strikes. Never hits you. | 40 blocks | 8 s |
| Nature | Poison Area | A thrown vial that leaves a poison cloud for 8 seconds. Poisons everything hostile (red) inside. | 24 blocks | 12 s |
| Air | Wind Gust | A wall of wind rolls forward and throws hostile (red) creatures back and up. | 8 blocks | 5 s |
| Dark | Void Walk | 10 seconds invisible, silent and untargetable; 50% faster, 20% harder hits; your world turns black and nearby hostile (red) ones are marked, even through walls. | around you | 30 s |

- The four empty schools (Water, Holy, Ice, Blood) and Earth are in the table already, ready for the
  spells that go in them later.

---

## 6. Friend or foe: factions

Everything alive is one of three colours to you. The Ring Scan and every mark show it, and it decides what your
powers go for.

| Colour | What | Examples |
|---|---|---|
| **Red** (hostile) | Monsters, creatures attacking you, players of an enemy faction, and anything your faction has turned hostile (see below) | zombies, an angry wolf, an enemy player |
| **Yellow** (neutral) | Everything else | animals, villagers, golems, players of other factions |
| **Green** (friendly) | Your faction, allied factions, and your and their pets | your teammate, your tamed wolf |

- Homing, auto-aim and area hits (the Giant Hands, the Air Strike, the Light Bubble's slam, the landing slam,
  Doctor Octopus's rampage and Ground Strike, the Poison Area and Wind Gust spells) only go for **red**.
- What you aim at yourself (a punch, a sword, a bolt, the Giant Fist) also hits **yellow**.
- **Green** is never hurt by you: not by your powers, not by your weapons.
- **Hit something yellow 3 times** (you and your faction together) and it turns **red** for your whole faction,
  until 5 minutes after the last hit or until it dies. A player turned red this way also sees your faction as red.
- Players only hurt each other where the world allows it (PvP).

**The command `/faction`** (a faction is the same as a vanilla scoreboard team, so vanilla `/team` works on them too):

| Command | Who | What |
|---|---|---|
| `/faction create <name>` | operators | Makes a faction (its members cannot hurt each other). |
| `/faction delete <faction>` | operators | Removes a faction. |
| `/faction add <faction> <players>` | operators | Puts players in a faction. |
| `/faction remove <players>` | operators | Takes players out of their faction. |
| `/faction enemy <faction> <faction>` | operators | The two factions are red to each other. |
| `/faction ally <faction> <faction>` | operators | The two factions are green to each other. |
| `/faction neutral <faction> <faction>` | operators | Back to yellow. |
| `/faction invite <player>` | members | Invites a player into your faction (for 5 minutes). |
| `/faction join <faction>` | invited players | Joins the faction you were invited to (operators can join any). |
| `/faction leave` | anyone | Leaves your faction. |
| `/faction list` | anyone | Shows every faction with its members, allies and enemies. |
