# Thor: Flight, Throws and Mjolnir in the World

*Design only, nothing built yet. Written 2026-10-02.*

Built on the code as it is now and on the ideas in `docs/ideas/THOR_AND_MJOLNIR.txt` (§1: calling the hammer back,
throwing it so it hangs or lies and flying to it, the short-range "teleport", the throw that ends in a ring of
chained lightning; §2: the armed and flight button lists; §3: everything working together). New abilities and
changes to how Thor plays need your yes before they are built (CLAUDE.md, Engine). §9 lists what to decide.

## Contents

1. The system in short
2. What exists today
3. Principles
4. How it plays
5. How it looks and sounds
6. How I will build it
7. Build phases
8. Tests
9. Decisions for your yes
10. Later

---

## 1. The system in short

Mjolnir is always in exactly one place: on Thor's belt, in his hand, flying, or resting somewhere in the world.
Thor can:

- **throw it so it comes back** (Hammer Throw), **throw it so it stays** (Throw to Stay), or **throw it and dash
  after it** (Throw and Follow: the longer he holds, the further it goes; half a second later he dashes into it);
- **leave it in the world**: lying on the ground, stuck in a wall or ceiling, or hanging in the air;
- **call it back** to his hand from wherever it rests, hitting what is in its way (Call the Hammer);
- **dash to it** wherever it rests, up to 24 blocks away (Follow the Hammer): near the ground he lands beside it
  and takes it up; up in the air he catches it and flies on;
- **fly only with it**: taking off without it calls it into his hand first; in flight he can hurl it in a Storm
  Throw and hang in the air until it is back.

All on the buttons he already uses: the right button throws, **the scroll wheel is the hammer button** (click:
bring it to me; hold: take me to it), space flies.

---

## 2. What exists today

Read from the code on 2026-10-02 (not yet checked in the game).

| Move | Button | What it does now | Code |
|---|---|---|---|
| Take Up the Hammer | scroll click, on the ground | belt ↔ right hand; refused while it is thrown | `Mjolnir.toggle` |
| Hammer Throw | right click, hammer in hand | flies up to 40 blocks at 2.2 blocks a tick, at the creature in the crosshair or along the look; stops at the first creature (3.5 hearts, thrown far along its path) or block; flies back at 2.6 through blocks, hitting nothing; caught within 1.8 × his size of his hand | `Mjolnir.fling(.., false)`, `Mjolnir.tick` |
| Throw and Follow | hold right 1 s, hammer in hand | the same throw (2 hearts); where it stops it waits upright in the air (`WAITING`) and the server sends `PULL`; his own game flies him to it (`ThorPull`: 2.8 blocks a tick, at most 40 ticks); the server catches it once he is within 2.6 × his size; no fall damage for 6 s; unreached, it flies back after 5 s | `Mjolnir.out`, `client/motion/ThorPull` |
| Flight | hold space | his left hand draws the hammer from the belt and holds it ahead; flies where you look at 18 blocks a second; hovers; a hit of 2.5 hearts knocks him down; flying onto the ground touches down | `ThorMotion.fly`, `ThorMoves.takeOff`, `ThorMoves.land` |
| Air Blink | right click in flight | 15 blocks in 3 ticks the way you steer, short of walls | `ThorMotion.blink`, `ThorMoves.blink` |
| Super Jump | double space | 10 blocks up, floats 2.5 s; the ground gestures still work in the float | `ThorGroundMotion.jumping` |
| Lightning Speed, Grab-Dash Dive, Sky Shockwave, Lightning Bolt | flight gestures | as in `docs/POWERS.md` | `ThorMoves`, `GrabDive`, `SkyMoves` |

The hammer's server state is `Mjolnir.State`: `HOME` (belt or right hand, by `armed`), `OUT`, `WAITING`, `BACK`.
Out of his hands it is a `ThrownHammer` entity: never saved, tracked out to 10 chunks, sent every tick, with
`SIZE` and `CHARGED` synced.

**Gaps this design closes**

