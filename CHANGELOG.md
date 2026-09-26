# Changelog

What changed in the project, newest first. Every release has its own section, `## [<version>] - <date>`;
the sections before 0.0.1-alpha came before versions were numbered.

## [0.2.8-alpha] - 2026-09-26

### Added
- **Light Beam, five stages:** the beam grows every 2 seconds you hold it, up to stage 5 at 10 seconds. Each stage
  looks bigger and wilder: a thin, concentrated ray at stage 1, a massive, unstable super-beam at stage 5 with energy
  rings, lightning whipping out of it and heavy sparks.
- **Light Beam:** reach grows with the stage, 40 up to 64 blocks (player idea).
- **Light Beam:** a new stage sends a wave of light down the beam, a shock ring out of your fist and jolts your view.
- **Light Beam hold bar:** five pieces next to the crosshair; the first fills while you hold, the rest while the beam
  grows, with the stage shown beside it.
- **Setting:** beam cost and beam reach at stage 5.
- **Thunder Clap:** a bubble of bent light where time slows, rolling ahead with the shock: the world behind it looks
  as through a magnifying glass. Sparks crawl slowly inside it.
- **Thunder Clap:** rings of light ripple over the ground ahead, the first with a swirl in it.

### Changed
- **Light Beam:** stages at 2, 4, 6, 8 and 10 seconds (was 2, 5, 10 and 20); damage and push grow up to x3 at stage 5.
- **Light Beam:** stage 5 costs a fixed 5 power a second for as long as you hold it; the stages before climb to it.
- **Light Beam:** your arm points dead straight ahead, never up: no more swing at the start, the kick goes straight
  back.
- **Light Beam:** one hand at stages 1 to 3; your other hand comes in to steady it from stage 4.
- **Light Beam:** its sparks and specks fly beyond your fist, no longer into your own view.
- **Light Bolt:** the arm kicks straight back, no longer up.
- **Flight:** half as fast: starts at 3 blocks a second, cruises at 3.75, tops out at 4.5; hovering and climbing slow
  down with it.
- **Flight, ram cone:** still rams from half your top speed and hits as hard at top speed as before.
- **Update manager:** "Update now (quits)" replaces "Update & restart": the game saves and quits, the update is put
  in place, and you start the game again yourself.

### Removed
- **Construct Wheel:** Brawler Gauntlets / Boxing Gloves, Dual Energy Daggers, Two-handed War Hammer, Spear /
  Halberd, Dual Micro-SMGs and Rotary Grenade Launcher.
- **Thunder Clap:** the chunks and slabs of ground and the ground bits it threw up (green on grass).
- **Setting:** the beam's last-stage cost as a multiple of the first.
- **Updates:** the automatic restart after an update.

## [0.2.7-alpha] - 2026-09-26

### Changed
- **Thunder Clap:** remade 1 on 1 after Thor's thunder clap in God of War Ragnarök. Arms wide and head back,
  then a clap that blasts forward only: a clear shock bubble, a blinding flash, a spray of thunder sparks,
  slabs of ground heaving up, flying chunks and a rolling wall of mist. No lightning bolts or arcs.
- **Thunder Clap:** hits only in a cone in front of you (9 blocks, about 90 degrees wide); the clap comes after
  0.6 s instead of 0.3 s.

### Added
- **Screen flash and camera shake** when a Thunder Clap goes off near you (the camera shake setting scales it).

### Removed
- **Thunder Clap:** the glowing ring around you and the lightning arcs.

## [0.2.6-alpha] - 2026-09-26

### Added
- **Thunder Clap (Lightning spell):** you clap your hands with a crack of thunder, sparks burst from them and a
  glowing shockwave rolls 9 blocks around you, hurting and throwing back hostile creatures. No lightning bolt.
- **Ability 12:** a new ability key, middle click by default. A key a character has nothing on shows as
  "Placeholder" on the panel.

### Changed
- **Middle click:** stays pick block while your character has nothing on it; an ability there takes it over.
- **Keys shared with vanilla keys** (middle click, X, C): no "nothing on this key" message when they have
  nothing to do.
- **Thor** moved from Marvel to Other on the power wheel.

## [0.2.5-alpha] - 2026-09-26

### Added
- **Western Revolver Assembly (unfinished, work in progress):** hold Left Alt for 2 seconds for a show of two giant
  hands: a cowboy hat, five finger-gun pews, a claw-machine grab, a revolver built from its parts, loading, six shots
  that tear through everything, three grip slams with shockwaves and a high-five goodbye. Animations are still being
  polished.
- **Hold ring:** a ring round the crosshair fills while you hold Left Alt, showing when the revolver show starts.
- **Settings:** revolver power cost, cooldown, distance, reach, damage of pews, shots and slams, knockback.

### Changed
- **Giant Hands:** now come when you let go of Left Alt (a short tap), so a long hold can call the revolver show.
- **Giant Hands and the revolver show:** each has its own cooldown.

## [0.2.4-alpha] - 2026-09-26

### Added
- **Light Beam stages:** the longer you hold, the bigger it gets: wider and stronger at 5 seconds, raging at 10,
  full power at 20. Damage climbs to x3, power cost to x8 (0.8 up to 6.4 a second), each stage flares, roars and
  crackles with more lightning.
- **Light Beam slows you:** the stronger the beam, the slower you walk (85% down to 30%); your speed comes back
  when it stops.
