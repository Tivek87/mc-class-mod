# Construct Wheel: Current Weapons & New Weapon Concepts

This document outlines the current weapons in the Construct Wheel (Green Lantern) and detailed concepts for all new weapons, each featuring 4 abilities/attacks.

All mechanics focus on animation, handling, kinetic feel, visual spectacle, and combat flow without hardcoded damage or range numbers.

---

## Part 1: Current Weapons in the Construct Wheel

All weapons in the wheel follow the universal control scheme:
- **Left Click Tap:** Quick primary strike / basic shot / combo attack
- **Left Click Hold:** Heavy sustained attack / charged attack
- **Right Click Tap:** Tactical move / utility / secondary strike
- **Right Click Hold:** Defensive stance / shield / sustained area move

---

### 1. Sword & Shield (SWORD_SHIELD)
- **Type:** Two-handed (blade in right hand, shield in left hand).
- **Status:** Fully implemented.
- **Four Abilities:**
  1. **Left Click Tap (Sword Combo):** Fluid melee combo system cycling through distinct attacks (Slash, Cleave, Uppercut, Overhead, Lunge, Spin).
  2. **Left Click Hold (Flurry):** Locks the shield against the chest for frontal protection while unleashing a furious flurry of rapid thrusts directly ahead.
  3. **Right Click Tap (Shield Charge):** Sprints forward with the shield braced, knocking aside and bowling over all enemies in your path. Finishes with a heavy ground slam shockwave upon impact.
  4. **Right Click Hold (Shield Block):** Raises the hard-light shield directly in front of your vision, intercepting incoming frontal attacks for as long as held.

---

### 2. Energy Whip (ENERGY_WHIP)
- **Type:** One-handed (coiled energy whip held in the right fist).
- **Status:** Functional in code, temporarily locked for visual/performance rework.
- **Four Abilities:**
  1. **Left Click Tap (Whip Lash):** Varied whip strikes (Forehand, Backhand, Overhead, Ankle Strike, Cowboy Crack) that sweep targets along the direction of the swing and crack with a sonic flash at the tip.
  2. **Left Click Hold (Whirlwind):** Spins the whip overhead in wide, roaring loops that continuously lash and stagger surrounding foes, concluding with a heavy sonic crack upon release.
  3. **Right Click Tap (Lasso):** Casts the whip forward to snare an enemy in glowing coils, then violently yanks them back to slam hard onto the ground at your feet, leaving them briefly slowed.
  4. **Right Click Hold (Spinning Shield):** Whirls the whip before you like a high-speed propeller, deflecting incoming projectiles and dampening incoming frontal blows.

---

### 3. Plasma Flamethrower (FLAMETHROWER)
- **Type:** Heavy two-handed (rear pistol grip, front foregrip beneath the cylindrical plasma fuel tank).
- **Status:** Fully implemented with a heat gauge and overheat mechanics.
- **Four Abilities:**
  1. **Left Click Tap (Flame Sweep):** Wide horizontal sweep of green plasma that pushes back oncoming targets.
  2. **Left Click Hold (Inferno):** Roaring continuous plasma jet along your crosshair. Heat accumulates on the HUD gauge; prolonged firing causes the weapon to overheat and vent steam before reigniting.
  3. **Right Click Tap (Fire Wall):** Drags the muzzle across the terrain to erect a blazing plasma wall that stops advancing foes and burns away incoming projectiles.
  4. **Right Click Hold (Fire Vortex):** Directs the nozzle toward the ground to whip up a surrounding fire tornado, protecting the user and exploding outward in a flaming shockwave when released.

---

## Part 2: Concepts & Ideas for the New Weapons

---

### 1. Rocket Launcher / RPG (ROCKET_LAUNCHER)
- **Class:** **Heavy two-handed (Heavy 2-handed)**.
- **Visuals & Handling:** A massive, shoulder-mounted hard-light rocket launcher. The right hand rests on the firing trigger atop the shoulder while the left hand stabilizes the foregrip and digital targeting optic. Features an open rear exhaust port that blasts concussive energy rings backward upon each launch. Player movement feels deliberate and heavy.

#### The 4 Abilities:
1. **Left Click Tap: Heavy Rocket (Primary Fire)**
   - **Mechanics:** Launches a heavy hard-light rocket down your crosshair. The rocket accelerates forward and detonates on impact with a massive explosion, blasting enemies into ragdoll trajectories with intense knockback.

