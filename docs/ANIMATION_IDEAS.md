# Animation ideas: making creatures move with weight

Ideas for later, worked out but **not built**. They are examples of one direction: creatures that react to speed,
turns, hits and the ground with their whole body instead of playing a fixed animation. Each needs a yes before it is
built, and each would get its own on/off switch in the client settings (off by default).

What the mod already has to build on: pose layers that bend a creature's body on top of its own animation and fade
in and out, feet that rest on the ground, legs placed by reach (knees bending), bodies that go limp with physics, and
the server telling every player near how a blow struck.

---

## A. Leaning and banking (speeding up and turning)

**What you see:** a zombie that sets off hard leans forward a few degrees; one that stops short leans back; one
running round a corner leans into the turn like a bike. It gives weight and momentum.

**Can it be done:** yes, fully.

**How it would work:**
- Each game follows every creature it draws: where it was the last few ticks gives its speed, how fast that speed
  changes (setting off, stopping) and how fast it turns.
- A pose layer tilts the body from the feet: forward by the setting off, back by the stopping, sideways by the turn
  (about 5 degrees at most). The feet stay planted, so the body leans over them instead of sliding.
- The trunk bends at the waist a little more than the legs lean, the way a runner's chest leads.
- A soft spring smooths it, so a creature knocked about by the server's jolts does not twitch.

**Cost and risks:** tiny. Creatures from other mods with odd bodies may lean oddly; those can be left out.

---

## B. Hit reactions that play on top (taking a blow without stopping)

**What you see:** hit from the left, a creature's chest and head snap to the right and spring back within a fifth
of a second, while it keeps walking or striking. Now it only flashes red.

**Can it be done:** yes, fully.

**How it would work:**
- Every player's game already hears when a creature is hurt and from which point the blow came.
- A pose layer adds a short turn of the trunk and head away from that point, bigger for bigger blows, which springs
  back over about 4 ticks. It is added on top of whatever the creature is doing, so the walk or attack plays on.
- Arms swing loose a moment with the jolt; a very hard blow could make it stagger half a step.
- The killing blow already throws the body the way it struck (built this session); this would be the same feel for
  blows it lives through.

**Cost and risks:** tiny. Many hits at once (a sweep through a crowd) each just start their own short spring.

---

## C. Looking round (head and chest turn to what they watch)

**What you see:** a creature's head, and then its chest, turn smoothly to what it watches, while its legs keep
walking the way they go; it no longer turns round as one stiff box.

**Can it be done:** partly. Where a creature looks is decided by its brain on the server (the game already sends
where its head looks); each player's game cannot pick targets itself without going out of step with the server.

**How it would work:**
- Take the head direction the server already sends, and share it out: the head turns most of the way, the upper
  chest a third of it, the hips hardly, each on a soft spring so it swings round with weight.
- Limit how far the head may twist over the chest, so it never turns like an owl.
- Idle creatures already glance about now and then (their own brain does that); the chest would follow those
  glances too.

**Cost and risks:** small. Creatures whose model has no separate head or chest (slimes) are left out.

---

## D. Footsteps and landings from the feet themselves

**What you see:** dust puffs and step sounds exactly when a foot comes down; after a hard fall the knees bend deep
to take the blow and a ring of dust spreads from the feet.

**Can it be done:** yes, with one catch: the game plays step sounds on the server by distance walked. Adding a
second step sound would double it, so either only the dust follows the feet, or the game's own step sound is replaced
by one timed to the foot (only in your own game).

**How it would work:**
- The mod already knows where each foot is while a creature is drawn (it rests the feet on the ground). When a foot
  reaches its lowest point and meets the ground, a small puff of the block under it is thrown up.
- A landing is seen from how fast a creature fell just before it stopped: the harder, the deeper a pose layer bends
  the knees for a few ticks and the bigger the ring of dust. The Hard-Light Mech already lands this way.

**Cost and risks:** small; the dust goes through the mod's batched particles, so a crowd does not flood the game.

---

## E. Squash and stretch (punch on jumps and landings)

**What you see:** a creature jumping or launched stretches a touch taller and thinner; landing it squashes a touch
lower and wider, for two or three ticks.

**Can it be done:** yes, easily, but with care:
- The body going limp only works on a creature drawn at its true shape (so a squashed one never goes limp), so the
  stretch must be off the moment one dies or is thrown.
- Some powers already squash creatures on purpose (the palm that presses them flat); those keep theirs.
- Minecraft's blocky style can take it only a little: about 5 to 8 percent, not the 15 of a cartoon, or it looks
  rubbery.

**How it would work:** just before a creature is drawn its size is scaled a little along its height and the other way
across, from how fast it is rising (stretch) or how hard it just landed (squash), easing back within a few ticks.

**Cost and risks:** tiny. Worth trying together with D (landings).

---

## More in the same spirit (not worked out as far)

- **Stagger:** a hard blow that does not kill makes the creature step back to catch itself, its feet stepping anew.
- **Wounded walk:** a creature low on health favours one leg and hunches a little.
- **Breathing:** creatures standing still breathe: the chest rises, the arms sway a little.
- **Loose arms:** a hard hit sets a creature's arms swinging free for a moment, as limp bodies do, then they take
  their own pose back (a small step towards bodies that are half limp, half alive).