- **Light Cage, dragging:** once lifted, the cage follows where you look and stops against walls.
- **Config: Client or Server first:** the config menu asks which you want; Server can only be changed by the host
  of the world or an operator on a server, checked again by the server.
- **Server settings, Power rules:** damage multiplier, cooldown multiplier and whether powers break blocks.
- **Client settings:** menu theme on or off, how often to look for updates, how long the update note stays in game.
- **Settings:** beam stage damage, cost and walking speed.

### Changed
- **Light Bubble is now the Light Cage:** a real prison cage of hard light (floor plate, bars, hoops, domed roof with
  a ring the beam holds it by) instead of the bubble; it breaks into solid bars.
- **Light Cage:** never lets go by itself any more; it holds until you pound it, let it go or the creature dies.
- **Energy Whip:** at rest the lash hangs curled up in loops below the handle; every attack unrolls it and it winds
  back up after.
- **Energy Whip, taking it out:** the lash now winds out of the handle into a coil, unrolls with a flick, twirls, cracks
  and is pulled back into its coil.
- **Light Bolt and Light Beam:** your arm points dead straight ahead along your view, in first and third person.
- **Update note:** bigger, with your version and the new one.

### Removed
- **Light Bubble setting "Held for"** (the cage no longer lets go by itself).

### Fixed
- **Update and restart (Modrinth App):** the game now really starts again after the update closes it.

## [0.2.3-alpha] - 2026-09-26

### Added
- **Energy Whip (Construct Wheel, slot 2):** a solid hard-light whip, a handle in your fist and a 4.5-block lash
  that tapers to a tuft at the tip; every flick runs down the lash as a wave and cracks at the tip with a flash and
  shock rings. Slack, it hangs, lies on the ground and trails after you.
- **Energy Whip, taking it out:** the handle grows out of your fist, the lash pours out and coils on the ground,
  rises in a wave, twirls over your head and is thrown forward for a crack.
- **Energy Whip, left click:** 12 different lashes, each with its own body movement: forehand, backhand, overhead
  crack, sidearm throw, rising lash, two diagonal cuts, figure of eight, leg sweep, spinning lash, wrist snap and
  cowboy crack. The tip hits 30% harder.
- **Energy Whip, hold left:** whirlwind over your head that hits every hostile creature around you; letting go
  throws one hard crack ahead.
- **Energy Whip, click right:** lasso: winds three times round the creature you aim at, your left hand grabs the
  lash and you yank it down at your feet (damage, slowed).
- **Energy Whip, hold right:** spinning shield before you that turns shots from the front back and stops 60% of
  hits from the front.
- **Settings:** damage and length of the whip, and the whirlwind, lasso and spinning shield.

## [0.2.2-alpha] - 2026-09-26

### Fixed
- **Flamethrower:** walking while you fire no longer slides you over the ground with bent knees; your legs walk
  as normal and the stance only shows while you stand still.

## [0.2.1-alpha] - 2026-09-26

### Added
- **Spells, drawn effects:** every spell now has its own drawn effect on top of its particles: a boiling fireball
  with streaming flames and a trail of heat, a rune and storm cloud with a leader and three lightning strokes, a
  tumbling glass vial and a churning poison cloud, a rolling crescent of wind, and a sphere of nothing for the void.
- **Fireball:** the burst hurts, lights and knocks back every hostile (red) creature within 2.5 blocks; water puts
  the fireball out with a hiss.
- **Lightning Strike:** stuns hostile ones near the strike, then leaps on to up to 3 more (5 in the rain).
- **Poison Area:** slows everything hostile inside; after 2 seconds inside the poison gets stronger.
- **Wind Gust:** turns hostile shots round, blows loose items away, puts out your flames and catches you when
  cast while falling.
- **Void Walk:** your first melee hit out of the void does 50% more, blinds and slows, and ends the walk; faint
  wisps where your feet fall and a warning two seconds before it ends.
- **Flamethrower, stance:** feet apart, knees soft, breathing and shifting weight; the knees really bend.

### Changed
- **Plasma Flamethrower:** 30% bigger, the front grip a little further back.
- **Flamethrower, left click:** a wide level sweep, right to left and back again, 50% further to each side;
  it gathers first and flows into the next, so it no longer snaps into the sweep.
- **Flamethrower, flames:** boiling, flickering flame round a bright core with threads of plasma, sparks, rising
  smoke, fire that splashes where it hits and a pulsing ring at the nozzle.
- **Lightning Strike:** the game's own bolt is no longer drawn, only the spell's own; it still hits the same.
- **Spells:** richer sounds for all five.

### Removed
- **Flamethrower, left click:** the other ten attacks (rising sweep, chop, geyser, overhead, fireball burst,
  lunge, low sweep, spin, cross, corkscrew).
- **Void Walk:** the 20% melee bonus for the whole walk (replaced by the ambush).

## [0.2.0-alpha] - 2026-09-26

### Changed
- **Releases:** old versions now stay on the Releases page; only the makers' own folder keeps just the newest 10.

## [0.1.9-alpha] - 2026-09-26

### Added
- **Report a bug, Suggest an idea:** new **Sent** button: your last 3 reports (or ideas) with their number, date and
  status on GitHub (open, fixed or added, not planned); sending a fourth drops the oldest.

### Changed
- **Report a bug, Suggest an idea:** what you type is kept until you send it, also when you leave the screen, die or
  close the game.

