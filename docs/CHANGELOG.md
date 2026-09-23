# Changelog

What changed in the project, newest first.

## [Shockwave key, a ring that lasts far longer, and a calmer power bar] - 2026-09-23

### Added
- **Shockwave (key X, ability 8).** The slam of a landing at full speed, whenever you want it: your ring fist
  smashes into the ground and one of the 32 giant constructs strikes and sends a shockwave over it (6 hearts in
  the middle, half at the edge 5 blocks out). On the ground it goes off at once. Flying, you dive straight down
  at full speed and slam where you hit the ground. Jumping or falling, you drop straight down with your fist
  cocked, and the ring breaks your fall. 1.6 power, 5 seconds cooldown. Its damage, reach, push and cost are
  the landing slam's too; in the settings they moved from Flight to Shockwave.

### Changed
- **Everything the ring does costs 60% less power**: a bolt 0.16, the beam 0.8 a second, the shield 0.08, the
  dome 0.24, the Giant Fist 1.6 to 3.2, a slam 1.6, and flying 1.07 a second, so a full ring keeps you up for
  93.75 seconds (0.8 to take off). Settings you never changed take the new numbers by themselves.
- **The power bar no longer blinks.** It drops at once when the ring pays and leaves a gold trail of what it
  paid that runs out a moment later; it glides up as you recharge; the cost of a fist you charge is a steady
  striped piece with a line where you will end up; marks show the quarters; the drain a second is shown in gold,
  now also the shield's small one.

### Fixed
- **A slam that starts just before you touch the ground no longer leaves you hanging in the air** until you get
  up again.

## [Recharging in flight, 32 solid slam constructs and a hero's landing] - 2026-09-23

### Added
- **32 landing-slam constructs**, one picked at random every time, each one something that would really make a
  shockwave: things that drop out of the sky (a fist, a hammer, an anvil, a boot, a ton weight, your lantern, a
  safe, an anchor, a spiked ball, a barbell, a bell, a slapping hand, a sword, a piano, a toy brick, a stamp, TNT,
  a meteor), things that clap shut (hands, fists, cymbals, a bear trap, a book), things that burst out of the
  ground (an uppercut, spikes, a toppling pillar), things swung down (a fly swatter, a pickaxe, a gavel,
  drumsticks on a drum), the emblem falling flat and a volley of rockets.
- **A hero's landing.** Diving into the ground at full speed you swing upright just before it, fist cocked, then
  come down on one knee and smash your ring fist into the ground, which cracks open and throws up chunks. In
  first person your view dips to your fist in the ground and comes back up for the construct.
- **Flying into the ground looking down lands you**, also slower than a slam.

### Fixed
- **The landing slam never went off.** The ground was only noticed when your speed down came out at exactly
  zero, which the game's gravity never allows; now a dive into the ground at full speed really slams.
- **A slam the server does not believe still lands you**, instead of leaving you flying along the ground.

### Changed
- **Constructs are always solid.** The slam constructs grow out of the ring's light and break into solid pieces
  at the end instead of fading; the shockwave is a ring of solid hard light. The shield, the dome and the ram
  cone are solid to everyone else; only from your own eyes (or from inside a dome) you look through them.
- **More energy in the suit, along straight lines** out of the core: over the chest, the back, the flanks, both
  arms, the legs and the head, the thickest and fastest to the ring.
- The slam constructs strike a moment later (half a second after you land) and stay a little longer, so you can
  see them.
- **Recharge works in the air**, once the take-off is over. You keep flying while the lantern comes out; seen
  from outside it hangs upright from your hand however your body lies, in first person the wind pushes and
  shakes it, and the hit sends rings of light out around the way you fly. Recharging while an empty ring lets
  you sink makes you fly again.
- **The settings screens are translated** (English and Dutch): every number has a short name and an
  explanation.

## [Flight, and the ring takes the mouse] - 2026-09-22

