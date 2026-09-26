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
   (Air, Fire, Darkness, Lightning, Nature; Lightning holds two); the ones still being filled say "Coming soon" (Earth, Water,
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
| Fire | Fireball | A burning comet that bursts, burns and throws back what is round it, and leaves a small fire | flies until it hits, up to 5 s | 2 s |
| Fire | Fire Wall | A swirling ring of fire around you that burns and knocks back enemies | 3.5 block radius | 10 s |
| Lightning | Lightning Strike | Charges up, then lightning strikes where you look and leaps on to more foes | 40 blocks | 8 s |
| Lightning | Thunder Clap | You clap your hands: thunder and a shockwave of sparks around you | 9 blocks around you | 10 s |
| Nature | Poison Area | A thrown vial that leaves a slowing poison cloud | 24 blocks | 12 s |
| Air | Wind Gust | A wall of wind that throws hostile (red) creatures back and turns their shots round | 8 blocks in front of you | 5 s |
| Dark | Void Walk | 10 s invisible and faster, hostile (red) ones marked, the first blow out of it an ambush | around you | 30 s |

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

**What you see:** sparks gather in your palm and catch with a click, and a spinning ring of fire with a
five-pointed star flares up at your hand. A blazing ball of fire flies out, crackling: a white-hot heart in
boiling lobes of yellow and orange, six flames streaming and waving off its back, a trail of heat fading from
yellow to smoky red, and dripping lava. On impact: a flash, a ball of fire swelling and rolling up, a ring of heat
racing over the ground, flames licking up in a circle, a scorched ring, then embers and a dark cloud of smoke
climbing on its own column. In water it goes out
with a hiss and a burst of steam.

| | |
|---|---|
| Speed | starts at 25 blocks per second and speeds up, straight where you look |
| Damage | 5 (2.5 hearts) to what it hits, and the target burns for 5 seconds |
| Burst | every hostile (red) creature within 2.5 blocks of the impact takes up to 4 (2 hearts, less further off), burns for 3 seconds and is knocked away |
| Fire | the spot it hits, plus about half of the 8 spots around it (not at another player's feet where players may not fight each other, and never in spawn protection) |
| Lifetime | fizzles out after 4.5 seconds if it hits nothing, or at once in water |
| Cooldown | 2 s |

---

## Lightning Strike

**What you see:** sparks jump from your hand. On the target a rune draws itself on the ground (three turning
rings, then spokes and glyphs), a dark storm cloud of rolling puffs gathers overhead with a far rumble, lit from
inside by flickers, and static crawls over the rune and everything standing on it, humming higher and higher. Just
before it breaks, a thin thread of light joins the ground to the cloud. Then a faint leader feels its way down,
and three blinding strokes blaze up it one after the other, each along a new jagged path with side branches: the
sky flashes, a dome of plasma swells over the strike, a ring races out over the ground, pieces of the ground fly,
and a scorched patch is left with glowing cracks that cool. From there the bolt leaps on in crackling arcs from
one foe to the next. It is the spell's own lightning: the game's normal bolt is never drawn.

| | |
|---|---|
| Range | 40 blocks |
| Charge time | 0.7 s before the bolt strikes |
| Aiming | the creature or block you look at; a creature is followed while the spell charges |
| Damage | normal lightning: 5 (2.5 hearts) and sets the target on fire |
| Shock | hostile (red) creatures within 3 blocks of the strike are slowed hard for 1.5 seconds |
| Chain | then the bolt leaps to the nearest hostile creature it has not touched, within 6 blocks, every 0.1 s: up to 3, or 5 when it rains there. 4 damage (2 hearts) each, and slowed like the shock |
| Fire | also sets the ground on fire, on Normal and Hard difficulty |
| You | your own lightning never hits you or anything green (your faction, allies, pets); other players only where players may fight each other |
| Cooldown | 8 s |

---

## Thunder Clap

**What you see:** you spread your arms, sparks crackle between your hands, and you clap them together in
front of you. A flash and a burst of sparks jump from your hands, you hear thunder, and a wide ring of
sparks and small lightning arcs rolls over the ground around you. Sparkles stay in the air for a moment
afterwards. No lightning bolt falls.

| | |
|---|---|
| Area | a circle of 9 blocks around you |
| Timing | the clap comes 0.3 s after casting; the ring rolls out at 22 blocks per second |
| Damage | 5 (2.5 hearts) close to you, down to half at the edge |
| Push | every creature it hits is thrown away from you and up; knockback resistance lowers it |
| Hits | only hostile (red) creatures, never you; other players only where players may fight each other |
| Fire | none |
| Cooldown | 10 s |

---

## Poison Area

**What you see:** you throw a glass vial of glowing poison that tumbles through the air in an arc, dripping a
thin green trail. It shatters in slime and glass shards with a squelch. A poison cloud spreads out: a glowing ring
and a five-pointed rune star on the ground, a green haze over it all, heavy fog rolling round low over the ground,
slow tendrils of gas winding up round the middle and bubbles welling up out of the muck, swelling and popping;
now and then the muck belches up a thick puff of gas, and it bubbles and brews. Every creature
inside gets a green haze. At the end the cloud sighs out and thins away.

| | |
|---|---|
| Range | 24 blocks, lands where you look |
| Cloud | 3.5 blocks around the landing spot, 8 seconds |
| Effect | Poison I for 3 seconds and Slowness I, renewed every half second while inside; after 2 seconds inside the poison turns to Poison II |
| Who | only hostile (red) ones; never you or anything green or yellow; other players only where players may fight each other, spectators never |
| Cooldown | 12 s |

---

## Wind Gust

**What you see:** three ribbons of wind wind up round your feet and fly off, then a crescent of air rolls
forward in layers, bowed in the middle and curling as it goes, with streaks racing through it and gusts, with the
whoosh of a breeze. Every creature it reaches is thrown back and up with a puff of air.

| | |
|---|---|
| Range | 8 blocks, in a wide cone in front of you (about 60 degrees to each side) |
| Wave speed | 20 blocks per second; creatures are thrown the moment the wave reaches them |
| Push | strongest up close, weaker further away; knockback resistance (like netherite armour) lowers it |
| Hits | every hostile (red) creature in the cone, never you; other players only where players may fight each other (PvP on, not in creative) |
| Damage | none, only the push (and the fall afterwards); it blows out the flames on what it throws |
| Shots | arrows and other shots from hostile (red) ones flying in the cone are turned round, straight where you look, and become yours |
| Loose things | dropped items and experience orbs in the cone are blown away |
| You | it blows out your own flames; cast while falling, it catches you: no fall damage and 1 second of slow falling |
| Cooldown | 5 s |

---

## Void Walk

**What you see:** light is sucked in toward you in violet streaks round a darkening heart, then a sphere of nothing
bursts out, black with a violet rim, shards of darkness flying off it and a ring racing out over the ground, in an
explosion of ink and void matter that swallows you. For 10 seconds you are gone.
Your own screen turns into black silhouettes with violet edges, with a dark border and a slow pulse,
and you hear a low heartbeat. When it ends, you come back with a burst: the sphere of nothing shrinks away round you. An ambush tears the
air where the blow lands: three claws of darkness edged with violet.

| | |
|---|---|
| Duration | 10 s |
| Invisible | fully: no body, armour, held item, name or shadow, for everyone |
| Silent | your footsteps and other sounds make no noise |
| Untargetable | mobs lose track of you and cannot target you, even when you hit them |
| Speed | +50% |
| Ambush | your first melee hit out of the void does 50% more and leaves the victim blind and slowed for 2 seconds; it also ends the walk |
| Marked | everything hostile (red) to you within 32 blocks glows for you, also through walls, with a purple mark above their head |
| Ends early | when you strike, die or log out, or when something takes the invisibility away (milk, a flash of light) |
| Cooldown | 30 s |

Only you see the marks, the dark screen and the faint wisps where your feet fall; two seconds before the end a
soft tone warns you.

---

## Characters

The tentacles of Doctor Octopus are no longer a spell: he is a **character** you turn into from the outer
ring of the same wheel. Everything about him, about Green Lantern, and about the ability keys is in
**[Characters and their powers](POWERS.md)**.