### Fixed
- **Bug reports for the makers:** the script that copies open reports and ideas no longer fails in Windows
  PowerShell 5.1.

## [0.1.8-alpha] - 2026-09-26

### Added
- **Green Lantern's arrival, the ring's voice:** the ring speaks to you while it comes for you, fading in at the
  start and out at the end; only then, and players near you hear it too.
- **Flight, jetpack:** when you pick up speed and lie down into your flight, the ring shoots a ball of light onto
  your back and a hard-light jetpack grows there, green flames roaring from its nozzles (long at speed, small when
  you hover); it breaks into solid pieces when you land or stop flying.
- **Flamethrower, left click:** twelve flame attacks, a different one every click, each flowing into the next:
  sweep, sweep back, rising sweep, chop, geyser, overhead whip, fireball burst, lunge jet, low sweep, spin, cross
  and corkscrew.

### Changed
- **Green Lantern's arrival:** plays exactly 2 seconds slower (about 10 seconds instead of 8), every part the same.
- **Flamethrower, left click:** the sweep and the sweep back are now two of the twelve attacks, picked at random
  instead of taking turns; each attack does its own damage and reach against the sweep settings.

## [0.1.7-alpha] - 2026-09-26

### Added
- **Plasma Flamethrower (construct wheel, slot 16):** works now, with its own taking-out animation: the gun grows
  out of the ring's light piece by piece, you turn its valve, light the pilot flame and fire a test burst.
- **Flamethrower, left click:** a sweep of flame, right to left and back again (2 hearts, 5.5 blocks, 0.3 power).
- **Flamethrower, hold left 2 seconds:** the inferno, a stream of fire 10 blocks long for as long as you hold
  (1.5 hearts every 4 ticks, 1 power a second).
- **Flamethrower, right click:** a wall of fire 3 blocks ahead for 3 seconds that throws hostiles back and burns up
  their shots (3 power).
- **Flamethrower, hold right 2 seconds:** a vortex of fire around you that keeps off half of every hit, hurts
  hostiles in it and bursts outward when you let go (1 power a second).
- **Green afterburn:** everything the flamethrower hits burns green for 3 seconds (1 heart a second).
- **Settings:** the damage, reach and cost of all four flamethrower moves and the afterburn, under the Construct
  Wheel.

### Changed
- **Plasma Flamethrower model:** a front grip under its tank and a valve wheel at its back, on the wheel too.

## [0.1.6-alpha] - 2026-09-25

### Changed
- **Flight:** half as fast: sets off at 6 blocks a second (was 12), cruises at 7.5 (was 15) and reaches its top
  speed of 9 (was 18) after 4 seconds of flying forward (was 7.5).
- **Flight, the dive:** straight down at 9.6 blocks a second (was 19.25), still just above your top speed.
- **Landing slam:** needs about 8 blocks a second when you dive in (was 16), still nearly your top speed.

## [0.1.5-alpha] - 2026-09-25

### Added
- **Giant Hands, settings per hand:** smack, grab, middle finger, slap, pound and axe pair each get a chance (how
  often it comes compared with the others, 0 = never), the most of it in one press, and a damage and knockback
  factor.
- **Power wheel (hold G):** shows your faction in its top-left corner, or "none".

### Changed
- **Sword and shield, the charge:** runs at 12 blocks a second (was 16).
- **Air Strike, the crash:** 75% smaller in everything: the blast (a quarter of its size), its reach (24 blocks,
  was 96), the crater (2.4 blocks, was 9.7), the blocks hurled away (40, was 158) and the shaking round it.
- **Air Strike, the miniguns:** fire 25% slower (2 rounds every 0.19 seconds, was 0.14) and a round does 0.8
  hearts (was 2.25).
- **Air Strike, the jets:** fire their small missiles half as often (about every 2.4 seconds, was 1.2) for 1.25
  hearts (was 2.5).

## [0.1.4-alpha] - 2026-09-25

### Removed
- **Suggest an idea:** the category; an idea now has only a name, a description and a priority.
- **Air Strike:** the wind sounds: the rush of air around the gunship, the whoosh of its jets and the whoosh as it
  dives; its engine hum, guns, missiles and blasts stay.

## [0.1.3-alpha] - 2026-09-25

### Added
- **Suggest an idea:** new button in the update manager, next to Report a bug. Give your idea a name, a
  description, a category (new power, new character, change or other) and a priority, and press Send; it goes to
  the mod's makers the same way as a bug report.

### Changed
- **Update manager:** What's new now takes a full row; Report a bug and Suggest an idea sit together under it.

## [0.1.2-alpha] - 2026-09-25

### Added
- **Factions:** everything alive is red (hostile), yellow (neutral) or green (friendly) to you; factions are
  scoreboard teams and decide it between players.
- **`/faction` command:** create, delete, add, remove, enemy, ally and neutral for operators; invite for members;
  join (when invited), leave and list for everyone.
- **Factions:** hitting something yellow 3 times (you and your faction together) turns it red for your faction
  until 5 minutes after the last hit or its death; a player turned red this way sees your faction as red too.
- **Factions:** green is never hurt by you, not by powers and not by weapons.

### Changed
- **Ring Scan:** marks in three colours (red, yellow, green) and counts hostile, neutral and friendly lifeforms.
- **Homing and area powers** (Giant Hands, Air Strike, Light Bubble slam, landing slam, Octopus Rampage, Ground
  Strike, Poison Area, Wind Gust, Void Walk marks) only go for red.