### Added
- **Flight (key C).** Green Lantern brings both fists to his chest, sweeps his arms down along his sides,
  looks up and rises off the ground, then flies on without a break. Hold forward to fly where you look, up
  to 50 blocks a second: half again as fast as an elytra with firework rockets. Let go and you glide to a
  hover; jump rises, sneak sinks. Your body lines up with the way you fly, banks into turns and leaves a
  streak of light behind you. You need at least 5 ring power to take off, and flying costs 6.7 a second, so
  a full ring keeps you up for 15 seconds. Press C again to stop. When the ring runs dry in the air you sink down gently with
  your arms up and land unhurt.
- **Fighting in the air** uses the same buttons: bolts take your own speed along, so you never overtake
  them; the beam makes strafing runs; the shield turns into a pointed ram cone that throws creatures away
  and hurts more the faster you fly (about 9.5 hearts at top speed); the dome works as a brake that halves
  your speed.
- **Click or hold.** Every mouse ability has a quick side (a tap) and a lasting side (hold the button for
  2 seconds). An arc beside the crosshair fills up while you hold. Keys you hold anyway, like the Giant
  Fist, work as before.
- **Light Bolt (tap left click).** With both hands empty a small bullet of hard light leaves the ring and
  flies exactly where the crosshair points, bursting on the first creature or wall it meets. 3 hearts,
  1 ring power, one every 0.3 seconds.
- **Light Beam (hold left click).** A steady beam of hard light pours out of the ring along your crosshair
  for as long as you hold: through a whole row of creatures, up to 40 blocks, 10 hearts a second for
  5 ring power a second.
- **Light Shield (tap right click).** Tap to put up a round pane of hard light in front of you, held by
  your left hand; tap again to drop it. It takes 70% off every hit from the front and costs 0.5 ring power
  a second. Damage that goes through armour anyway, and arrows that pierce, go through it as well.
- **Light Dome (hold right click).** A dome of hard light all around you for as long as you hold: 40% off
  every hit from every side, 1.5 ring power a second.
- **A character can sit on the mouse.** While it does, and both your hands are empty, the game's own left
  and right click do nothing at all: no mining, hitting, placing or using. Pick anything up and the mouse
  works as it always did. The panel shows these abilities as `[Left Button]` and `[Right Button]`.
- **The uniform lights up while the ring works.** The lantern on your chest glows like a core, lines of
  green energy run over the suit and down your right arm into the ring, and the ring flares. The harder the
  ring works, the brighter all of it; a resting ring leaves the suit as it is.
- **The panel shows how fast the ring drains** while something keeps drawing on it, and in the air how
  many seconds of flight are left.

### Changed
- **The ring is bigger and not see-through.** A silver band, a dark setting and a glowing green stone,
  solid metal that catches the light; the stone's colour shows how full it is.
- **The Giant Fist lands under your crosshair.** The middle of the fist now goes exactly where you aim,
  swinging over in a smooth curve from the side it charges on.
- **A smaller, dearer and harder fist:** a full charge is 5.7 blocks across instead of 8.8, costs 20 ring
  power instead of 11 and hits for 14 hearts instead of 7. It also has more detail: curled fingers, a
  folded thumb, knuckles and a cuff at the wrist.
- **The beam from ring to construct** now leaves the hand the body really reaches with, runs thin at the
  stone and full where it meets the construct, and stays a cord instead of a fat bar right in front of your
  eyes.
- **The lantern's light is all green** when you recharge, and it does not fly as far: a burst in front of
  the lantern instead of a beam across the room.
- **The lantern is held closer in first person**, so the arm holding it no longer reaches back past your
  eyes, where the edge of the screen cut it open and you could look straight through it.
- **No recharging in the air.** The lantern needs the ground: land first, then recharge.

## [Ring power, the lantern, and a fist that charges beside you] - 2026-09-21

### Added
- **Ring power (Green Lantern).** The ring holds 100 power. A full ring glows bright on your right middle
  finger; the emptier it gets the dimmer it glows, and a nearly empty ring sputters. Everyone around you
  sees it.
- **The power in your panel:** a bar with the number next to it. While you charge the Giant Fist, the part
  it will cost blinks at the end of the bar. It turns red, and the fist says "no power", once the ring
  cannot pay for the smallest fist.