2. **Left Click Hold: Cluster Airburst Mortar (Ascending Cluster Rocket)**
   - **Mechanics:** Loads a specialized warhead and fires it straight up into the air. At the apex of its trajectory, the missile detonates and splits into 8 homing cluster rockets. The clusters disperse and automatically seek out either the highest-health target in the area or the closest enemies threatening the player.

3. **Right Click Tap: Rocket Blast Jump (Ground Repulsor)**
   - **Mechanics:** The player leaps and simultaneously discharges a rocket directly into the ground beneath their feet. The blast creates an immediate concussive area-of-effect that damages and repels ground enemies, propelling the player high into the air with complete immunity to fall damage upon landing.

4. **Right Click Hold: Bunker Lock & Blast Trench (Heavy Artillery Lockdown)**
   - **Mechanics:** The player plants stabilizer struts into the ground and braces the launcher forward. While held, an angular hard-light blast trench forms in front of the player, completely absorbing incoming frontal blows and projectiles while automatically charging the weapon's next shot for extra concussive radius.

---

### 2. Arm Cannon (ARM_CANNON)
- **Class:** **Heavy one-handed (Heavy 1-handed)**.
- **Visuals & Handling:** Encased directly over the player's entire right forearm and fist as a heavy cybernetic cannon of solid hard light. Features glowing heat sinks, pulsing energy conduits, and expanding barrel plates. The left hand remains completely free for agile maneuvers, sprinting, and balance.

#### The 4 Abilities:
1. **Left Click Tap: Shockwave Kinetic Orb (Medium Heavy Blast)**
   - **Mechanics:** Fires a dense, mid-sized orb of hard light. As it travels, concentric shockwaves trail along its path, pushing back targets and delivering substantial concussive impact.

2. **Left Click Hold: Mega Singularity Cannon (Charged Piercing Core)**
   - **Mechanics:** Charges the cannon with an intense build-up. While charging, the muzzle pulses repeating circular shockwaves forward along your aim, steadily shoving enemies back without ragdolling them. Once fully charged, fires a massive, slow-moving energy ball that glides through the air, dealing heavy damage to anything touching it. It pierces through all enemies along its path until striking solid terrain, detonating in an enormous explosion with a massive shockwave radius that ragdolls and sweeps all enemies away without excessive knockback.

3. **Right Click Tap: Contagion Core (Infection Detonator)**
   - **Mechanics:** Launches a volatile energy dart that sticks to the targeted entity, coating them in a sickly green glow with an active countdown timer. When the timer expires, the core violently detonates. Any other entities touched by the infected host become infected as well, triggering secondary localized explosions after a short delay in a chaotic chain reaction.

4. **Right Click Hold: Seismic Ground Slam (Expanding Shockwave Ring)**
   - **Mechanics:** The player slams the cannon directly into the earth. The cannon channels energy into the terrain, producing the same circular shockwaves as the charge attack, but expanding outward all around the player. The longer the button is held, the wider the shockwave ring expands, constantly staggering and pushing away all surrounding threats.

---

### 3. Minigun (MINIGUN)
- **Class:** **Heavy two-handed (Heavy 2-handed)**.
- **Visuals & Handling:** An imposing rotary gatling cannon carried at the hip with a sturdy overhead carry-handle and rear control grip. Barrel rotation accelerates with an audible spin-up whine before firing. Forward movement is slowed while the barrels are actively spinning.

#### The 4 Abilities:
1. **Left Click Tap: Spin-Up Burst (Controlled Volley)**
   - **Mechanics:** Rapidly cycles the barrel cluster to unleash a tight, focused burst of high-velocity hard-light rounds, ideal for snapping onto quick targets without a full commitment.

2. **Left Click Hold: Overdrive Barrage (Spin-Up & Sustained Lead Storm)**
   - **Mechanics:** The barrels spin progressively faster, taking a brief build-up period to reach maximum RPM. Once fully spooled, it unloads an unrelenting hail of high-speed rounds for as long as held. Prolonged continuous firing causes bullet spread to widen and heat to rise toward an overheat limit. If held until overheating, the mechanism locks into an emergency vent cycle with a lengthy cooldown animation. If released before overheating, the barrels smoothly spin down and cool off in half the time.

3. **Right Click Tap: Concussive Shrapnel Ejection (Breach Blast)**
   - **Mechanics:** Discharges a heavy concussive slug directly from the core axis in a loud front blast, staggering enemies directly in front of you and clearing close-range space.

4. **Right Click Hold: Juggernaut Whirlwind (360° Rotary Sweep)**
   - **Mechanics:** The player charges the rotary barrels and begins spinning completely around in a continuous 360-degree circle like a top for as long as the button is held, showering the entire battlefield in an omnidirectional bullet storm.