1. It never stays: an unreached hammer flies back after 5 s, and there is no way to call it.
2. At rest it always floats upright, also when it hit a floor or a wall.
3. Only the puller sees the pull: the others see him slide through the air in no pose and without a streak
   (`ThorPull`'s sparks are made in his own game only; `ClientThor.update` starts nothing for others on `PULL`).
4. The pull flies a straight line with no wall handling and gives up after 40 ticks wherever it is; arriving in the
   air he just drops.
5. Until the hammer stops he just stands there: no pose, nothing that shows the pull coming, and for a time that
   depends on the throw.
6. No throw pose (the hammer vanishes from his hand) and no catch (it reappears in it).
7. He can take off with the hammer thrown; his left hand then grips nothing (`ThorHammerLayer` hides the hammer,
   `ThorPoses` still poses the grip).
8. Touching down with the hammer in hand, the pose hangs it on the belt, then it jumps to his right hand.
9. The flight back hits nothing.
10. Only holds of the left button show the charge ring (`ThunderGauge`); the 1-second right hold shows nothing.
11. A hammer whose chunk unloads is dropped with it and snaps back into his hand.
12. At Lightning Speed (48 blocks a second) nothing loads the chunks ahead on the server; only his own game slows
    him at their edge (`ChunkEdge.cap`).

---

## 3. Principles

- **The server owns the hammer, his game owns his body.** As with all his moves, his own game moves Thor; the
  server decides where the hammer is, checks, hits, and tells everyone.
- **Every state has a way out.** Nothing waits forever. Leaving, dying, another dimension, logging out, a chunk
  unloading and the server stopping always bring the hammer home cleanly.
- **The world decides.** What stopped the hammer decides how it rests; what lies under the hammer decides how Thor
  arrives.
- **No new keys.** The same buttons, chosen by state through the existing `needs` bits and the ground/flight split.
- **Everyone sees the same.** Every move has a pose and particles sent from the server; how the hammer rests is
  synced on its entity.

---

## 4. How it plays

### 4.1 Where the hammer can be

| Where | Thor fights with | How it is drawn |
|---|---|---|
| On his belt | fists | on his left hip (as now) |
| In his right hand | the hammer | in hand (as now) |
| In his left hand, in flight | flight moves | held out ahead (as now); belt or hand is remembered for after landing |
| Flying out (thrown) | fists | tumbling end over end (as now) |
| Resting in the world | fists | lying, stuck or hanging (4.4) |
| Flying back to him | fists | head first, handle trailing |

### 4.2 The buttons, by situation

**On the ground, hammer in hand**

| Button | Does |
|---|---|
| Left click | hammer blows (as now) |
| Hold left 0.75 s | Hammer Uppercut (as now) |
| Right click | **Hammer Throw**: it comes back by itself |
| Sneak + right click | **Throw to Stay** (new): it rests where it stops |
| Hold right, let go | **Throw and Follow**: the longer you hold, the further it goes (4 to 14 blocks); half a second after it leaves his hand he dashes into it |
| Scroll click | hang it on the belt (as now) |
| Hold scroll 2 s | Charge, for the hammer (as now) |

**On the ground, hammer on the belt:** as now (Thunder Fists, Thunderclap, Dash, Grab, Grab Dash, Take Up the
Hammer, Charge for himself).

**On the ground, hammer away** (flying out, resting or flying back):

| Button | Does |
|---|---|
| Left and right | the fists' moves, as with the hammer on the belt |
| Scroll click | **Call the Hammer** (new) |
| Hold scroll 0.5 s, hammer resting | **Follow the Hammer** (new): he dashes to it, up to 24 blocks away |
| Hold space | he raises his left hand, the hammer flies into it, he takes off |
| Double space | Super Jump (as now) |

**In flight:** as now, plus **hold scroll 0.5 s: Storm Throw** (new).

The panel greys a move out with what it waits for, as it does now for *with hammer*; the new ones read *hammer
with you*, *hammer away* and *hammer resting*.

### 4.3 Throwing

**Hammer Throw (right click).** He draws his arm back and hurls it: it leaves his hand 2 ticks after the click,
from his hand toward the point under the crosshair, so it meets what he aims at. It flies up to 24 blocks (was 40),
end over end; the first creature it hits takes 3.5 hearts and is thrown far along its path; a block stops it with a
clang and a puff of that block's dust. Then it flies back by itself (4.5), hitting what it passes, and he catches
it in his right hand. Cooldown 1.5 s (as now).

**Throw to Stay (sneak + right click).** The same throw, but it does not come back: it rests where it stops (4.4)
until he calls it or follows it. It shares Hammer Throw's cooldown. This is how he leaves it in the world on
purpose: on a hilltop to come back to, in the air as a waypoint, by a door.

**Throw and Follow (hold right, let go to throw).** Held past 0.3 s it is no click any more: he draws the hammer
back over his shoulder, sparks crawling over it (everyone sees it), and the longer he holds, the further it will go:
4 blocks at 0.3 s, growing to 14 blocks at 1 s, where it stays however long he holds on (at most 3 s, then it goes
by itself). The ring round the crosshair fills with the draw, and in his own view a small crackling mark shows where
the hammer will stop. He lets go and hurls it. It stops at the first creature (2 hearts; the creature is thrown away
and the hammer hangs where it hit), at a block, or at the drawn distance, and rests there. Half a second after the
throw he dashes into it (4.6) and catches it. Unreached (walled off, knocked out of the dash) it keeps resting: it
no longer flies back by itself after 5 s. Cooldown 5 s (as now).

Aimed at a creature it closes the gap (he lands right in front of it, hammer in hand); aimed at the ground ahead it
is a short-range "teleport" along the ground; aimed into the sky it launches him into flight.

### 4.4 The hammer at rest

What stopped it decides how it rests:

| Stopped by | Rests | Looks |
|---|---|---|
| the top of a block (a floor) | **Lying** | standing on its head, handle straight up and a hair off upright, its peaked roof pressed about a tenth of a block into the ground; a ring of dust as it lands |
| the side of a block (a wall) | **Stuck** | head a quarter into the wall, handle straight out of it; a puff of that block's dust |
| the underside of a block (a ceiling) | **Stuck** | head in the ceiling, handle hanging down |
| a creature, or nothing (the end of its reach) | **Hanging** | upright, head up, bobbing and turning slowly, sparks crawling over it |

- **It stays** while he is within 128 blocks of it in the same world (a setting). Further away it comes home by
  itself, onto his belt.
- **The world moves it.** A lying hammer whose floor is broken falls and lies again; a stuck one whose block is
  broken drops and lies; in water or lava it sinks and lies on the bottom; a block placed into it lifts it onto
  that block; fallen out of the world it comes home.
- **Nobody else can have it.** It is no item: nothing picks it up, pushes it or harms it; blows and blasts do
  nothing to it; players and creatures pass through it.
- **It looks alive.** Sparks crackle over it every few seconds; while it is charged its runes keep glowing.
- **It is never saved** (as now): leaving the world brings it home.
- **It never vanishes with an unloaded chunk:** while it is out of his hands its own chunk is kept loaded (4.9).

### 4.5 Calling it back

**Call the Hammer (scroll click while the hammer is away)**, also while it is still flying out: it turns back at
once.

- He stretches his right arm toward it, palm open, and keeps it there; everyone sees the arm follow the hammer.
- The hammer jerks free (lifting off the ground, or wrenching out of the block it is stuck in with a crack and that
  block's dust; the block stays), turns its head to him and flies back fast: from 1 to 3.2 blocks a tick within 4
  ticks, straight, through blocks.
- Every creature it passes takes 2 hearts and is knocked aside out of its path, never into him; each once.
  Charged: half as much again, and further.
- Once it is 3 ticks away his hand opens; it slams into his palm, his arm is shoved back, a short crack of thunder,
  sparks; your own view jolts a little. He holds it in his right hand.
- His right hand busy (holding a creature he grabbed): it lands on his belt instead.
- Cooldown 0.5 s.

The return after a plain Hammer Throw is the same flight and catch; it hits only what the throw has not hit already.

### 4.6 Going to it: the wait and the dash

Two ways in: **Throw and Follow**, or **Follow the Hammer (hold scroll 0.5 s while it rests, up to 24 blocks
away)**. The wait and the dash together are what the code calls the pull.

**The wait.** For half a second (counted from the throw, or from the moment Follow the Hammer starts) he plants his
feet and stretches out the hand that will catch it (left when he will fly on, right when he will land), and a
crackling line of lightning grows between that hand and the hammer, brighter and brighter. Everyone sees it, so the
wait reads as the pull charging up, not as lag.

**The dash.** Then he is yanked off at it at a set pace: 6 blocks take 0.7 s (about 11 blocks a second once he is
going; he speeds up over the first 3 ticks and brakes over the last ones), 14 blocks about 1.4 s, 24 blocks about
2.3 s. Body flat behind his reaching hand, in a zigzag streak of lightning that everyone sees; he brakes into the
catch instead of crashing into it. A ledge in the way lifts him over it; walled off for 8 ticks, he gives up and
drops (no fall damage) and the hammer keeps resting. No fall damage during the dash and for 2 s after it. His other
moves wait until he arrives.

**Arriving.** One rule: **is there ground within 2.5 blocks below the hammer?**

- **Yes, he lands**: a step short of it, as it flies the last bit into his right hand (out of a wall it wrenches
  free first). Hammer in hand.
- **No, he flies on**: he catches it in his left hand and is flying, keeping part of his speed: holding W he goes
  on, letting go he settles into a hover. Every flight move works from here.

**Breaking it off.** A hard hit (2.5 hearts or more, as in flight) or another Thor's Sky Shockwave ends the pull:
he drops (no fall damage), the hammer keeps resting.

**Follow the Hammer** has a 2 s cooldown and is refused while the hammer rests more than 24 blocks away or in lava
(call it instead).

### 4.7 Flight and the hammer

- **Flight needs the hammer.** Hold space with it on the belt or in hand: as now. With it away: he raises his left
  hand, the hammer is called (4.5) into that hand, and he takes off the moment he catches it.
- **Touching down** puts it back where it was: in his right hand (his left passes it across) if he flew off with it
  in hand, on his belt if not. (Gap 8.)
- **Storm Throw (new; hold scroll 0.5 s in flight).** He hurls the hammer from his left hand at what he aims at, or
  at the point he looks at, up to 24 blocks. Where it strikes, a ring of lightning flashes round it (4 blocks), and
  lightning leaps from what it struck to the nearest creature out to hurt you inside the ring, and on from there,
  each struck once, up to 5 leaps: 3 hearts to what it strikes, 2 to each leap. Then it flies back into his left
  hand. While it is gone he hangs in the air, arms out, sinking slowly, as at the top of a super jump; his other
  flight moves wait for it (his right-hand blows still work). Cooldown 10 s.
  This is the attack from the concept list: Throw and Follow into the air, arrive flying, Storm Throw into the
  crowd below.
- **Lightning Speed** also keeps the chunks ahead of him loaded on the server, as Green Lantern's flight does
  (`ChunkPreloader.keep`). (Gap 12.)

### 4.8 How it chains together

1. Hammer in hand, a skeleton 12 blocks off: hold right until the mark sits on it, let go. The hammer knocks the
   skeleton flying and hangs where it hit; half a second later he dashes in and lands there with the hammer in
   hand: hammer blows.
2. Throw and Follow straight up at full draw: he arrives flying 14 blocks up. Storm Throw at the zombies below; the
   hammer comes back; Grab-Dash Dive onto one of them.
3. Throw to Stay onto a hilltop; fight on with fists (Dash, Grab, Thunderclap); hold scroll: he is on the hilltop,
   hammer in hand.
4. The hammer lies behind a crowd: scroll click, and it plows back through them into his hand.
5. The hammer is away and enemies close in: hold space, it flies into his raised hand (hitting what it passes) and
   he is up in the air.
6. Super Jump; in the float at the top, Throw and Follow (the ground moves work in the float): he dashes on from up
   there.

### 4.9 Edge cases

| Situation | What happens |
|---|---|
| He walks more than 128 blocks from it | it comes home onto his belt |
| He goes to another dimension | it comes home onto his belt |
| He dies, logs out, stops being Thor, or Thor is switched off in the world | it is gone; he has it again the next time (as now) |
| The world is saved and loaded, or the server stops | it is not saved; home (as now) |
| Its chunk would unload | kept loaded while the hammer is out, by a ticket that lapses by itself; should it vanish anyway, home onto his belt |
| Two Thors | each has his own; nobody can call or follow another's |
| Thrown at a wall a block away | it rests at once; a follow is a tiny hop |
| Thrown over the void | it hangs; following it he flies on |
| It comes back while his right hand holds a creature | onto the belt |
| Called while still flying out | it turns back at once |
| Held past the full draw | it stays at 14 blocks; it goes when he lets go, or by itself after 3 s |
| Scroll wheel during the pull | does nothing |
| Hit hard during the pull | the pull ends, he drops without fall damage, it keeps resting |
| Its floor or its block is broken | it falls and lies |
| A dedicated server with `allow-flight=false` | the pull and the hang count as flight on the server, so he is never kicked for floating |
| Lag: his game says he arrived, the server sees him a little short | the server accepts a catch up to 1.5 blocks further off, and catches by itself if the message never comes |
| Movement keys during the pull | ignored, as during a dash |

---

## 5. How it looks and sounds

Everyone sees the body and the hammer; "own view" is first person.

| Moment | Body | Own view | Hammer | Particles and sound |
|---|---|---|---|---|
| Throw | right arm back over the shoulder (2 ticks), whips forward with a step, follows through | the arm throws and comes back empty | leaves the hand | sparks; a trident throw, a light crack of thunder |
| Drawing (Throw and Follow's hold) | hammer drawn back over the shoulder, further the longer he holds, weight back, sparks over it | the hammer drawn back on the right; a small mark where it will stop | in hand | a crackle growing with the draw |
| Flying out | | | tumbling end over end | spark trail, blue glow if charged |
| Resting | | | lying, stuck or hanging (4.4) | sparks every few seconds, a soft crackle |
| Called, flying back | right arm out at the hammer, palm open | your arm reaches toward it | jerks free, flies head first | spark trail; a trident return as it sets off |
| Catch | arm shoved back, the body rocks a little | the hammer slams into your palm, the view jolts | in hand | sparks at the hand; a heavy smack and a light crack of thunder |
| The wait | feet planted, the catching hand stretched to the hammer | your arm reaches toward it | resting ahead, shaking | a line of lightning from his hand to the hammer, brighter each tick; a rising crackle |
| The dash | flat behind the reaching hand | the reaching arm ahead; the view widens a little | resting ahead | a blue zigzag streak; riptide and thunder as he sets off, a ground smack on landing |
| Storm Throw | left arm hurls, then arms out, hanging | the left arm throws, both hands free | flies, strikes, returns | a ring of lightning, arcs leaping between creatures; thunder |

`ThrownHammerRenderer` draws the hammer from its synced state: tumbling (out), head first (back), lying, stuck
(along the face it hit) and hanging (bobbing, turning).

---

## 6. How I will build it

### 6.1 Where the code goes

`Mjolnir` (399 lines) would pass 600 with all this, so the hammer becomes a family of its own (CLAUDE.md: split by
responsibility, a sub-package once a family forms, a chain of package-private classes with the public one on top).

| File | What |
|---|---|
| `character/thor/hammer/Mjolnir.java` | public top of the chain: what the rest calls (`inHand`, `home`, `away`, `resting`, `flags`, `where`, `toggle`, `fling`, `call`, `follow`, `caught`, `uppercut`, `leave`, `clear`) |
| `character/thor/hammer/MjolnirCatch.java` | the flight back (called or by itself), its hits, the catch into a hand or onto the belt |
| `character/thor/hammer/MjolnirRest.java` | resting: lying, stuck, hanging; falling when its support goes; sinking in water and lava; the leash; its chunk kept loaded; the crackle |
| `character/thor/hammer/MjolnirFlight.java` | the flight out: path, hits, where it stops and what it becomes |
| `character/thor/hammer/MjolnirCore.java` | the per-Thor state and fields, the `ALL` map, spawning and moving the entity |
| `character/thor/hammer/ThrownHammer.java` | the entity, moved here; its registry id stays `mjolnir`; new synced `OWNER`, `REST`, `FACE` |
| `character/thor/HammerPull.java` | the pull on the server (the wait and the dash), a sub-state of `ThorMoves` as `GrabDive` is: flags, the lightning line, the streak, flight safety, arrival |
| `character/thor/StormThrow.java` | the Storm Throw's strike: the ring and the chain |
| `character/thor/client/pose/ThorHammerPoses.java` | the new body poses (the draw, the wait, the dash, call, catch, hang), called from `ThorPoses.pose` as `ThorBlowPoses` is, so `ThorPoses` stays under 600 lines |

Changed: `ThorPowers`, `ThorMoves`, `ThorStatePayload`, `ThorBlow`, Thor's table in `GameCharacter`,
`client/ClientThor`, `client/ThrownHammerRenderer`, `client/ThunderGauge`, `client/motion/ThorMotion`,
`client/motion/ThorPull`, `client/pose/ThorPoses`, `client/pose/ThorBody`, `client/pose/ThorHammerLayer`,
`client/pose/ThorFirstPerson`, `client/blow/ThorFists`, `client/blow/ThorBlowKeys`, `MultiversePowers` (the import of
`ThrownHammer`), `en_us.json`. Engine: `engine/world/ChunkPreloader` (a `hold` for one spot) and
`engine/target/Targeting` (the chain step, moved out of `LightningSpell`).

Methods called across the new package line (`ThorMoves.tell`, `ThorMoves.spare`, `ThorMoves.flying`,
`ThorCharge.hammer`) become public.

### 6.2 The hammer on the server

```
State: HOME (belt or right hand, by `armed`)   OUT   RESTING   BACK
Rest:  LYING   STUCK (with the face it is in)   HANGING
Throw: RETURN (click)   STAY (sneak + click)   FOLLOW (hold)   STORM (flight)
```

| From | Event | To |
|---|---|---|
| HOME, in hand | right click, sneak + right click, letting go of hold right, Storm Throw | OUT as RETURN, STAY, FOLLOW or STORM (FOLLOW starts the pull as well) |
| OUT | stops (creature, block, end of reach) as RETURN or STORM | BACK (STORM strikes its ring first) |
| OUT | stops as STAY or FOLLOW | RESTING: the block face it hit decides (top: LYING; side or underside: STUCK; none: HANGING) |
| OUT, RESTING | Call (scroll click), or hold space (call to fly) | BACK, called (`CALLING` shown, path hits) |
| RESTING | Thor arrives (the pull) | HOME: right hand (lands) or left hand (flies on) |
| RESTING | leash, another dimension, entity gone | HOME, belt |
| BACK | within catch range | HOME: right hand; left hand if flying or called to fly; belt if his right hand holds a creature |
| any | `leave` (death, logout, character off), server stop | HOME, entity discarded (as now) |

Every tick, per state (one effect per hammer through `Effects.start`, as now):

- **OUT:** as now (`LoadedWorld.clip` along each step, hits through `Targeting.mayStrike`), plus: the face it hit
  decides the rest; it reaches 24 blocks as RETURN, STAY or STORM, the drawn distance (4 to 14) as FOLLOW.
- **RESTING:** a crackle every 10 ticks; `ChunkPreloader.hold` on its spot every 10 ticks; LYING checks what holds
  it up (nothing solid under it: it falls, game gravity, until it lands and lies again); STUCK checks its block
  (gone: it falls); in a fluid it sinks at 0.1 a tick; a solid block in its own spot lifts it onto that block; below
  the world's bottom it comes home; the leash every 20 ticks.
- **BACK:** speed eases up from 1.0 to 3.2 (called) or 2.6 (by itself) in 4 ticks; each creature on the step is hit
  once (`Targeting.mayStrike`, pushed aside out of the line, scaled by `ThorCharge.hammer`); caught within 1.8 × his
  size, as now.

`LONGEST` (300 ticks) stays for OUT and BACK only; RESTING has no time limit.

### 6.3 The pull on the server (`HammerPull`)

- Started by `Mjolnir` when a FOLLOW throw leaves his hand, or by `hammer_follow` while the hammer rests (refused
  past 24 blocks or in lava). It lives in `ThorMoves.pull`, as the dive lives in `ThorMoves.dive`.
- First the wait: for 10 ticks a zigzag line from his hand to the hammer (`ParticleFx.zigzag`, brighter each tick);
  then the dash.
- Every tick: `resetFallDistance`, `connection.aboveGroundTickCount = 0` (the dedicated server's flying kick), flag
  `PULLING`; during the dash a zigzag streak from where he was to where he is (`ParticleFx.zigzag` in the colours of
  `bolting`).
- Arrival: his game sends `hammer_follow` with `Characters.SLAM` (a move's landing: its cooldown never refuses it),
  accepted while the hammer rests and he is within catch range plus 1.5 blocks. Fallback: the server catches by
  itself within catch range. Either way only once.
- The outcome by the rule of 4.6 (ground within 2.5 blocks straight below the hammer, by `LoadedWorld.clip`):
  landing puts the hammer in his right hand, no fall damage for 40 ticks; flying on sets him flying in `ThorMoves`
  without the take-off show, hammer in his left hand. Then `CATCH` with the hand.
- Ends without a catch after the wait plus the trip's time at the set pace plus 20 ticks, on a hit of `STUN` or more
  (`ThorMoves.onHurt`), on a Sky Shockwave (`SkyMoves.ground`), or on `leave`. The hammer keeps resting; no fall
  damage for 40 ticks.

### 6.4 His own game (`ThorPull`, `ThorMotion`)

`ThorPull`, every tick of his movement input:

```
h = his hammer (the ThrownHammer whose OWNER is him, looked up near him)
gone, or flying back                     -> stop
the first 10 ticks (the wait)            -> stand still in the reaching pose
still flying out after the wait          -> keep waiting until it rests
goal: resting high (he will fly on)      -> his chest at the hammer
      resting low  (he will land)        -> his feet on the ground 1.2 blocks short of it, on his side
speed = PACE * ramp(dashAge / 3), capped by braking: sqrt(2 * BRAKE * gap)
        (PACE 0.545 blocks a tick: with its 3-tick ends, 6 blocks take 14 ticks)
horizontalCollision                      -> add 0.6 upward; stuck for 8 ticks -> give up
resting and gap < 1.4 * size             -> arrive: send the SLAM and act on the outcome at once
                                            (fly on: flying, no lift, 40% of his speed kept;
                                             land: he drops the last bit)
dashAge > trip / PACE + 20               -> give up
```

The arrival rule runs in his game from the hammer's synced `REST` and his own world, so he flies on or lands
without waiting; the server's `CATCH` corrects him if the two ever disagree.

Further in `ThorMotion`:

- `state()` adds `HOME`, `AWAY` and `RESTING` from the view's flags.
- Any throw sets `THROWN` in his own view at once (as `mjolnir` flips `ARMED` now), so the scroll wheel is Call
  without waiting for the server.
- `flight` with the hammer away: he waits in a calling pose and starts flying when the server's `TAKE_OFF` comes
  (the hammer landed in his hand).
- In flight with the hammer out (the Storm Throw): no steering, sinking 0.03 a tick until it is back; blink, dive,
  bolt, shockwave and Lightning Speed are refused on both sides meanwhile.
- Throw and Follow is a held ability: its press (once held 6 ticks) starts the draw on the server (`COCKED`, so the
  others see it); its let-go throws, with the drawn distance in tenths of a block in its data, as the dash sends
  its length (`ThorMotion.act`; the server keeps it within 4 to 14). Held 3 s, his game lets go by itself.
- While drawing, his own game shows the mark where the hammer would stop: the first block or creature along his
  look, else the drawn distance.
- While pulled, his other moves are refused in his game and on the server.

### 6.5 What is sent

`ThorStatePayload` (its fields are VarInts, so new bits cost nothing):

| New | Value | Meaning |
|---|---|---|
| flag `RESTING` | 512 | the hammer rests in the world (set together with `THROWN`) |
| flag `PULLING` | 1024 | he is being pulled to it |
| flag `CALLING` | 2048 | it flies back to him: his arm reaches for it |
| flag `COCKED` | 4096 | he draws it back for Throw and Follow |
| move `CATCH` | 14 | he caught it; arg 0 right hand, 1 left hand, 2 belt |

`THROWN` keeps meaning "not on him" (out, resting or back), so every check that uses it now stays right. The throws'
and the Storm Throw's poses go as `STRIKE` with two new `ThorBlow`s of `Kit.MOVE`, as the uppercut does:
`HAMMER_THROW` (right arm) and `STORM_THROW` (left arm), added at the end of the enum (its ordinal is what is
sent); his own game shows them at once through `predictBlow`, and the combo never picks a `Kit.MOVE` blow. `PULL`
stays and now goes to everyone at the throw: his own game starts the wait and the dash from it, the others pose the
wait for its first 10 ticks and the dash after that, as long as `PULLING` lasts. Flying with the hammer gone
(`FLYING` together with `THROWN`) is the hang pose, with no flag of its own. His game opens the catching hand by
itself once the hammer is 3 ticks away.

`ThrownHammer` synced data:

| Field | Type | Meaning |
|---|---|---|
| `OWNER` | int | the entity id of the Thor it belongs to, so every game finds "his" hammer, also one that only just saw him |
| `REST` | byte | 0 out, 1 lying, 2 stuck, 3 hanging, 4 back |
| `FACE` | byte | the face of the block it is stuck in |

Only a real connection encodes these (singleplayer passes payloads as they are): checked in a multiplayer test
(§8).

### 6.6 Abilities and buttons

`ThorPowers` needs bits: `UNARMED 1`, `ARMED 2`, `WALKING 4`, `SPRINTING 8` (as now); new `HOME 16` (the hammer on
him), `AWAY 32` (not on him), `RESTING 64` (resting in the world). New holds `FOLLOW_HOLD = 10` and
`STORM_HOLD = 10`; `LEAP_HOLD` becomes 6 (the draw starts), with `LEAP_FULL = 20` (the full draw) and
`LEAP_LONGEST = 60` (it lets go by itself).

Thor's table in `GameCharacter` (slots 18 to 20 are free):

| Slot | Id | Button | When | Needs | Cooldown (ticks) | Damage (half hearts) |
|---|---|---|---|---|---|---|
| 7 | `hammer_throw` | right | ground | `ARMED`; crouch `ALTERNATE` (sneaking: Throw to Stay) | 30 (as now) | 7 (as now) |
| 8 | `hammer_leap` | hold right, 6, `held()` (let go throws) | ground | `ARMED` | 100 (as now) | 4 (as now) |
| 9 | `mjolnir` | scroll | ground | **`HOME`** (new) | 10 | |
| 10 | `charged` | hold scroll, 40 | ground | **`HOME`** (new) | 900 | |
| 18 | `hammer_call` | scroll | ground | `AWAY` | 10 | 4 |
| 19 | `hammer_follow` | hold scroll, 10 | ground | `RESTING` | 40 | |
| 20 | `storm_throw` | hold scroll, 10 | flying | | 200 | 6 |

`ThorPowers.use`: `hammer_throw` reads `Characters.SNEAKING` for STAY; `hammer_leap` starts the draw when pressed
and throws when let go;
`hammer_call` calls `Mjolnir.call`; `hammer_follow` starts `HammerPull`, and with `SLAM` is the arrival;
`storm_throw` throws as STORM; `flight` with the hammer away calls it to fly; the flight moves other than blows
refuse while the hammer is out.

`en_us.json`: the three names (`ability.welcomescreen.thor.hammer_call` "Call the Hammer",
`ability.welcomescreen.thor.hammer_follow` "Follow the Hammer", `ability.welcomescreen.thor.storm_throw` "Storm
Throw"), the needs names (`screen.welcomescreen.character.needs.home` "hammer with you",
`screen.welcomescreen.character.needs.away` "hammer away", `screen.welcomescreen.character.needs.resting` "hammer
resting") and the name of each new setting.

### 6.7 Engine additions (general, naming no character)

- `ChunkPreloader.hold(ServerLevel, Vec3, int key)`: keeps the chunk of one spot loaded with the existing timed
  ticket. Asked again every 10 ticks; it lapses by itself 100 ticks after the last ask, so nothing stays loaded if
  the caller stops asking.
- `Targeting` gets the chain step that is now inside `LightningSpell.chain`: the nearest creature not struck yet,
  within reach, with a clear path, filtered by the caller. `LightningSpell` calls it from then on and does exactly
  what it did before.

### 6.8 Numbers (first guesses, all world settings)

| Ability | Setting | Default | Meaning |
|---|---|---|---|
| `hammer_throw` | `stayBlocks` | 128 | how far away a resting hammer waits for him |
| `hammer_throw` | `throwBlocks` | 24 | how far the plain throw and Throw to Stay go |
| `hammer_leap` | `throwBlocks` | 14 | how far a full draw throws |
| `hammer_leap` | `dashSpeed` | 11 blocks a second | the dash's pace once going (6 blocks in 0.7 s); Follow the Hammer too |
| `hammer_follow` | `reachBlocks` | 24 | how far away a resting hammer may be to dash to it |
| `hammer_call` | damage | 4 (2 hearts) | to each creature the hammer passes flying back |
| `storm_throw` | damage | 6 (3 hearts) | to what it strikes |
| `storm_throw` | `chainDamage` | 4 (2 hearts) | to each leap |
| `storm_throw` | `ringBlocks` | 4 | how far the ring reaches |
| `storm_throw` | `leaps` | 5 | the most leaps |

Fixed in the code: the wait (0.5 s); the draw (4 blocks after 0.3 s, 14 at 1 s, let go by itself after 3 s); every
throw flies out at 2.2 (as now), a called hammer returns at 3.2, one coming back by itself at 2.6 (as now); the Storm
Throw's 24 blocks; the ground rule's 2.5 blocks; giving up after 8 stuck ticks; the catch's 1.5 blocks of slack.

---

## 7. Build phases

Each phase works on its own, is tested (§8), updates `docs/POWERS.md`, `CHANGELOG.md` and `en_us.json`, and is one
release (commit and push only after your yes).

| Phase | What | What you see in the game |
|---|---|---|
| 0. Groundwork | the `hammer/` package and its chain; `ThrownHammer` moved, with `OWNER`, `REST`, `FACE`; the needs bits; `ChunkPreloader.hold`; the chain step in `Targeting` | nothing: the game stays exactly as it is (checked against the old code) |
| 1. It stays | resting lying, stuck or hanging, drawn so; the world moving it; the leash; its chunk kept; Throw and Follow's unreached hammer stays; Throw to Stay | throws come to rest and stay |
| 2. Calling | Call the Hammer: its flight, path hits, wrench-out, catch into the hand or onto the belt; the calling and catch poses, also in your own view; taking off with it away calls it first; `THROWN` set at once in his own game | call it back from anywhere near |
| 3. The dash | `HammerPull` (the wait's lightning line, streak, flight safety, arrival, outcome); `ThorPull` rewritten (wait, set pace, brake, lift, give up); Throw and Follow's draw and its mark; Follow the Hammer; the poses for everyone; right and scroll holds in `ThunderGauge` | the wait, then the dash: landing or flying on |
| 4. Feel | the throw pose and the release from the hand toward the crosshair; the head-first return; the plain throw's return hits; the hand-over on touching down; chunks loaded ahead at Lightning Speed | throws and catches with weight |
| 5. Storm Throw | the left-hand throw, the hang, the ring and the chain, back into the left hand | the concept's combo |

Phase 1 is what 2 and 3 stand on; 4 and 5 build on all three.

---

## 8. Tests

- **Unit tests** (`src/test/java`; the game does not start, `Vec3` works): the rest a stop makes from the face it
  hit; the arrival rule; the dash's pace (6 blocks in 14 ticks, 14 blocks in about 29; it arrives, never passes its
  goal and slows over the last blocks); the draw (hold time to distance, 4 to 14 blocks); the flags and needs bits
  are each a single bit of their own; the chain step's choice from plain points (nearest, skips what was struck,
  within reach).
- **In the game** (a temporary test class behind a flag file, as CLAUDE.md "In-game tests" describes; FOV 70;
  contact sheets; per-tick logs of positions and states):
  - every kind of throw at a zombie, a wall, the floor and the sky: how it rests, a shot of each pose;
  - Call from each kind of rest, through a row of zombies (who is hit, each once), the catch into the hand; with a
    grabbed creature in hand: onto the belt;
  - Throw and Follow at several draws (4, 6 and 14 blocks) to the floor, a wall low and high, a ceiling and the
    sky: the wait is 10 ticks, 6 blocks take 14 ticks, the outcome, no fall damage, flight afterwards; walled off:
    he gives up, the hammer stays;
  - Follow the Hammer after Throw to Stay; refused in lava;
  - holding space with the hammer away; Storm Throw at a group (how many are struck, the leaps, how long he hangs,
    the return);
  - the leash (walking 140 blocks away), another dimension, breaking its floor or block, water, the void;
  - from a side camera (an armor stand, as CLAUDE.md describes) the poses as the others see them.
- **Multiplayer** (`runServer` and one client): the new payload values and entity data are encoded and decoded
  right; with `allow-flight=false` no kick during the pull and the hang; logging out with the hammer resting: it is
  gone and he has it on his return.
- **Phase 0 against the old code:** the same throw and follow test run on `git archive HEAD` (CLAUDE.md: a
  refactor that must keep the game the same), screenshots and logs compared pair by pair.

---

## 9. Decisions

**Decided (2026-10-02):**

- Throw and Follow becomes the timed dash: hold to draw (the longer, the further: 4 to 14 blocks), let go to throw,
  half a second of wait, then the dash at a set pace (6 blocks in 0.7 s; longer trips take longer).
- The plain throw goes at most 24 blocks (was 40).
- The hold's timing was left to me: the draw starts at 0.3 s and is full (14 blocks) at 1 s.

**Still open, answer by number.** The numbers in §6.8 are first guesses and all of them are settings.

1. **Flight needs the hammer**: holding space with it away calls it into his left hand first (today he flies
   without it).
2. **The scroll wheel is the hammer button**: with the hammer away, click is Call and hold is Follow the Hammer.
   Take Up and Charge then need the hammer on him, so he can no longer charge himself while it rests.
3. **A resting hammer stays** until called, followed, or he is 128 blocks away (today it flies back after 5 s).
4. **The arrival rule**: ground within 2.5 blocks below the hammer, he lands with it in his right hand; otherwise
   he flies on with it in his left.
5. **New: Throw to Stay** (sneak + right click).
6. **New: Call the Hammer**, hitting creatures on its way back; the plain throw's return hits too.
7. **New: Follow the Hammer** (hold scroll 0.5 s while it rests): the same wait and dash, up to 24 blocks.
8. **New: Storm Throw** in flight (hold scroll 0.5 s, up to 24 blocks), with the hang while the hammer is out.
9. **The draw's mark**: while drawing, a small mark in your own view where the hammer will stop.
10. **Build order** as in §7, one release per phase.

---

## 10. Later (fits on this system, not designed here)

- Taking a grabbed creature along into a jump or flight (from the concept list): the grab would first need a phase
  in which he keeps holding it; holding space would then take off with it.
- A charged hammer at rest zapping creatures out to hurt you that come near it ("lightning stays on the ground
  while charged").
- A called hammer smashing through glass and leaves where the world lets powers break blocks (`BlockRules.mayBreak`).
- A marker at the edge of the screen pointing to where your hammer rests.
- "You are not worthy." when another player tries to use it.