- **Recharge (key Z).** The lantern appears in your left hand and you snap it up; your right fist, the one
  with the ring, swings up over it and smacks it on the back. The light blasts out of the front, away from
  you, and the ring drinks 50 power from it, so two hits fill an empty ring. It takes under two seconds,
  can be done once every 3 seconds, and everyone around you sees it.
- **One mouse rule for every construct**, shown on every slot of the Construct Wheel: left click attacks
  (right hand), right click defends (left hand).
- **Seven new settings** for the Giant Fist: its biggest size, how long a full charge takes, what the
  smallest fist costs, what a full charge costs, how hard a full charge hits, how hard a block may be for
  it to smash and how many blocks it may smash. Recharging has one of its own: how much power it gives
  back.

### Changed
- **Constructs are solid now.** Hard light is a thing, not a haze: green sides that catch the light like
  blocks do, with a glow around them. You cannot see through a construct any more, not even its own far
  side, and nothing thins it out when it hangs close to you.
- **No more wire over a construct.** A bright line runs only along the edges where the shape ends against
  what is behind it; the sides you look straight at are plain green mass.
- **The Giant Fist smashes soft blocks** on its way: dirt, sand, stone, wood, glass and leaves break and
  drop. Iron, diamond, obsidian and bedrock hold, and chests and other blocks that keep something in them
  are left alone. One fist smashes at most 150 blocks, so it punches a hole and no more.
- **Your right arm reaches out** to the construct you are holding, in your own view as well, and the beam
  leaves the stone of your ring. It keeps running from the ring to the construct for as long as it lasts,
  in flight as well.
- **The Giant Fist charges beside you**, on your right, instead of in front of your view: a small one at
  chest height, a big one standing on the ground. It starts one block across and grows slowly while you
  hold R: 8.8 blocks across after 4.1 seconds.
- **It finds room by itself.** When it would go through a wall or the ground, it flows smoothly higher up,
  above your head or to your left, and keeps charging there. When its first spot is free again it goes
  back.
- **The Giant Fist costs ring power:** 10 for the smallest and a little more for every half second of
  charging, 11 for a full charge. The ring pays when the fist flies; letting it fall apart (crouch) costs
  nothing.
  With too little power, R tells you which key recharges the ring.
- **The ring sits on top of your middle finger**, on the back of your right hand, where you look straight at
  it in first person. It used to sit on the side of the hand, by the thumb, and it is much smaller than it
  was: a solid little band with its stone, close to your fingertips, instead of a glowing plate over your
  hand. The band is green as well now, not pale, so the ring reads as one thing.
- **A bigger Giant Fist hits harder:** 6 hearts with a tap, growing with its width up to 7 hearts for a
  full charge. It used to do 6 hearts at every size.
- **No build-up animation and no ring around the fist any more:** it simply appears beside you and grows.
- **The Construct Wheel's writing stays above the wheel and never overlaps it.** On a small screen the
  wheel shrinks and the lowest lines of writing make way. The bar above your hotbar and the panel with your
  abilities are hidden while the wheel is open.
- **The settings screen grows with an ability's list of numbers**, and on a small screen its rows move
  closer together, so its buttons are never covered.

### Removed
- The Giant Fist settings for where it formed in front of you: it charges beside you now.
- The old names shown for empty hands (Energy Bolt, Energy Burst).

## [Construct Wheel, and a fist you can see grow] - 2026-09-21

### Added
- **Construct Wheel (Green Lantern).** Tap V to swap straight between empty hands and the last slot you
  had out; hold V and a wheel of twelve slots opens around your crosshair. Flick the mouse at a slot (or
  use your mouse wheel, or click) and let go to take it. The middle is empty hands. Escape or a
  right-click closes it without changing anything. Green light flares out of your crosshair when your
  hands change.
- **Nothing is filled in yet:** every slot is called Placeholder and says Coming soon, the frames where
  the pictures go are still empty, and your pick stays on your own screen.
- **A bar above your hotbar** shows which slot you are holding. Empty hands are normal, so nothing is
  shown then.
