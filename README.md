# Multiverse Powers

A Minecraft mod for **NeoForge 1.21.1**: pick an RPG class, cast spells and turn into characters from across the
multiverse.

> **Alpha.** Things change fast and settings may reset between versions.

## Features

- **Characters:** turn into one from the power wheel (hold **G**).
  - **Green Lantern:** a power ring that shapes solid green hard light: flight, weapons, giant hands, a steam train,
    an air strike and a mech.
  - **Doctor Octopus:** four robot tentacles that walk, climb, grab, throw, strike the ground and open portals.
  - **Thor:** thunder fists, flight, Mjolnir to throw and fly after, a thunderclap and lightning from the sky.
- **Classes:** pick a group and a class before you play, each with its own start ceremony.
- **Spells:** cast from the same wheel; fifteen schools of magic, five spells so far.
- **Stamina:** sprinting and jumping cost stamina; walking or standing still refills it.
- **Bodies:** creatures go limp when they are thrown, blasted or killed, and get back up when they can.
- **Music:** the mod's own theme plays in the main menu.
- **Updates in the game:** the mod tells you when a new version is out and installs it in one click; after an update
  a short tour shows the big changes.
- **Feedback:** report a bug or suggest an idea from inside the game. It becomes a public issue on this repository,
  with your Minecraft name.

More: [Characters and their powers](docs/POWERS.md) · [Classes](docs/CLASSES.md) · [Callings](docs/CALLINGS.md) ·
[Spells](docs/SPELLS.md) · [Everything else](docs/PROJECT.md)

## Install

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1 (21.1.250 or newer).
2. Download the newest `multiverse-powers-<version>.jar` from
   [Releases](https://github.com/Tivek87/mc-class-mod/releases).
3. Put the jar in your `mods` folder and start the game.

A server and every player on it need the same version; joining one on another version offers to switch yours.

## Controls

| Key | Does |
|---|---|
| **G** (hold) | Power wheel: pick a character or a spell |
| **R V Z B H N Y X C**, **Left Alt**, **K** | Your character's abilities (**Y** is the ultimate); more on the mouse, space and shift (Thor uses only those) |
| **P** | Ability guide: every control of your character |
| **U** | Update manager: updates, feedback and the tour |

Change any key in *Options > Controls > Multiverse Powers*. A panel at the bottom right shows what you can use right
now.

## Settings

*Mods > Multiverse Powers > Config*: **Client** for what only you see and hear, **Server** for how a world plays
(damage, cooldowns, costs). Each world keeps its own server settings; only the host or a listed owner may change them.

## Building

Needs Java 21. `./gradlew build` puts the jar in `build/libs/`; `./gradlew runClient` starts a test game. The code is
in `src/main/java/nl/tivek/multiversepowers/`, release and bug-report scripts in `scripts/`. What each version
changed: [CHANGELOG.md](CHANGELOG.md).

## License

All Rights Reserved. The characters belong to their owners (Marvel, DC and others); this is an unofficial fan
project.