- **Air Strike:** guns, missiles and jets only go for creatures its scan marked red, the one closest to you first;
  with nothing red marked it holds fire and keeps its bomb bay shut.
- **Air Strike, the miniguns:** 2.25 hearts per round that strikes (was 1.5).
- **Flight:** much faster: sets off at 12 blocks a second (was 4.2), cruises at 15 (was 5.2) and reaches 18 at top
  speed (was 6.3); climbing and sinking stay the same.
- **Landing slam:** needs about 16 blocks a second when you dive in (was 5.6), still nearly your top speed.
- **Air Strike, the crater:** blows 75% more blocks out of the ground: 9.7 blocks from its middle (was 8), with
  158 blocks hurled away (was 90); the smoke and flames after it spread over the bigger crater.
- **Air Strike, the crash:** the blast grows with the crater: six times its original size (was five times), and it
  hurts everything up to 96 blocks away (was 80).

### Removed
- **Air Strike:** the miniguns no longer rake the ground when there is nothing to shoot at.

## [0.1.1-alpha] - 2026-09-25

### Changed
- **Air Strike, the crash:** the blast is five times as big: fireball, mushroom cloud, shell of light, rings, flames
  and smoke, and the view shakes five times as far away.
- **Air Strike, the crash:** hurts everything up to 80 blocks away (was 16); the crater stays the same size.
- **Air Strike setting:** the crash's reach goes up to 200 blocks (was 40).
- **Air Strike, the miniguns:** fire 65% faster, 2 rounds every 0.14 seconds (was every 0.23 seconds).
- **Air Strike, the miniguns:** aim where a creature on the move will be when the rounds get there.

### Fixed
- **Air Strike, the miniguns:** rounds that strike a creature now always hurt it (almost none did).

## [0.1.0-alpha] - 2026-09-25

### Added
- **Giant Hands settings:** Fewest hands (4), Most hands (8) and Time between hands (half a second).

### Changed
- **Giant Hands:** 4 to 8 hands at every press, how many picked at random (was 1).
- **Giant Hands:** a new hand every half second, up to five at once (was one every 2 to 3 seconds, three at once).
- **Giant Hands:** every hand goes for the enemy nearest to you with the fewest hands on it (was a random enemy).
- **Giant Hands, the axe pair:** counts as one hand and no longer holds the other hands back.
- **Giant Hands:** your arm stays out while hands come up one after another, swinging from one to the next.
- **Giant Hands:** hands never come up inside each other or the axe pair; with no room a hand goes for another enemy
  or waits.
- **Giant Hands, the middle finger:** rises and slows to a stand smoothly instead of bouncing, then holds the finger
  up still (it no longer jabs it forward and back).

### Removed
- **Giant Hands setting:** Hands (replaced by Fewest hands and Most hands).

## [0.0.9-alpha] - 2026-09-25

### Changed
- **Giant Hands:** the thumbs-up of the two hands from the portals now faces straight forward, thumbs straight up,
  instead of tilting up at the sky; the sparkles sit on the thumb tips.

## [0.0.8-alpha] - 2026-09-25

### Added
- **Report a bug:** new button in the update manager, next to What's new. Give the bug a name, a description and
  a priority (low, medium or high) and press Send; your Minecraft name and version go with it, and you get the
  report's number. Reports become public issues on the mod's GitHub page.
- **Bug reports for the makers:** a small online relay turns each report into a GitHub issue, and a script copies
  the open ones into the project every five minutes, highest priority first.

## [0.0.7-alpha] - 2026-09-25

### Changed
- **Flight:** top speed 35% lower: 6.3 blocks a second (was 9.6). Start 4.2, cruising 5.2.
- **Flight:** top speed after 7.5 seconds of flying on (was about 6).
- **Flight:** you pick up speed 75% faster.
- **Bolt and beam:** work as long as one hand is free, also while the ring hand charges the Giant Fist, waves up
  a giant hand or calls the air strike. Blocked only when both hands are busy: shield or dome up as well,
  recharging, the take-off.
- **Giant Fist, Giant Hands, Air Strike, Light Bubble:** no longer stop a beam you are pouring out.

### Fixed
- **Flight:** a hit no longer drops you out of the sky: you keep flying.

### Removed
- **Code:** nearly all comments; a few short ones stay where they are needed.

## [0.0.6-alpha] - 2026-09-25

### Changed
- **Update manager:** **What's new** is always there now, also when you are up to date: it shows the notes of the
  version you have, marked INSTALLED. With an update, the new versions come first (the newest marked NEW) and the
  version you have is at the bottom.

## [0.0.5-alpha] - 2026-09-25

### Added
- **Updates in the game:** the mod looks for a new release when the game starts and every five minutes. A new one
  comes with a pling and a small popup at the top right; click it in the menu, or press `U` in the game (changeable
  under Controls) to open the update manager at any time. It shows the installed and the newest version, **What's new**
  (every new version's notes), **Update later** (installed when you close the game) and **Update & restart** (saves,
  closes and starts the game again on the new version; with Prism Launcher and MultiMC it closes and you start it
  again yourself). Every download is checked against the release page's checksum. This version is the first with the
  updater: from here on, updates can be installed from inside the game.

## [0.0.4-alpha] - 2026-09-25