- **Crouch while you hold R** and the Giant Fist falls apart instead of firing: no shot, no cooldown.
- **A ring around the Giant Fist fills up** as it grows, and beats along with it once it is at its
  biggest.
- **Three new settings** for the Giant Fist: how far in front of you it forms, how much further back it
  goes as it grows, and how far to the right of your crosshair it hangs.

### Changed
- **The Giant Fist hangs closer and really does grow.** It used to move back exactly as fast as it grew,
  so a full-size fist looked no bigger than a small one; now it only moves back part of the way.
- **The Giant Fist no longer blocks your view.** It keeps the same spot on your screen wherever you look,
  to the right of your crosshair and a little below it, and swings a little further out as it grows.
- **The Giant Fist no longer flies off at strange angles.** It still flies at what is under your
  crosshair, but never turns more than a small amount away from the way you look.
- **The beam from your ring to the fist is no longer cut off** halfway: it is drawn in pieces now, so the
  part nearest your eyes shows as well, and it thins out towards your hand.
- **A better build-up:** the ring throws off a flare of light as it lets go, and a finished fist snaps
  together with a shockwave running out of it.
- **The ring sits on the middle finger** of your right hand, as a small band with its stone on top,
  instead of a large glow on the side of your hand.

## [Giant Fist: hold to grow] - 2026-09-21
### Added
- **Hold R to make the Giant Fist bigger.** It grows while you hold the key, up to twice its size after
  3 seconds, and shoots off when you let go. A tap still gives a normal-size fist. A bigger fist hits a
  wider area; the damage stays the same.
- **Feedback while it grows:** light runs down the beam into the fist, specks land all over it, and a hum
  rises; a chime and a steady throb tell you it is at its biggest.

### Changed
- **A new build animation:** the ring flares, a twisting stream of light runs from it to where the fist
  will be, gathers there into a glowing ball with spinning rings, and the fist unfolds out of that ball;
  a bright wave runs over it from the forearm to the knuckles as it turns solid.
- **The cooldown starts when you let go of R**, so holding the fist never eats into it. Pressing R while
  it cools down tells you how long it has left, and keeping R down starts the fist once it is ready.
- **A fist right next to you fades out close to your eyes**, so a big one never fills your screen.

## [Green Lantern's uniform] - 2026-09-21
### Added
- **The ring makes Green Lantern's uniform** over your own clothes: the green and black suit, white
  gloves and a green mask; your own face and hair stay. A line of green light slides down your body and
  leaves the uniform behind it; changing back, it slides up and the uniform falls apart into green sparks.
  Everyone around sees it.
- **A glowing green ring** on your right hand, also on your own arm in first person.

### Changed
- **Giant Fist is half again as big** (about three blocks across) and reaches 32 blocks instead of 24.
- **The ring builds the fist** out of specks of light flying in from all around, from the wrist to the
  knuckles, instead of it simply popping up.
- **The fist goes through the ground and walls** instead of bursting on the first block, so it also hits
  creatures behind a wall. It still never breaks blocks.

## [Green Lantern: Giant Fist] - 2026-09-21
### Added
- **Green Lantern's first power: Giant Fist** (key R, ability 1). A huge fist of green hard light forms
  in front of you and shoots off at what you aim at. Every creature in its way takes 6 hearts and is
  thrown far; the fist stops at the first wall and never breaks blocks. Range 24 blocks, cooldown
  4 seconds. A beam of light runs from your ring to the fist while it flies.

## [Combat Mode removed] - 2026-09-21
### Removed
- **Combat Mode and Free Mode are gone.** Key K (ability 11) is free for something new and does
  nothing yet. Every tentacle ability works all the time, and the tentacles no longer swing at monsters
  by themselves.

### Changed
- **A tentacle strike is your own hit again**: your weapon, enchantments and critical hits count,
  instead of the flat 4.5 hearts of Combat Mode.

## [Smooth movement] - 2026-09-21
### Changed
- **Everything the tentacles do moves smoothly now.** A tip no longer takes a straight step towards
  where it should be every tick; it is carried there on a spring, so it builds up speed, swings through
  a turn and eases off again instead of starting and stopping with a jerk.
