# Thor: Hammer and Flight (short version)

*Design only, nothing built yet. 2026-10-02. The full version, with the technical build plan:
`THOR_MJOLNIR_FLIGHT_SYSTEM.md`.*

## The idea

Mjolnir is always in one place: on Thor's belt, in his hand, flying, or lying somewhere in the world.

- Throw it and it comes back, throw it and it stays, or throw it and dash after it.
- Leave it in the world: on the ground, stuck in a wall, or hanging in the air.
- Call it back to your hand. It hits enemies on the way.
- Dash to it wherever it rests (up to 24 blocks). Near the ground you land next to it; high up you catch it and
  keep flying.
- Flying needs the hammer. In flight you can throw it in a Storm Throw.

No new keys: right click throws, the scroll wheel is the "hammer button", space flies.

## What is wrong now

- The hammer never stays: after 5 seconds it flies back by itself, and you can't call it.
- At rest it always floats upright, even when it hit the floor or a wall.
- Only you see yourself fly to the hammer. Other players see you slide through the air: no pose, no lightning.
- Flying to it ignores walls. Arriving in the air, you just fall.
- Until the hammer stops you just stand there: nothing shows that the pull is coming.
- No throw animation (the hammer just disappears from your hand) and no catch animation.
- You can fly while the hammer is thrown: your hand then holds nothing.
- The hammer flying back hits nothing.

## Where the hammer can be

| Where | You fight with |
|---|---|
| On the belt | fists |
| In the right hand | the hammer |
| In the left hand, while flying | flight moves |
| Flying out | fists |
| Resting in the world | fists |
| Flying back to you | fists |

## Buttons

**On the ground, hammer in hand**

| Button | Does |
|---|---|
| Right click | Hammer Throw: it comes back by itself |
| Sneak + right click | **Throw to Stay** (new): it stays where it lands |
| Hold right, let go | Throw and Follow: hold longer to throw further (4 to 14 blocks); half a second later you dash into it |
| Left click, hold left, scroll click, hold scroll | as now |

**On the ground, hammer away**

| Button | Does |
|---|---|
| Left and right | fist moves, as with the hammer on the belt |
| Scroll click | **Call the Hammer** (new) |
| Hold scroll 0.5 s | **Follow the Hammer** (new): you dash to it (up to 24 blocks away) |
| Hold space | the hammer flies into your hand, then you take off |

**Flying:** as now, plus **hold scroll 0.5 s: Storm Throw** (new).

## Throwing

- **Hammer Throw (right click):** a real throw animation. The hammer flies up to 24 blocks (was 40), hits the first
  enemy hard (3.5 hearts) and flies back into your hand, hitting enemies on the way back too.
- **Throw to Stay (sneak + right click):** the same throw, but the hammer stays where it lands. Use it to leave the
  hammer somewhere on purpose.
- **Throw and Follow (hold right, let go to throw):** you pull the hammer back over your shoulder. The longer you
  hold, the further it will fly: 4 blocks at first, up to 14 blocks after 1 second. A small mark shows where it
  will stop. Let go to throw. It stops at the first enemy (2 hearts), a block, or that distance. Half a second later
  you dash into it and catch it.
  - At an enemy: you land right in front of it.
  - At the ground: a short "teleport" along the ground.
  - Into the sky: you shoot up and keep flying.

## The hammer resting in the world

| It stopped on | It rests | Looks like |
|---|---|---|
| A floor | lying | standing on its head, handle up |
| A wall | stuck | head in the wall, handle out |
| A ceiling | stuck | head in the ceiling, handle down |
| An enemy, or nothing | hanging | floating upright, turning slowly, sparks |

- It stays while you are within 128 blocks. Further away, it flies home onto your belt.
- If the block under it breaks, it falls. In water or lava it sinks to the bottom.
- Nobody else can take it, move it or break it.
- It is not saved: leave the world and it is back with you.

## Calling it back (scroll click)

