# Multiverse Powers

A Minecraft mod for **NeoForge 1.21.1** that adds RPG classes, a Vanilla+ stamina bar, spells, and characters from
across the multiverse you can turn into.

> **Alpha.** Things change fast and settings may reset between versions.

## Features

- **Classes:** pick one of six groups (or The Forsaken) and a class before you play, with its own start ceremony,
  death and level-up animation. See [Classes and skill trees](docs/CLASSES.md) and [Callings](docs/CALLINGS.md).
- **Stamina:** sprinting and jumping cost stamina; standing still or walking refills it.
- **Spells:** fifteen schools of magic, cast from the wheel (hold **G**). See [Spells](docs/SPELLS.md).
- **Characters:** turn into a character from the wheel and use its powers on eleven ability keys.
  - **Green Lantern:** a power ring that shapes solid green hard light: a giant fist, flight, a construct wheel,
    shockwaves, giant hands, a light cage and an air strike.
  - **Doctor Octopus:** four robot tentacles that walk, climb, grab, throw, strike the ground and open portals.
- **Music:** the multiverse theme plays on a loop in the main menu, in place of Minecraft's menu music; the music slider
  sets its volume.
- **Updates in the game:** the mod looks for a new release every five minutes. A new one comes with a pling and a popup;
  click it in the menu, or press **U** (changeable under Controls) for the update manager: read what's new, then update
  later (installed when you close the game) or update now (the game quits, then start it again yourself).
- **Bug reports:** **Report a bug** in the update manager sends a bug with a name, a description and a priority. It
  becomes a public issue on this repository, with your Minecraft name. What you type is kept until you send it, also
  when you leave the screen, die or close the game; **Sent** shows your last 3 reports with their status on GitHub.
- **Ideas:** **Suggest an idea** (next to it) sends an idea the same way, with a name, a description and a priority,
  and keeps your text and your last 3 ideas the same way.

Everything each power does: [Characters and their powers](docs/POWERS.md).

## Install

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1 (21.1.250 or newer).
2. Download the newest `multiverse-powers-<version>.jar` from
   [Releases](https://github.com/Tivek87/mc-class-mod/releases).
3. Put the jar in your `mods` folder and start the game.

On a server, the server and every player need the same version of the mod.

## Controls

| Key | Does |
|---|---|
| **G** (hold) | The wheel: pick a character or a spell |
| R, V, Z, B, H, N, Y, X, C, Left Alt, K | Ability 1 to 11 of the character you are (Y is always the ultimate) |
| Crouch + a key | Depends on the ability: undo it, a second version, or nothing |

Every key can be changed in *Options > Controls > Multiverse Powers*. A panel in the bottom right shows what each
key does right now and its cooldown.

## Settings

Every character's numbers (damage, cooldowns, costs) can be changed in the game: *Mods > Multiverse Powers >
Config*, where you first pick *Client* or *Server*. Server settings belong to a world: each world keeps its own in
`<world>/serverconfig/welcomescreen/`, a server sends its own to everyone who plays on it, and only the host or an
operator may change them. What only you see and hear (screen shake, menu music, update checks) is in
`config/welcomescreen/client.toml`.

## Versions

Every update is one step higher: `0.0.1`, `0.0.2` ... `0.0.9`, `0.1.0` ... `9.9.9`. Every version from `0.1.0` on
stays on the [Releases](https://github.com/Tivek87/mc-class-mod/releases) page. What changed: [CHANGELOG.md](CHANGELOG.md).

## Building from source

Needs Java 21.

```
./gradlew build
```

The jar lands in `build/libs/`. `./gradlew runClient` starts a test game, `./gradlew runServer` a test server.

## Project layout

| Folder | What |
|---|---|
| `src/main/java/nl/tivek/multiversepowers/` | The mod. `engine/` holds what every power can use, `character/` one folder per character, plus `spell/`, `classes/`, `stamina/`, `config/`, `network/`. |
| `src/main/resources/` | Textures, sounds, `en_us.json` (all in-game text) |
| `docs/` | How everything plays, per topic |
| `scripts/` | `release.ps1`: builds a release and keeps the newest 10 jars in `releases/` (ignored). `bugs.ps1`: copies the open bug reports and ideas to `bugs/` (ignored). `bug-relay/`: the Cloudflare Worker that turns a report or idea from the game into an issue |

## License

All Rights Reserved. The characters belong to their owners (Marvel, DC and others); this is an unofficial fan
project.