- **Claws turn instead of snapping.** A claw that starts looking somewhere else swings its last stretch
  over, however sudden the change.
- **Steps are longer and softer**: a foot lifts off and sets down with no speed at all, in a wider arc.
- **Climbing eases along the wall** instead of setting your speed anew every tick, and the legs carry
  you with a softer suspension.
- The shield and the portals follow your head between ticks as well, so they no longer step along with
  the server; the energy shield grows and shrinks eased in and out; the Ground Strike spike eases into
  its highest point and slides back down slowing off.
- Tentacles are drawn with more points, so the line itself is rounder.

## [Free Mode] - 2026-09-21
### Added
- **Free Mode** next to Combat Mode on the same key. In Free Mode your own hands fight and the
  tentacles keep out of it: Grab, Multi-Tentacle, Block, Ground Slam, Portal and Ground Strike are off.
  The key shows the mode it would put you in.
- **Claws can turn any way.** A tentacle bends its last stretch round to point its claw where it is
  looking, so looking at you really turns the side the spikes come out of towards you.

### Changed
- **Blocks and Building is gone** from the keys; ability 8 is free for something new.
- **Octopus Rampage lasts 20 seconds** instead of 10.
- **Resting on a school opens it twice as fast** (0.75 seconds), and so does the Back card.
- The resting tentacles sit further out of your own view.
- A claw looks around less often while you stand still.

### Fixed
- **Legs no longer cross.** Every leg stands on the side of you it grows out of, instead of taking a
  place in a ring that could put a left tentacle on your right.
- **Climbing tentacles no longer swap sides.** The hip pair had the quarters of the wall the wrong way
  round, so those two crossed on every climb.
- **The Portal tentacle no longer circles its target for ever**: it turns harder the closer it gets, and
  anything it passes within one step of counts as caught.

## [Tentacle physics] - 2026-09-20
### Added
- **Combat Mode** (key K, ability 11). **Hands:** you hit with what you are holding and the tentacles
  help by themselves, one slow swing at the nearest monster every 3 seconds for 3.5 hearts.
  **Tentacles:** the tentacle itself is the weapon, a flat 4.5 hearts a hit, and your weapon counts for
  nothing.
- **Ceiling climbing.** Jump up against a roof and the claws hook into it: you hang under it upside down
  and move where you look. Tap jump to let go.
- **Idle claws.** Standing still, a claw now and then turns to look around, or back at you.
- **Marked creatures light up**, through walls, so you can see who is on the Ground Strike list.
- **Ground Strike shows itself first**: the tentacles swing up in front of you and the spikes slide out
  of the claws before anything goes into the ground.

### Changed
- **The resting tentacles really point forward** now, each in a lane of its own instead of two mirrored
  pairs, so the claws hang in front of you instead of over your head.
- **Running on the tentacles is about 40% faster** than running on foot, and jumping off them throws you
  higher the more of them carry you.
- **Tentacle Dash goes the way you press** (forward, sideways or backwards) and much further.
- **Ground Strike reaches 32 blocks** instead of 20.
- **Spells open by resting on a school** for 1.5 seconds, as a page of its own, instead of a drop-down
  list under the card.

### Fixed
- **The four tentacles never tangle any more.** Two that want the same piece of air are pushed apart, so
  they stop crossing, mirroring or sitting inside one another. Blocking gave two of them the very same
  spot, which is why they were inside each other there.
- **Going around an outside corner while climbing no longer drops you**: the tentacles feel around the
  edge for the wall that carries on, and keep holding while part of you is still over the wall.
- **Hanging tentacles keep their distance** while the legs find no ground, instead of swinging through
  each other.

## [Schools of magic] - 2026-09-20
### Added
- **Ten schools of magic**: Earth, Air, Fire, Water, Holy, Dark, Ice, Lightning, Nature and Blood. Every
  spell belongs to one of them, and the schools that are still empty say so.
- **Fire Wall** (Fire school): a whirling ring of fire around you for 2 seconds. Anything that touches it
  takes 3 hearts of fire damage, burns for 4 seconds and is thrown outward. You are safe inside it.