- You hold your hand out. The hammer pulls free (out of the ground or the wall) and flies straight back, through
  blocks.
- Every enemy in its way takes 2 hearts and is knocked aside. More if the hammer is charged.
- It slams into your hand with a crack of thunder.
- If your right hand holds an enemy you grabbed, the hammer goes onto your belt.

## Dashing to it

You get there with Throw and Follow, or with **Follow the Hammer** (hold scroll 0.5 s while it rests, up to 24
blocks away).

- First, half a second: you plant your feet and reach for the hammer, and a line of lightning grows between your
  hand and it. Everyone sees it, so the wait looks like charging, not lag.
- Then you dash to it at a set pace: 6 blocks take 0.7 s, 14 blocks about 1.4 s. Everyone sees a lightning streak.
  You slow down at the end, so you don't crash.
- A ledge in the way: you go over it. Blocked by a wall: you stop and drop; the hammer stays where it is.
- No fall damage during the dash or just after it.
- A hard hit (2.5 hearts or more) stops you.

**Arriving: is there ground just below the hammer (within 2.5 blocks)?**

- Yes: you land and the hammer flies into your right hand.
- No: you catch it in your left hand and keep flying.

## Flight

- **Flying needs the hammer.** Hammer away? Hold space: it flies into your hand, then you take off.
- **Landing** puts the hammer back where it was: in your hand or on your belt.
- **Storm Throw (hold scroll 0.5 s while flying):** you throw the hammer at an enemy. A ring of lightning flashes
  around the hit, and lightning jumps from enemy to enemy inside it (up to 5). Then the hammer comes back. While
  it is gone you float in the air, slowly sinking.

## Combos

1. Throw and Follow at a skeleton: you land in front of it with the hammer and hit it.
2. Throw and Follow into the sky: you are flying. Storm Throw at the zombies below.
3. Throw to Stay onto a hill, fight with your fists, then hold scroll: you are on the hill.
4. The hammer lies behind a group of enemies. Scroll click: it smashes through them back to you.

## Special cases

| What | Then |
|---|---|
| You die, log out or stop being Thor | the hammer is gone; next time you have it again |
| You go to another dimension | it comes home onto your belt |
| You call it while it is still flying out | it turns around at once |
| You keep holding after the full draw | it stays at 14 blocks; it throws when you let go (or by itself after 3 s) |
| Two Thors | each has his own hammer |
| A server that kicks floating players | the wait and the dash count as flight, so no kick |

## How I build it

Six steps. Each step works on its own and is its own release (only after your yes).

| Step | What | You see |
|---|---|---|
| 0 | Move the hammer code into its own folder | nothing changes |
| 1 | The hammer stays (lying, stuck, hanging) + Throw to Stay | throws stay |
| 2 | Call the Hammer + flying needs the hammer | you can call it back |
| 3 | The draw, the wait and the dash + Follow the Hammer | the dash into the hammer |
| 4 | Throw and catch animations, small fixes | it feels heavy and real |
| 5 | Storm Throw | the lightning combo |

Every step is tested in the game: each kind of throw, calling, the dash to the hammer, the special cases, and
multiplayer.

## Decided

- Throw and Follow becomes the timed dash: hold to throw further (4 to 14 blocks), let go, wait half a second,
  then dash at a set pace (6 blocks in 0.7 s).
- The normal throw goes at most 24 blocks.

## Still to decide (yes or no per number)

1. Flying needs the hammer.
2. Scroll wheel = hammer button (click: call it, hold: dash to it). Charge then only works with the hammer on you.
3. A resting hammer stays (no more flying back after 5 seconds).
4. Arrival rule: ground below, you land; otherwise you keep flying.
5. New: Throw to Stay.
6. New: Call the Hammer (it hits enemies on the way back).
7. New: Follow the Hammer (the same wait and dash, up to 24 blocks).
8. New: Storm Throw.
9. The small mark that shows where the hammer will stop while you hold.
10. Build in the 6 steps above.