---

### 4. Sawed-off Shotgun (SHOTGUN)
- **Class:** **Mid two-handed (Mid 2-handed)**.
- **Visuals & Handling:** A compact break-action double-barrel shotgun. Extremely agile, aggressive, and tuned for hit-and-run close-range skirmishing.

#### The 4 Abilities:
1. **Left Click Tap: Buckshot Spread (Single-Barrel Blast)**
   - **Mechanics:** Discharges a single barrel in a wide cone of hard-light pellets. Delivers devastating impact at point-blank range, falling off with distance, and knocking the target backward.

2. **Left Click Hold: Double-Barrel Obliteration (Dual Hammer Discharge)**
   - **Mechanics:** After brief point-blank focus, drops both hammers simultaneously with an ear-splitting boom. Massive forward damage that flattens targets onto their backs into ragdoll states, while the violent recoil drives the player backward.

3. **Right Click Tap: Stock Strike & Barrel Jab (Melee Shove)**
   - **Mechanics:** A lightning-fast melee bash with the wooden stock or barrel muzzle, interrupting the target's attack, staggering them, and setting them up for a clean follow-up shot.

4. **Right Click Hold: Deflection Stance & Concussive Blast Screen (Parry & Counter)**
   - **Mechanics:** Raises the shotgun defensively across the chest, bracing to absorb incoming melee strikes and deflect projectiles. Releasing the stance triggers an immediate deafening blast that knocks away all forward foes and leaves a lingering cloud of green sparks that blinds and disorients attackers.

---

### 5. Revolvers (REVOLVERS)
- **Class:** **Special: 1-handed (Single) or 2-handed (Akimbo / Dual-Wield)**.
- **Visuals & Handling:**
  - Can be wielded as a **single revolver** (surgical precision, maximum mobility, fanning from the hip or deliberate sights-aligned shots) or as **dual revolvers** (Akimbo: double fire volume, sweeping crossfire, oppressive suppression).
  - Integrates both **Light/Fast actions** (reflex hipfire, rapid fanning) and **Heavy/Slow actions** (aimed magnum shots, precision execution).

#### The 4 Abilities:
1. **Left Click Tap: Hipfire / Fan the Hammer (Fast / Light Fire)**
   - **1 Revolver (Single):** Snaps from the hip to fan the hammer, firing quick shots in rapid succession with wide bullet spread and high mobility.
   - **2 Revolvers (Dual):** Fires rapid hipfire volleys alternately and simultaneously with both revolvers, laying down a wide screen of scattering lead.

2. **Left Click Hold: Sights-Aligned Execution (Heavy / Slow Fire)**
   - **1 Revolver (Single):** Lifts the revolver up to eye level, aiming down the sights with focused tunnel-vision. After holding to steady your aim, releases a thunderous, armor-piercing shot that punches clean through multiple lined-up targets.
   - **2 Revolvers (Dual):** Crosses both arms across the chest in an X formation with both revolvers braced. Releases heavy precision rounds from each revolver that hone in on the closest enemy with the highest remaining health, staggering them with severe impact.

3. **Right Click Tap: Akimbo Stance Toggle & Trick Spin (Stance Swap & Bash)**
   - **Mechanics:** Instantly swaps between **1 Revolver** (precision & mobility) and **2 Revolvers** (maximum rate of fire). If an enemy is in melee range when swapping, the transition includes a swift gun spin that strikes the foe with the grip, briefly staggering them.

4. **Right Click Hold: Gunslinger Duelist Flow (Projectile Parry & Ricochet)**
   - **1 Revolver (Single):** Enters an agile gunfighter stance, reflexively shooting incoming hostile projectiles out of the air and ricocheting bullets into nearby enemies.
   - **2 Revolvers (Dual):** Rapidly twirls both revolvers in interlocking loops, generating a dynamic defensive perimeter that deflects attacks and scatters stray rounds into surrounding enemies.

---

### 6. Battleaxe (BATTLEAXE)
- **Class:** **Heavy two-handed (Heavy 2-handed)**.
- **Visuals & Handling:** A brutal, double-bitted war axe of solid hard light. Features a long, reinforced haft gripped with both hands and massive glowing crescent blades that leave lingering light trails on every swing. Swings feel heavy and momentum-driven, transferring the full weight of the character into every chop.

#### The 4 Abilities:
1. **Left Click Tap: Cleaving Chop & Sweep (Heavy Melee Cleave)**
   - **Mechanics:** Delivers heavy, sweeping horizontal and diagonal chops that slice smoothly through multiple enemies in front of you, cleaving through shields and staggering targets.