### Changed
- **The power screen is a table instead of a wheel.** The characters stand side by side in a bar at the
  top, clearly apart from the magic; the ten schools sit under a dividing line, five cards across.
  Hovering a school drops its spells open right under it, so holding the key, picking and casting is one
  movement of the mouse without a single click.

## [Ground Strike] - 2026-09-20
### Added
- **Ground Strike** (key Left Alt) in the place of Tentacle Sprint. You **mark** your targets first: look
  at a creature and press the key, once for every tentacle you have free, up to four. Press again while
  you aim at nothing and every marked creature gets a tentacle out of the ground right underneath it,
  for 12.5 hearts (a quarter of an Iron Golem) and a throw into the air. Range 20 blocks; crouch + the
  key lets the marks go.

### Changed
- **Tentacle Sprint is gone.** Walking and climbing are one speed again.
- **You pick what you grab.** One press of the grab key is one tentacle on one creature: the one you aim
  at, or else the nearest enemy in front of you. Before, one press filled every free tentacle by itself.
- **The resting tentacles point forward again**, arcing over your shoulders instead of spreading out
  sideways, and they sit further back on your back.

### Fixed
- **Thrown blocks no longer jump across the screen.** A thrown block was moved right after it was
  created, so everyone saw it appear in one place and be somewhere else a moment later.
- **Climbing up to a ceiling is no longer a dead end.** Hanging under a roof, the tentacles now keep your
  head clear of the blocks instead of pulling it into them, so you can carry on upside down.
- **The Portal ability stays near you.** You can start it up to 24 blocks away (was 48), the portals are
  never pulled further apart than the creature itself is away, and nothing of it can open more than 28
  blocks from you.
- **The creature no longer blinks from spot to spot** while it is dragged and slammed: it is moved and
  its speed is sent along every tick, so everyone sees it slide. Only going through a portal is still an
  instant jump, with a flash on both sides.
- **The thrusters are smaller and sleeker:** two slim pods lying along the tentacle with a short flame,
  instead of three fat glowing bars.

## [Numbered ability keys] - 2026-09-20
### Changed
- **Ability keys are only numbered now:** ability 1 up to ability 10. A key is no longer a kind of ability
  (first, second, movement, guard, heavy, ...), so every character can put anything on any number.
- **Crouching + a key is the ability's own choice, not a rule.** For Doctor Octopus: grab lets go (free, even
  on cooldown), ground slam gives the slam around you while your claws stay full, blocks takes the whole
  cluster, feet-or-tentacles steps back instead of forward, the rest changes nothing.
- **Every ability works the same on players as on mobs.** Where the tentacles pick their own targets they now
  pick players too, as long as the server allows PvP and you are allowed to hurt them. A held player cannot
  walk out of the claw.
- **Blocks: taking and setting down are two buttons.** The key always takes another block or cluster (so
  all four tentacles can carry something), the **right mouse button** sets a load down where you aim, and
  the left mouse button hurls it. Before, a second press of the key set the first block down again, so you
  could never carry more than one.
- **The view no longer stretches** while Tentacle Sprint or Block changes how fast you walk.
- **Settings in the game:** Mods > Classes & Stamina > Config now opens a choice: the stamina bar, or a
  character. A character's screen changes the cooldown, the damage and every setting of every ability.
- **The resting tentacles** hang over your shoulders instead of in front of you, out of your own view.
- **All settings live together** in the `config/welcomescreen` folder: one file per character
  (`doc_ock.toml`, `green_lantern.toml`) plus `stamina.toml` for the stamina bar. Inside a character's file
  every ability has its own `[abilities.<name>]` section. Settings from the old files are not carried over,
  so everything starts at its default once.

## [Characters] - 2026-09-20
### Added
- **Characters.** The wheel (hold G) now has two rings: the characters you can turn into on the outside, the
  spells inside. Picking a character turns you into them; picking the one you are turns you back.
- **The same keys for everyone.** Ability keys belong to a slot, not to a character: V is always "the second
  ability", whoever you are. Ten slots, all changeable in Options > Controls.