### Added
- **Music:** the multiverse theme plays on a loop in the main menu, in place of Minecraft's menu music. In a world
  the normal music plays. The music slider sets its volume, and the theme comes back when the slider goes up again
  after being at 0.

## [0.0.3-alpha] - 2026-09-25

### Changed
- **Repository:** audio and video files are part of the repository now, the reference clips in `docs/reference/`
  too. The game itself is the same as in 0.0.2-alpha.

## [0.0.2-alpha] - 2026-09-25

### Changed
- **Giant Hands, the axe pair:** the two hands no longer pass through each other. Their fingers stay clear while they
  wave, reach over each other and grab the haft, and letting go they drop off the haft instead of through it.
- **Sword & Shield, taking them out:** the shield stays facing forward, braced before you, and the sword swings out to
  your right and in flat across the face of the shield twice (the second bang lighter), then back into the guard in
  one sweep. Every turn in between is smoother, and seen from outside the blade lands flat on the face too. The shield
  cannot block during the bangs, a moment longer than before.

## [0.0.1-alpha] - 2026-09-25

The first release with a version number: Green Lantern and Doctor Octopus, the classes, stamina and spells as
described in the docs.

### Changed
- **Giant Fist (R): crouching no longer cancels it.** Crouch while you hold R and the fist keeps charging; it
  flies when you let go.
- **Versions** now count up one step per release (0.0.1, 0.0.2 ... 0.1.0 ... 9.9.9); the jar is
  `multiverse-powers-<version>.jar` and the newest 10 are on the Releases page.
- **Code:** every source file over 600 lines was split into smaller files by what they do. The game plays and
  looks exactly the same.

## [Giant Hands, a new Air Strike and the sword thrown up] - 2026-09-24

### Added
- **Giant Hands (Left Alt)**, in place of the Lantern Flare: wave your ring hand and a giant hand of hard light
  rises out of the ground at one of the enemies within 20 blocks of you (one every time you press; set more and
  they come one after another, at most three at once, the next once the one before is halfway). Each smacks its
  enemy away, grabs it and throws it, slaps it flat against the ground (it lies squashed a moment and springs
  back), pounds it with the flat of its fist three times, or shoots up right in front of it with its middle finger
  raised, launching it and everything round it far away and high up. They stay where they came up, turn after the
  enemy nearest to them and sink back into the ground when they are done. Or two of them come as a **pair** out of
  portals of the ring's light (counting as one hand, and only ever alone): they snap their fingers and make the OK
  sign, pull a **giant axe** of hard light out of a third portal, chop it down on the enemy (18 hearts, flinging
  everything round it far away), leave it stuck in the ground and give you a thumbs up.
- **Air Strike jets:** two jets of hard light race round the gunship and fire small homing missiles at your
  enemies. When the gunship's engine bursts they break away, break the sound barrier with a thunderclap and a ring
  of light, and vanish in a star of light.

### Changed
- **Giant Hands** move far more smoothly: their fingers close and open one after another, their wrists follow
  through after every blow, and while they wait they sway a little and their fingers drift. The middle finger
  shoots up out of the ground, overshoots, settles and jabs at its enemy, and launches what is round it.
- **Air Strike missiles** now drop out of a bomb bay in the gunship's belly (its doors swing open and shut again),
  fall a way, fire their motor at a moment of their own and home in on the enemy nearest to them; they do three
  times the damage (12 hearts).
- **Air Strike miniguns** fire about 30% faster, their rounds are smaller, and the guns swing smoothly after what
  they fire at and stay on it.
- **Air Strike crash:** the gunship no longer drops out of the sky in a second: an engine bursts, it shudders and
  struggles, and only then does its nose drop slowly into a dive of about three seconds. The blast is a little
  bigger and harder (24 hearts, 16 blocks).
- **Taking out the sword and shield:** you now toss the sword up spinning, catch it, look it over while a gleam runs
  up the blade, and bang it twice on the shield.

### Removed
- Leftovers of old weapons and spells that were no longer in the game: the unused **Void Instability** effect (no
  longer in `/effect`), the empty **Cosmic Realm** dimension, and the old item pictures and texts.

### Fixed
- The rounds of the Air Strike's miniguns froze in the air, and the blasts of its missiles (and of a pounded Light
  Bubble) stopped halfway, leaving a missile stuck in the ground.
- After going through a portal or respawning, the constructs of where you were (an Air Strike's gunship and its
  drone) no longer stay behind on your screen for a few seconds.

## [World and client settings, flying over new land, and ready for servers] - 2026-09-24

### Added
- **World settings and your own settings:** every number of the stamina bar and of every character is now a world
  setting. Every world keeps its own (in its `serverconfig/welcomescreen` folder); the files in
  `config/welcomescreen` are what a new world starts with. On a server everyone plays by the server's world
  settings: your game gets them as you join, and again at once when they change on the server. Your own settings
  (`config/welcomescreen/client.toml`) hold what only you see and feel: a new **camera shake** (how hard the
  powers shake and jolt your view, 0 = never) and the ram cone's **scraping shake**, which moved there. In the
  settings screen the world tabs can only be changed in a world of your own; the new *Your settings* tab always.
- **Flying over new land:** while you fly, the world is made ready round you (128 blocks) and far ahead along the
  way you fly (8 seconds of flying), so you no longer stop dead in the air at the edge of land that is not there
  yet. Should the world still fall behind, you slow down smoothly before its edge and fly on once it is there.
  Both distances are world settings of the flight.