2. **Left Click Hold: Guillotine Earthbreaker (Leaping Overhead Slam)**
   - **Mechanics:** The player leaps forward into the air and brings the battleaxe down overhead with terrifying momentum, burying the head deep into the earth. The impact fractures the ground forward in a linear energy fissure, launching targets airborne and knocking them down.

3. **Left Click Tap / Right Click Tap: Hook & Drag (Disarming Axe Hook)**
   - **Mechanics:** Uses the curved beard of the axe head to hook around an enemy or their guard. With a violent pull, the player yanks the target off-balance toward them, stumbling them and setting them up for a direct overhead execution.

4. **Right Click Hold: Berserker Whirlwind (Continuous Bladestorm)**
   - **Mechanics:** Braces the axe at waist level and begins spinning continuously in wide, devastating circles. The player gains complete immunity to knockback and slices through all surrounding enemies for as long as held, finishing with a wide outward discharge when released.

---

### 7. Heavy Chainsaw (CHAINSAW)
- **Class:** **Heavy two-handed (Heavy 2-handed)**.
- **Visuals & Handling:** A monstrous, industrial chainsaw forged of emerald hard light. Features a roaring motor housing with vibrating intake vents, dual grip bars, and high-speed circulating saw teeth that throw off showers of sparks. The screen and hands shake with violent mechanical vibration while the motor is engaged.

#### The 4 Abilities:
1. **Left Click Tap: Rev & Slash (Quick Shredder Strike)**
   - **Mechanics:** Revs the throttle and delivers a fast, aggressive diagonal swipe. The spinning teeth dig into the target on contact, spewing green energy sparks and leaving a deep tearing wound that briefly causes the enemy to stumble.

2. **Left Click Hold: Continuous Rend & Saw (Sustained Meat Grinder)**
   - **Mechanics:** Revs the motor to maximum RPM and drives the humming blade straight forward into the enemy. As long as held, the chainsaw grinds relentlessly into the victim, locking both the player and the target in place while rapidly sawing through them with intense screen rumble and blood/spark spray.

3. **Right Click Tap: Impale Thrust & Eject (Piston Bayonet Lunge)**
   - **Mechanics:** The player thrusts the nose of the spinning chainsaw violently forward into the enemy's torso, letting the spinning tip chew for a split second before revving full throttle to violently eject and blast the enemy backward into a ragdoll tumble.

4. **Right Click Hold: Whirring Guard & Deflector Grind (Active Blade Parry)**
   - **Mechanics:** Raises the revving chainsaw diagonally across the upper body as an active defensive barrier. Hostile melee attacks striking the blade are instantly ground away and parried, staggering the attacker, while incoming physical projectiles are shredded into harmless sparks.

---

## Summary Overview

| Weapon | Type & Stance | Left Click Tap | Left Click Hold | Right Click Tap | Right Click Hold |
|---|---|---|---|---|---|
| **RPG** | Heavy 2-handed | Heavy Rocket (Direct Impact) | Cluster Airburst (8 homing rockets) | Rocket Blast Jump (No fall damage) | Bunker Lock (Blast trench shield) |
| **Arm Cannon** | Heavy 1-handed | Kinetic Orb (Shockwave trail) | Mega Singularity (Piercing orb & blast) | Contagion Core (Infection chain) | Seismic Slam (Expanding shockwave ring) |
| **Minigun** | Heavy 2-handed | Spin-Up Burst (Rapid volley) | Overdrive Barrage (Spin-up, spread & heat) | Concussive Shrapnel (Breach blast) | Juggernaut Whirlwind (360° spin barrage) |
| **Sawed-off Shotgun** | Mid 2-handed | Buckshot Spread (Point-blank blast) | Double-Barrel (Twin hammer blast) | Stock Strike (Melee interrupt) | Deflection Stance (Parry & blast screen) |
| **Revolvers** | 1-handed or 2-handed (Akimbo) | Hipfire / Fan (Fast, wide spread) | Aimed Execution / X-Cross Shot | Akimbo Toggle + Trick Spin | Gunslinger Flow (Parry & ricochet) |
| **Battleaxe** | Heavy 2-handed | Cleaving Chop (Wide sweep) | Guillotine Earthbreaker (Leap & fissure) | Hook & Drag (Disarm & yank) | Berserker Whirlwind (Spinning bladestorm) |
| **Heavy Chainsaw** | Heavy 2-handed | Rev & Slash (Quick shred) | Continuous Rend (Grinds into victim) | Impale Thrust (Tip chew & eject) | Whirring Guard (Shreds blows & arrows) |