- **Green Lantern** as a test character: you really turn into him and the screen says so, but he has no
  abilities yet.
- **Settings file per character** (`welcomescreen-characters.toml`): one section per character with one
  section per ability, where every cooldown, every damage number and the abilities' own settings (ranges,
  cluster size, sprint speed, stamina) can be changed.
- **Feet or tentacles** (key C): walk on your own feet, or on 2, 3 or 4 tentacles. Every tentacle you do not
  walk on is free. Walking on 3 or 4, one leg lifts off when an ability needs it.
- **Tentacle Sprint** (hold Left Alt): the tentacles themselves step quicker and further, so you really run,
  on the ground and up walls. No potion effect.
- **Blocks and building** (key X): pull a block, or a whole cluster or small building (crouch + X), out of the
  world, set it back down in the same shape somewhere else, or hurl it at what is in front of you.
- **Smash what you hold into the ground** with the heavy key while your claws are full.

### Changed
- **Grabbing takes several creatures at once:** every free tentacle takes one of its own, and pressing again
  grabs more.
- **Nothing is ever let go by itself.** No timer and no distance limit: only you let go (crouch + the grab
  key), throw it, or turn into someone else.
- **Smashing works again:** swinging your view whips what you hold along, and hitting a wall, ceiling or the
  ground hurts it, harder the faster it went.
- **Climbing spread out:** every tentacle holds its own spot in its own quarter around you, far from the
  others, and prefers edges and corners. One tentacle reaches for a new grip at a time.
- **Roofs and overhangs:** the tentacles find their own way over the edge, around a corner or on underneath a
  ceiling, so you no longer get stuck against them.
- **Portal rebuilt and much clearer:** the tentacle looks around first, a sharp point slides out between its
  three claws, thrusters fold out and light up, and only then does it dive. The three portals are always
  pulled far apart and can never open far away from you. Hunting range 30 blocks, and the slam does 25
  hearts (half an Iron Golem) instead of always killing.
- **Thrusters are back** on the tentacle, with a flame trail while it boosts.
- Octopus Arms is no longer a spell in the wheel; Doctor Octopus is a character.

### Fixed
- Portals no longer open at the world spawn or far away from you.

## [Spells] - 2026-09-19
### Changed
- The tentacles carry you: you stand in the air on the two lower ones, they step over blocks and catch falls.
- Wall Climb rebuilt: you hang at a normal distance and move with WASD, go over the top edge, carry on under
  ceilings and change from wall to wall. Jump pushes you off.
- Every tentacle works on its own: hold several creatures at once while another one strikes.
- Portal Grab is no longer a spell of its own: it is the Portal ability of the Octopus Arms (key N), and the
  rest of the tentacles stay out and keep working while it runs.
- The tentacles are slimmer and smoother, with a new claw; the rocket boosters are gone.
- Block is now a cross of tentacles behind a glowing energy shield, while your legs keep walking.
- Portals open much slower and look better: the ring assembles piece by piece, lamps light one by one, the
  energy tears open from a line, with a tunnel behind it and a turning toothed ring. They close backwards.
- Portal Grab rebuilt: the tentacle slowly grows out of your back, looks around, gets two rocket boosters, shoots
  through the portals after the mob, and rockets it out of a portal high in the sky to smash it dead. Then it
  slowly comes back to you. Portals are now solid 3D rings with a glowing energy field.
- Robot arms move smoothly: no more stutter or flipping, also in first person, and a grabbed mob moves exactly
  with the claw.

### Fixed
- The mod no longer crashes a dedicated (multiplayer) server on start.
- A mob that was held by a robot arm when its area unloaded gets its normal behaviour back.

### Added
- Octopus Arms (replaces Iron Tentacle): four tentacles stay out until you cast it again. Always: Tentacle Reach,
  Tentacle Strike, Wall Climb, and legs that walk beside you. On their own keys: Grab and Throw, Multi-Tentacle,
  Tentacle Dash, Block, Ground Slam, and the ultimate Octopus Rampage. A panel shows keys and cooldowns.
  Dash, Block and climbing use stamina.