### Changed
- **Light Bolt:** shooting, your ring arm comes straight up and points where you aim, so every bolt leaves the ring
  on your outstretched hand (your own hand too, in first person); it kicks back a little with each bolt, stays up
  while you keep shooting and goes down again a moment after the last one. Everyone else sees it too.
- **Lighter on servers:** the particles of every power and ceremony reach each player in one packet per tick
  instead of one per particle; the ring's power is told once a tick (to others every few ticks); Doctor Octopus's
  arms and portals are only sent again when they move; a construct that is gone is only told to the players near
  it; and what the powers send goes out together at the end of every tick.
- **No more freezes from far away:** a power still at work where nobody is (an Air Strike you flew away from, a
  portal's target that ran far off, a long beam, lightning following its target) no longer makes the server load
  the world there.

### Fixed
- **Multiplayer:**
  - Logging out or dying no longer makes a running cooldown ready (the ultimate above all); spell cooldowns show
    right again after you log back in.
  - Spectators can no longer use powers, and becoming one takes your character off.
  - The Giant Fist and the Air Strike's crater no longer break blocks you could not break yourself (adventure
    mode, spawn protection, land claimed with other mods).
  - A player caught in a Light Bubble or held by Doctor Octopus is no longer kicked for flying, keeps looking
    round freely, and can no longer be held by two powers at once; a flying Green Lantern who is caught stops
    flying. Doctor Octopus lets go of a player who became a spectator or can no longer be hurt, and a player he
    drops when he leaves or dies lands safely.
  - With PvP off, Lightning Strike, the poison cloud, Wind Gust and the Fireball's fire no longer hurt other
    players; the Fireball's fire no longer burns in spawn protection.
  - Void Walk: a server that stops while you walk in the void no longer saves you silent and invisible for good;
    milk or a flare that takes the invisibility away ends the walk; a potion's invisibility you had before comes
    back afterwards.
  - Ground Strike marks no longer leave creatures and players glowing for good, and a spike only throws what it
    really hit. Picking Doctor Octopus again right after putting him away no longer breaks him.
  - Dying while picking a class brings the welcome screen back instead of leaving you a spectator without a
    class.
  - Names over Green Lanterns who fly or spin with the sword stay upright for everyone else.
  - After respawning or going to another dimension: no phantom landing slam any more, and your sword and shield
    are put away on your screen too, as they already were on the server.
  - Taking out the sword and shield no longer plays its sound twice for you.
  - Someone who leaves your sight no longer comes back still in his flying pose.
  - The Light Beam only starts once the ring really gathered its light, and a held fist of someone too far away is
    no longer drawn in the wrong place.

## [Snappier flight, a quicker arrival and no blocking mid-swing] - 2026-09-24

### Added
- **Tap jump twice in the air to stop flying:** once you are up, a quick double tap on jump turns your flight off
  and you fall from there, just like pressing C in the air.

### Changed
- **Flight: half the top speed, and there sooner:** you set off at 6.4 blocks a second, are at a cruising 8 within
  half a second and reach the top speed of 9.6 after about 6 seconds of flying on (it was 19.25, after more than
  half a minute). Picking up speed goes 40% faster. The dive for a slam still goes 19.25 blocks a second, and a
  landing slam by itself still needs about 8.7 blocks a second.
- **The ring's arrival takes about half as long** (8 seconds instead of 15), with the same steps, only snappier: a
  ring of light bursts out where it stops in the sky, it stops dead before your eyes with a flash, the scan sweeps
  down and up trailing light, the lantern flashes as it is done, and as the ring slides on a pillar of light shoots
  up out of you into the sky and your view jolts harder.
- **Sword and shield:** you can no longer block while you attack. A cut, a thrust or the flurry lowers the shield
  for as long as the move lasts; it comes back up by itself if you still hold right. The flurry keeps its own shield
  before your chest.

## [Sword and shield remade, quicker flight and a harder Light Bubble] - 2026-09-24

### Added
- **Pictures on the Construct Wheel:** every slot shows its construct. The sword and shield are their own small
  hard-light model, solid and glowing, the shield turning gently to and fro with the sword crossed behind it. The bar
  above your hotbar shows the picture of what you hold.
- **All sixteen weapons on the Construct Wheel:** the fifteen placeholders became Energy Whip, Brawler Gauntlets /
  Boxing Gloves, Dual Energy Daggers, Battleaxe, Two-handed War Hammer, Spear / Halberd, Heavy Chainsaw, Dual
  Revolvers / Hand Cannons, Sawed-off Shotgun, Dual Micro-SMGs, Arm Cannon / Mega Blaster, Rotary Grenade Launcher,
  Minigun, Rocket Launcher / RPG and Plasma Flamethrower. Each has its own detailed hard-light model as its picture;
  they show only their name (no line under it) and do nothing yet. A long name breaks after its slash.

### Changed
- **Sword and shield, remade:** you hold them the way the game holds a sword and shield: in first person the sword
  stands upright in your right fist at the bottom right, the shield hangs on your left forearm at the bottom left.
  Taking them out: the sword grows hilt first, you look it over, twirl it round once and knock it twice on the
  shield. Every cut winds up, whips through and brakes, the edge always leading; your body turns, bends and steps
  into it, and a blow that lands jolts your view. In first person every cut stays in view: where the blade strikes
  it lies across the screen, the thrusts go in from low at the right to where you aim, and your arms are slim and
  always run out of view.
