# Spells

Everything about the spells anyone can cast: how to cast them, what each one does, and the exact numbers.
Back to the [overview](PROJECT.md). Turning into a character and their abilities are in
[Characters and their powers](POWERS.md).

- 1 second = 20 game ticks. 1 heart = 2 damage.
- "Mob" means any creature that is not a player: monsters, animals, villagers, golems.

---

## How casting works

1. **Hold G.** The screen opens. At the top are the franchises of the characters you can turn into (see
   [Characters and their powers](POWERS.md)); under them, the schools of magic, as the same kind of cards.
2. **The 15 schools of magic**, five cards across, three rows deep. The ones that hold spells come first
   (Air, Fire, Darkness, Lightning, Nature); the ones still being filled say "Coming soon" (Earth, Water,
   Holy, Ice, Blood, Metal, Gravity, Time, Illusion, Cosmic).
3. **Rest on a school.** Keep the mouse still on a school card for **0.38 seconds** and that school opens
   as a page of its own: the same kind of cards, one for every spell in it. A bar fills along the bottom
   of the card while you wait, so you can see it coming. Nothing has to be clicked. A card only starts to
   wait once you have moved the mouse, so a page never opens or closes by itself under a mouse you left alone.
4. **Cast:** move onto a spell and let go of G. **Click** a spell to read what it does instead; letting go
   then casts nothing. Letting go over nothing closes the screen. **Right-click or Escape** goes back to the
   first page, and so does resting on the Back card.
5. **Cooldown.** After casting, the spell needs time before it can be cast again.
   - The line on the card shows "Ready" or the seconds left, in red.
   - Picking a spell that is not ready yet shows how long is left, and casts nothing.

Good to know:
- The key can be changed in Options > Controls, under "Multiverse Powers".
- Anyone can cast every spell, whatever their class. Spells cost nothing but their cooldown.
- The cooldown starts when the spell is cast, not when it ends.
- While the screen is open you stand still, but the world keeps running.
- Spells never break blocks. Only fire spells and lightning can start fires.
- Everyone within 128 blocks sees a spell's effects.

---

## All spells at a glance

| School | Spell | In short | Range | Cooldown |
|---|---|---|---|---|
| Fire | Fireball | A burning comet that explodes and leaves a small fire | flies until it hits, up to 5 s | 2 s |
| Fire | Fire Wall | A swirling ring of fire around you that burns and knocks back enemies | 3.5 block radius | 10 s |
| Lightning | Lightning Strike | Charges up, then lightning strikes where you look | 40 blocks | 8 s |
| Nature | Poison Area | A thrown vial that leaves a poison cloud | 24 blocks | 12 s |
| Air | Wind Gust | A wall of wind that throws creatures back | 8 blocks in front of you | 5 s |
| Dark | Void Walk | 10 s invisible, faster and stronger, enemies marked | around you | 30 s |

---

## Fire Wall (Fire School)

**What you see:** an intense explosion of flames and smoke erupts around your feet. Multiple swirling rings of
fire, embers and lava droplets rise up around you for 2 seconds.
**Effect:** any enemy entering or touching the 3.5 block perimeter takes 6 damage (3 hearts), is set on fire
for 4 seconds, and receives a powerful outward knockback impulse. The caster is immune.

| | |
|---|---|
| Radius | 3.5 blocks circle around the caster |
| Duration | 2 seconds (40 ticks) |
| Damage | 6 (3 hearts) fire damage |
| Knockback | 1.35 outward strength with upward lift |
| Ignites | 4 seconds of fire |
| Cooldown | 10 s |

---

---

## Fireball

**What you see:** a spinning ring of fire with a five-pointed star flares up at your hand. A blazing comet
flies out: a glowing core, two flame strands spiralling around it, a smoke tail and dripping lava.
On impact: a flash, a ball of fire, a burning ring rolling over the ground, then embers and smoke.

| | |
|---|---|
| Speed | starts at 22 blocks per second and speeds up to almost 40, straight where you look |
| Damage | 5 (2.5 hearts), and the target burns for 5 seconds |
| Fire | the spot it hits, plus about half of the 8 spots around it (not at another player's feet where players may not fight each other, and never in spawn protection) |
| Lifetime | fizzles out after 5 seconds if it hits nothing |
| Cooldown | 2 s |

---

## Lightning Strike

**What you see:** sparks jump from your hand. On the target, a glowing rune circle draws itself on the ground
and a dark storm cloud gathers overhead. Then a jagged bolt with side branches tears down, with a flash,
a ring of sparks, flying pieces of the ground, and a smoking, crackling scorch mark.

| | |
|---|---|
| Range | 40 blocks |
| Charge time | 0.7 s before the bolt strikes |
| Aiming | the creature or block you look at; a creature is followed while the spell charges |
| Damage | normal lightning: 5 (2.5 hearts) and sets the target on fire |
| Fire | also sets the ground on fire, on Normal and Hard difficulty |
| You | your own lightning never hits you; other players only where players may fight each other |
| Cooldown | 8 s |

---

## Poison Area

**What you see:** you throw a glowing green vial in an arc. It shatters in slime and glass shards.
A poison cloud spreads out: low toxic fog, rising bubbles, a turning ring and a five-pointed rune star on
the ground. Every creature inside gets a green haze. At the end the cloud thins away.

| | |
|---|---|
| Range | 24 blocks, lands where you look |
| Cloud | 3.5 blocks around the landing spot, 8 seconds |
| Effect | Poison II for 3 seconds, renewed every half second while inside |
| You | never poisoned by your own cloud; other players only where players may fight each other, spectators never |
| Cooldown | 12 s |

---

## Wind Gust

**What you see:** wind spirals up around you, then a curved wall of wind rolls forward with streaks and gusts.
Every creature it reaches is thrown back and up with a puff of air.

| | |
|---|---|
| Range | 8 blocks, in a wide cone in front of you (about 60 degrees to each side) |
| Wave speed | 20 blocks per second; creatures are thrown the moment the wave reaches them |
| Push | strongest up close, weaker further away; knockback resistance (like netherite armour) lowers it |
| Hits | every living creature in the cone, never you; other players only where players may fight each other (PvP on, not in creative) |
| Damage | none, only the push (and the fall afterwards) |
| Cooldown | 5 s |

---

## Void Walk

**What you see:** a dark explosion of ink and void matter swallows you. For 10 seconds you are gone.
Your own screen turns into black silhouettes with violet edges, with a dark border and a slow pulse,
and you hear a low heartbeat. When it ends, you come back with a burst.

| | |
|---|---|
| Duration | 10 s |
| Invisible | fully: no body, armour, held item, name or shadow, for everyone |
| Silent | your footsteps and other sounds make no noise |
| Untargetable | mobs lose track of you and cannot target you, even when you hit them |
| Speed | +50% |
| Melee damage | +20% |
| Marked | monsters and other players within 32 blocks glow for you, also through walls, with a purple mark above their head |
| Ends early | when you die or log out, or when something takes the invisibility away (milk, a flash of light) |
| Cooldown | 30 s |

Only you see the marks and the dark screen.

---

## Characters

The tentacles of Doctor Octopus are no longer a spell: he is a **character** you turn into from the outer
ring of the same wheel. Everything about him, about Green Lantern, and about the ability keys is in
**[Characters and their powers](POWERS.md)**.