- Portal Grab: big, slow tech portals, chains and a homing robot arm that goes through the portals, drags a mob
  through one and smashes it into the ground. Range 48 blocks.
- Iron Tentacle: four Doctor Octopus robot arms from your back; one grabs a mob up to 20 blocks away. Drag it where
  you look, slam it into walls, left click to throw.
- The robot arms and chains are solid 3D arms (metal segments, spikes, a claw with yellow lights) instead of particle lines.
- A spell guide, [Spells](SPELLS.md): how casting works, and every spell's range, damage, timing and controls,
  with a step-by-step timeline for Portal Grab and Iron Tentacle.
- The spell wheel is now an oval, so seven spells fit without overlapping.
- Void Walk: vanish in a dark explosion for 10 seconds. Invisible and untargetable, faster and a bit stronger,
  the world turns into black silhouettes and nearby enemies are outlined for you.
- A spell wheel: hold G, aim at a spell, let go to cast. The key can be changed under "Classes & Stamina" in Controls.
- Four spells for everyone: Fireball, Lightning Strike, Poison Area and Wind Gust. Each has a cooldown.
- Every spell has its own animation, like the ceremonies: a flaming comet and fire burst, a charging rune
  circle with a storm cloud and branching bolt, a thrown vial and bubbling poison cloud, and a rolling wall of wind.

## [Death and level-up animations] - 2026-09-19
### Added
- A one-second death animation where you die, different for every group.
- One level-up animation for everyone when you gain an experience level. Gaining several levels at once plays it once.
- The developer menu can also play every group's death animation and the level-up.

## [Start screen] - 2026-09-18
### Added
- Choosing is now two steps: first a group, then a class. All six groups, all 24 classes and The Forsaken.
- Every group and class shows its description from the design. Hovering previews, clicking picks.
- While choosing you are out of the world: invisible, untouchable, no damage.
- Info panel next to the list. Groups: story, where the power comes from, why to pick it, classes with roles.
  Classes: role, story, special, strong, weak, why to pick it, starting items. Also in the choosing guide in CLASSES.md.

- A gold "Your class is not a cage" box on the welcome screen, and a short reminder on the choosing screens:
  your class only shapes your skill tree, everything else stays open.
- Your class is shown above your inventory (survival and creative). Hover it for details.
- Every class has its own start ceremony: a different animation per class, not just different particles.
- The ceremony plays again every time you respawn after dying, without the class name on screen.
- The ceremony stays on the spot where it started and always finishes, even if you walk away.
- Every ceremony has its own finale, and its sounds are much quieter.
- Ceremonies rebuilt again: every class now has a completely different animation that fits the class
  (shield wall, heartbeat, arrow volley, storm cloud, greatsword from the sky, bubbling brew...),
  not the same animation with a different symbol.
- The first-time ceremony is bigger and longer (up to ten seconds). The respawn ceremony stays short
  but is now its own animation, different from the first time while fitting the same class.
- The Wizard's magic circle is bigger with thin, glowing lines.
- Everything stays close to you and happens all around you, never just in front of you.
- Developer menu to play any class's ceremony at any time.

### Changed
- Choosing screens are tidier: dividers between sections, "Pick if" shown first.
- The stamina bar is hidden in creative and spectator.
- The Forsaken is no longer secret. He is on the start screen like every other class.
- For now every class starts with one bread. The shipwreck scraps come later.

## [Concept Fase] - 2026-09-17
### Status
- Alle mod-code en build-bestanden zijn verwijderd op verzoek van de gebruiker.
- Alleen essentiële bestanden zijn aanwezig:
  - Git repository met `.gitignore` en `.env`.
  - `docs/` map met `PROJECT.md` en `CHANGELOG.md`.
  - `voorbeeld background.png` als referentie voor de gewenste achtergrond.
- Er is nog niets aan de mod zelf gebouwd; het concept en idee staan vast in `docs/PROJECT.md` en kunnen nog vrij aangepast worden.

### Laatste wijziging
- Project teruggebracht naar pure concept- en documentatiestatus.