- **The shield:** holding right now **blocks** (85% of what comes from the front, as long as you hold it);
  clicking right **charges**, ramming what is in your way aside with one of six rams (1.5 hearts each), and ends in
  the shield slam on a wall, on a second click or after 1.4 seconds. The shield bashes became those rams.
- **Flight picks up speed quickly:** from 6.4 to a cruising 13 blocks a second within 3 seconds, then slowly on to
  the top speed of 19.25 over 30 seconds more.
- **Light Bubble pounds harder:** pressing K again pounds the bubble into the ground three times (straight down,
  then on your left, then on your right), each slam harder (2, 2 and 6 hearts). The bubble stretches and squashes,
  the ground bursts up, a shockwave with cracks of light runs out and the view shakes; the last shockwave reaches
  4.5 blocks.
- **Air Strike:** the gunship flies lower (55 blocks up, it needs 21 blocks of room) and never stops firing: with
  nothing marked it rakes the ground along its way. Big six-barrelled guns with thick glowing tracers, missiles
  streaking down on a trail of light that each blow a small crater, a quicker crash (about a second) and a crater
  that keeps smoking and burning a while.

### Removed
- **The jets at top speed:** no more chains and small jets of hard light behind you while you fly.
- **The Dutch translation:** the mod is English only.

## [Gunship air strike, Light Bubble and the sword and shield] - 2026-09-24

### Added
- **Light Bubble (K), Green Lantern's eleventh power:** a ball-shaped cage of hard light grows round the creature
  you look at (up to 24 blocks away) and lifts it 3 blocks up; for 6 seconds it can do nothing. Press again to
  smash the bubble into the ground (6 hearts and a small shockwave round it); crouch and press to let it go.
  Too big creatures and bosses do not fit.
- **Sword and shield (Construct Wheel, slot 1):** the sword spins up out of the ring and you catch it, the shield
  grows onto your left arm. Left click: 12 different sword moves (slash, backhand, cleave, uppercut, overhead,
  stab, lunge, low sweep, spin and more) that flow into each other in a random order. Right click: 6 shield bashes
  that shove creatures away. Hold left click 2 seconds: shield up in front of your chest and 12 lightning-fast
  stabs around you, while the shield takes most of the damage. Hold right click 2 seconds: you bend forward behind
  the shield and sprint straight ahead, shoving everything in your path aside; on a wall or when you let go you
  slam the shield into the ground for a small shockwave.
- **Every power has its own animation,** the Ring Scan too: your ring hand comes up and sweeps the area.

### Changed
- **Air Strike is a gunship now:** a big, slow hard-light plane with four propellers flies twice as high as the
  jet did, in one straight line over the area for 20 seconds. Two miniguns on its sides fire two bullets every
  0.3 seconds (many bullets, not very precise), and two missile launchers fire a homing missile every 2 seconds
  (about nine in ten hit, each with a small blast). It scans the ground for enemies only, over half again the Ring
  Scan's area. Then an engine fails and it suddenly noses down into the ground: a real explosion that blasts a crater and
  throws blocks about, and a giant see-through mushroom cloud of green energy.
- **The ring glows** whenever it does something, and blazes during the ultimate.

## [Air Strike, jets at top speed and a ring that comes from the sky] - 2026-09-23

### Added
- **Air Strike (Y), Green Lantern's new ultimate:** you throw your ring fist up at the sky and a giant fighter jet
  of hard light takes shape high over the area you look at. For 10 seconds it circles there, firing homing
  missiles (about two in three find their creature) and bursts from its guns; then it slowly loses height and
  crashes into the middle in a massive blast of green energy that shakes the view of everyone close by. It only
  goes for what is out to hurt you, never your pets.
- **Jets at top speed:** once you fly at top speed, two chains of hard light run from your waist back to your
  left and right, each ending in a small jet of hard light with a glowing booster; they break apart when you slow
  down.
- **Take off by tapping jump twice,** on the ground or in the middle of a jump or a fall.
- **The ring scans you** when it comes for you, speaks to you ("you have the ability to overcome great fear"),
  and welcomes you to the Corps once the uniform is on.

### Changed
- **The ring's arrival is longer and closer to the comics:** it streaks down out of the sky like a comet, circles
  you once, scans you, and flares up round you as it slides on; your eyes light up and a surge runs over the suit
  once the mask is on. About 15 seconds in all.
- **Changing back is longer too:** your eyes go dark, the uniform draws back into the ring with specks of light
  streaming off you, and the ring hangs over your head a moment before it spirals off into the sky and is gone
  in a twinkle.
- **The beam arm:** your arm trembles as the ring fills, your other hand comes over to brace your wrist, and the
  beam breaking loose kicks your arm up and back before it settles, in first person and seen from outside.
- **Lantern Flare** is much stronger and looks better: 14 blocks, blinded and slowed for 12.5 seconds (hard at
  first, then milder), weakened, dazed creatures stumble about unable to find anyone, the invisible show again,
  the creatures of the dark are thrown back. It costs 8 power and waits 30 seconds between flares.
- **Ring Scan:** reaches 56 blocks, marks for 21 seconds, and the marked glow clearly through walls: red when out
  to hurt you, green otherwise.
- **Flight:** a slower top speed (19.25 blocks a second, reached after 30 seconds of flying on); the ram cone does
  more damage for its speed, so a full-speed ram still does about 7 hearts.
- **The Construct Wheel** shows how the buttons work at the top of the screen instead of the bottom.
- **Landing-slam constructs are only for the shockwave:** the lantern of the Lantern Flare is its own now.

### Removed
- **Construct Storm**, the old ultimate.

## [Construct Storm, Ring Scan, Lantern Flare and the ring coming to you] - 2026-09-23

### Added
- **Construct Storm (Y), Green Lantern's ultimate:** you throw your ring fist up at the sky, a pillar of light
  opens into a great ring of light over your head, and for 8 seconds it rains constructs down on the creatures
  around you (a fist, a hammer, an anvil, a safe, a piano and more), each with a shockwave of its own. The panel's
  title turns red and counts down. Y is now the ultimate key for every character.
- **Ring Scan (N):** a wave of the ring's light rolls out through walls; every creature it passes is framed for
  you, with its name and health, for 12 seconds, and the ring tells you what it found.
- **Lantern Flare (Left Alt):** the ring shapes your lantern over your raised fist; it fills with light and
  bursts like a small sun, blinding and slowing everything that sees it and burning the creatures of the dark.
- **The ring comes to you:** becoming Green Lantern, the ring flies to you from far off, shapes your lantern for
  you to catch and slides onto your finger; a shockwave makes the creatures of the dark flee, the uniform grows
  over you out of the ring (the mask last), and you recharge the ring. Changing back plays it the other way round.
- **The beam charges up:** while you hold left click on its way to the beam, the energy runs from your chest
  down your arm into the ring and over your whole suit, light streams into your fist and a whine rises.

### Changed
- **The beam is far more dramatic:** it shoots out with a flash, roars with surges, spinning strands, rings and
  crackling sparks, splashes where it strikes, shakes your view a little and pushes what it hits back.
- **Flying starts three times slower and speeds up the longer you fly,** up to the same top speed as before
  after 12 seconds; letting go of forward loses the speed again. Both are settings.
- **The landing-slam constructs** take shape in the air before you, hang, wind up and strike, and your view
  follows them; many of them are bigger, clearer and better animated, and they are lit like real objects.
- **The landing on one knee** really kneels now, and the fist goes into the ground in front of you.
- **The Construct Wheel is bigger and has 16 slots.**

### Fixed
- The ram cone no longer flickers or jumps while you fly.

## [A steerable Giant Fist, living constructs and clearer settings] - 2026-09-23

### Added
- **`/constructshockwave`:** a screen with all 32 landing-slam constructs, in groups, each explaining itself.
  Click one and a second later it strikes in front of you, shockwave and all. For anyone with cheats on (or an
  operator), whatever character they are; free, no cooldown.
- **Scraping with the ram cone:** flying with it low along the ground (under 1.5 blocks) or sliding over it costs
  2 more power a second, throws sparks, grinds and shakes your view. Three new settings under Light Shield.
- **Two new settings under Shockwave:** how big the constructs are (1.35) and how slowly they play (1.5).

### Changed
- **The Giant Fist is steered by your eyes.** Let go and it glides from beside you onto the middle of your view
  without turning, then stays exactly under your crosshair for its whole flight and follows where you look.
  It no longer turns or slides off sideways as it sets off.
- **All 32 landing-slam constructs are rebuilt** with far more detail: round things are truly round (bells,
  drums, coins, chains, plates), angular things have many more parts. Every one of them moves: the safe's
  door flies open and money spills out, the piano's lids spring up, the TNT's fuse burns down, the anchor's
  chain piles up, the drumsticks play a roll, and so on for all 32.
- **The constructs are 35% bigger and play half again as slowly**, and stand a good second after they strike,
  so you can see what happens. The shockwave strikes a little later with them; its reach is the same.
- **The shield is a real shield:** bulging, with a rim, rivets, the emblem and a grip, a ripple where hits land
  and a glint now and then. The dome's seams run like a stone wall, the ram cone has drill ridges and turns,
  the bolt is a round bullet, and your fist wears your ring.
- **The settings screen is rebuilt:** tabs for the stamina bar and every character, abilities that fold open
  and shut, a search box that looks through everything, a bar that explains the number you point at, shift
  for ten steps, and one Apply or Save for the changes on every tab.

### Fixed
- The name of what holding a mouse button leads to (beam, dome, brake) showed as a raw text key.

## [Multiverse Powers, and a power screen by franchise] - 2026-09-23

### Added
- **Every character of the multiverse list is in the power screen**, sorted by who owns them: Marvel, DC,
  Disney, Warner Bros. and Other (The Terminator and Spawn belong to none of the four). Doctor Octopus and
  Green Lantern can be picked; the other 25 are dimmed, say "Coming soon", and the mouse passes over them.

### Changed
- **The mod is called Multiverse Powers**, and so is its jar: `multiverse-powers-1.0.0.jar`. Its id inside
  the game stays `welcomescreen`, so worlds, settings and keys carry over.
- **The power screen (hold G) is rebuilt.** The first page shows the franchises and the schools of magic as
  the same rounded cards; resting on one opens it after 0.38 seconds instead of 0.3 (25% longer). A card only
  starts to wait once you have moved the mouse, also right after the screen opens or a page changes, so
  nothing opens or goes back under a mouse you left alone. Long names are cut short with "…" instead of
  running out of their card, the background is darker, and the panel with your abilities is hidden while the
  screen is open.

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
