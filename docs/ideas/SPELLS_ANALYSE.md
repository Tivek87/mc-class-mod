# Volledige Technische Analyse: Magic Schools, Spells & Custom VFX

Dit document beschrijft tot in detail hoe het magie-systeem, de **15 Magic Schools**, de **5 actieve Spells** (`Fireball`, `Lightning Strike`, `Poison Area`, `Wind Gust`, `Void Walk`) en de **dubbele custom VFX-engine** (server-side geometrische particles + client-side 60+ FPS procedurele 3D-meshes & GLSL post-processing shaders) zijn opgebouwd in de codebase.

---

## 1. Overzicht: Magic Schools & Spell Architectuur

### 1.1 De 15 Magic Schools (`MagicSchool.java`)
Bestand: `src/main/java/nl/tivek/multiversepowers/spell/MagicSchool.java`

Elke school is gedefinieerd als een enum-waarde in `MagicSchool` met een eigen naam en een vaste **24-bit RGB hex-kleur** die als primaire school-kleur en voor de gloed-accenten wordt gebruikt:

| Enum ID | Weergavenaam | RGB Hex Kleur | Actieve Spell in Code |
| :--- | :--- | :--- | :--- |
| `EARTH` | Earth | `0x8B5A2B` (Aardebruin) | *(Nog geen actieve spell)* |
| `AIR` | Air | `0xDDEEF2` (IJs-/Luchtwit) | **Wind Gust** (`WIND_GUST`) |
| `FIRE` | Fire | `0xFF5511` (Fel Vuuroranje) | **Fireball** (`FIREBALL`) |
| `WATER` | Water | `0x2E86DE` (Oceaanblauw) | *(Nog geen actieve spell)* |
| `HOLY` | Holy | `0xFFEAA7` (Warm Goudgeel) | *(Nog geen actieve spell)* |
| `DARK` | Dark | `0x6C5CE7` (Diep Donkerpaars) | **Void Walk** (`VOID_WALK`) |
| `ICE` | Ice | `0x74B9FF` (Helder IJsblauw) | *(Nog geen actieve spell)* |
| `LIGHTNING` | Lightning | `0x00D2FF` (Elektrisch Cyaan) | **Lightning Strike** (`LIGHTNING_STRIKE`) |
| `NATURE` | Nature | `0x2ED573` (Smaragdgroen) | **Poison Area** (`POISON_AREA`) |
| `BLOOD` | Blood | `0xD63031` (Bloedrood) | *(Nog geen actieve spell)* |
| `METAL` | Metal | `0xB0BEC5` (Staalgrijs) | *(Nog geen actieve spell)* |
| `GRAVITY` | Gravity | `0x5E35B1` (Zwaartekracht-indigo) | *(Nog geen actieve spell)* |
| `TIME` | Time | `0xF1C40F` (Tijd-goud) | *(Nog geen actieve spell)* |
| `ILLUSION` | Illusion | `0xD980FA` (Illusie-magenta) | *(Nog geen actieve spell)* |
| `COSMIC` | Cosmic | `0x4834D4` (Kosmisch diepblauw) | *(Nog geen actieve spell)* |

---

### 1.2 De Spell Registry (`Spell.java`)
Bestand: `src/main/java/nl/tivek/multiversepowers/spell/Spell.java`

Alle actieve spells zitten in de `Spell` enum. Elke spell koppelt een `MagicSchool`, een basis-cooldown in server ticks (`20 ticks = 1 seconde`), een eigen primaire kleur en een method-reference (`Consumer<ServerPlayer>`) naar de server-side `cast`-functie:

| Spell Enum | Naam | School | Basis Cooldown | Spell RGB Kleur | Server Entrypoint |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `WIND_GUST` | Wind Gust | `MagicSchool.AIR` | `100 ticks` (`5.0s`) | `0xDDEEF2` | `WindGustSpell::cast` |
| `FIREBALL` | Fireball | `MagicSchool.FIRE` | `40 ticks` (`2.0s`) | `0xFF8A2A` | `FireballSpell::cast` |
| `VOID_WALK` | Void Walk | `MagicSchool.DARK` | `600 ticks` (`30.0s`) | `0x9B5CFF` | `VoidWalkSpell::cast` |
| `LIGHTNING_STRIKE` | Lightning Strike | `MagicSchool.LIGHTNING` | `160 ticks` (`8.0s`) | `0x48DBFB` | `LightningSpell::cast` |
| `POISON_AREA` | Poison Area | `MagicSchool.NATURE` | `240 ticks` (`12.0s`) | `0x7FD46B` | `PoisonSpell::cast` |

* **Dynamische Cooldown Scaling:** `Spell.cooldown()` vermenigvuldigt `baseCooldown` met de gamerule/config multiplier `PowerRules.cooldowns()`. Als die op `<= 0` staat, is de cooldown `0 ticks`.
* **Lookup per School:** Bij het laden van de class bouwt `Spell` een statische `EnumMap<MagicSchool, List<Spell>> BY_SCHOOL` op, zodat via `Spell.ofSchool(school)` direct alle spells van een school kunnen worden opgehaald.

---

### 1.3 Casting Flow, Cooldowns & Target Filtering (`SpellCasting.java` & `SpellTargets.java`)
Bestanden:
* `src/main/java/nl/tivek/multiversepowers/spell/SpellCasting.java`
* `src/main/java/nl/tivek/multiversepowers/spell/SpellTargets.java`

1. **Server Validatie & Cooldowns (`SpellCasting.tryCast`):**
   * Wanneer de server een `CastSpellPayload(spell.getId())` ontvangt, controleert `SpellCasting.tryCast` of `player.isAlive()`, `!player.isSpectator()`, en of spells globaal aan staan via `PowerRules.spells()`.
   * Controleert in `CooldownTracker<Spell> COOLDOWNS` of `COOLDOWNS.left(player, spell, 0) > 0`.
   * Als de spell vrij is, roept de server `spell.cast(player)` aan, start de cooldown via `COOLDOWNS.start(player, spell, cooldown, 0)` en synchroniseert de cooldown via `SpellCooldownPayload(spell.ordinal(), cooldown)`.
   * Bij doodgaan (`LivingDeathEvent`) of uitloggen (`PlayerLoggedOutEvent`) worden alle actieve cooldowns en een eventuele actieve `Void Walk` direct gewist.
2. **Target Filtering (`SpellTargets.hits`):**
   * Geen enkele area-spell gebruikt ruwe vanilla entity-lijsten zonder filter. Elke spell filtert met `SpellTargets.hits(caster, entity)`:
     * Negeert de caster zelf (`entity == caster`), dode entities (`!entity.isAlive()`) en spectators (`entity.isSpectator()`).
     * Controleert `Targeting.isTargetable(entity, caster)` (houdt rekening met Creative mode, onkwetsbaarheid en PvP-regels).
     * Controleert `Factions.mayHit(caster, entity)` zodat teamgenoten en bevriende summons nooit door area-spells worden geraakt.
3. **Fysieke Knockback (`SpellTargets.push`):**
   * Berekent `resist = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)` en schaalt de duwkracht met `scale = max(0.0, 1.0 - resist)`.
   * Telkens wanneer een spell een entity wegduwt, wordt zowel `target.hasImpulse = true` als **`target.hurtMarked = true`** gezet. Dit dwingt Minecraft om de nieuwe snelheidsvector direct via een netwerkpakket naar een geraakte speler te sturen (zonder `hurtMarked = true` bewegen andere spelers niet op hun eigen scherm).

---

## 2. De Dubbele Custom VFX-Architectuur

Het visuele systeem van de spells is uniek omdat het **twee aparte lagen** tegelijk combineert:
1. **Laag 1 — Server-Side Tick-Driven Geometrische Particle Engine (`Effects`, `ParticleFx`, `ParticleBatch`)**
2. **Laag 2 — Client-Side 60+ FPS Procedurele 3D Mesh & Construct Renderer (`SpellFxPayload`, `SpellFx`, `ConstructPainter`, `Particles`)**

```
[Server: Spell.cast(player)]
       │
       ├──► 1. Effects.start(level, (lvl, age) -> ...)  [Elke server tick (20 Hz)]
       │         └──► ParticleFx + ParticleBatch (Wiskundige 3D vormen: spiralen, sterren, Fibonacci-bollen)
       │
       └──► 2. SpellFxPayload.send(...)                 [1 enkel compact netwerk-packet!]
                 └──► Client: SpellFx.spawn(...)
                        ├──► ClientTickEvent (20 Hz): Lokale ambient particles (Particles.java) + Entity tracking
                        └──► RenderLevelStageEvent (60-240+ FPS): ConstructPainter 3D-meshes, tapers, flares & shaders
```

### 2.1 Laag 1: Server-Side Geometrische Particle Engine (`ParticleFx.java` & `Effects.java`)
Bestanden:
* `src/main/java/nl/tivek/multiversepowers/engine/fx/ParticleFx.java`
* `src/main/java/nl/tivek/multiversepowers/engine/fx/ParticleBatch.java`
* `src/main/java/nl/tivek/multiversepowers/engine/tick/Effects.java`

In plaats van één voor één losse `ServerLevel.sendParticles`-calls te doen, start een spell een tick-taak via `Effects.start(level, (lvl, age) -> ...)`. Deze lambda krijgt elke server-tick zijn leeftijd `age` (`0, 1, 2, ...`) mee en stopt zodra hij `false` teruggeeft.

Binnen die tick-loop bouwt **`ParticleFx`** wiskundige 3D-vormen op die via **`ParticleBatch`** gebundeld worden verstuurd naar spelers binnen `VIEW_RANGE = 128.0` blokken:
* **Kleur- en Fade-Dust:**
  * `ParticleFx.dust(rgb, size)` maakt een `DustParticleOption` met exacte RGB-kleur en schaal (`0.01` t/m `4.0`).
  * `ParticleFx.fade(fromRgb, toRgb, size)` maakt een `DustColorTransitionOptions` die tijdens zijn levensduur vloeiend van de ene RGB-kleur naar de andere verkleurt.
* **Orthonormale 3D Basisvectoren (`ParticleFx.basis(Vec3 normal)`):**
  * Berekent bij elke willekeurige richtingvector `n` (zoals de vliegrichting van een Fireball) twee loodrechte eenheidsvectoren `u` en `v` via kruisproducten (`n.cross(ref)`). Hiermee kunnen cirkels, sterren en spiralen onder elke hoek in 3D-ruimte worden getekend.
* **Wiskundige Vormen in `ParticleFx`:**
  * `disc(level, center, normal, radius, points, turn, particle)`: Tekent een georiënteerde 3D-cirkel op basis van `u * cos(angle) + v * sin(angle)`.
  * `discStar(level, center, normal, radius, tips, perEdge, skip, turn, particle)` & `groundStar(...)`: Tekent een echt **ster-polygoon** (zoals een 5-puntig pentagram bij `tips = 5, skip = 2`) door lijnstukken te interpoleren tussen hoekpunt `i` en hoekpunt `i + skip`.
  * `sphere(level, center, radius, count, ...)` & `sphereOut(...)`: Verdeelt `count` particles exact gelijkmatig over een 3D-bol met het **Fibonacci-bol algoritme** en de gulden hoek (`GOLDEN_ANGLE = 2.399963229728653 rad`):
    * `y = 1.0 - (2.0 * i + 1.0) / count`
    * `ring = sqrt(1.0 - y * y)`
    * `theta = GOLDEN_ANGLE * i`
    Bij `sphereOut` krijgt elk punt een snelheidsvector exact naar buiten (`dx * speed, y * speed, dz * speed`), wat een perfecte bolvormige drukgolf oplevert.
  * `zigzag(level, from, to, steps, jitter, particle)`: Interpoleert lineair tussen punt A en B en telt op elk tussenpunt willekeurige 3D-jitter op (behalve op het begin- en eindpunt) voor bliksem- en energie-bogen.
  * `helix(level, origin, height, radius, turns, steps, particle)`: Tekent een opstijgende 3D-spiraal.
  * `shockwave(...)`: Schiet `points` particles in een plat vlak radiaal naar buiten met snelheid `speed`.

---

### 2.2 Laag 2: Client-Side Procedurele 3D Renderer (`SpellFxPayload`, `SpellFx`, `ConstructPainter`)
Bestanden:
* `src/main/java/nl/tivek/multiversepowers/spell/SpellFxPayload.java`
* `src/main/java/nl/tivek/multiversepowers/spell/client/SpellFx.java`
* `src/main/java/nl/tivek/multiversepowers/spell/client/Particles.java`
* `src/main/java/nl/tivek/multiversepowers/engine/client/render/ConstructPainter.java`

Minecraft particles alleen zijn niet genoeg voor vloeiende lichtstralen, 3D-flacons, bliksemschichten met vertakkingen of zwarte gaten. Daarom gebruikt de mod een eigen **client-side 3D vertex-renderer**:

1. **Netwerk-synchronisatie met Deterministische Seed (`SpellFxPayload.java`):**
   * Wanneer een spell afgaat, stuurt de server **één klein pakketje** (`SpellFxPayload`) naar alle spelers binnen `128` blokken met:
     * `Kind` (`FIREBALL_FLY`, `FIREBALL_BURST`, `LIGHTNING_CHARGE`, `LIGHTNING_BOLT`, `LIGHTNING_ARC`, `POISON_VIAL`, `POISON_CLOUD`, `WIND_GUST`, `VOID_ENTER`, `VOID_LEAVE`, `VOID_AMBUSH`)
     * `Vec3 from` & `Vec3 to` (begin-/eindpositie of richtingsvector)
     * `int entity` (entity ID om te volgen, of `-1`)
     * `int seed` (willekeurige seed van de server)
     * `int ticks` (levensduur)
   * **Waarom de `seed` zo belangrijk is:** Alle clients gebruiken `Noise.of(seed, ...)` en `Noise.direction(seed, ...)` met exact dezelfde `seed`. Daardoor kronkelt een bliksemschicht of vuurtong op het scherm van **elke speler op exact dezelfde plek**, zonder dat de server honderden coördinaten hoeft te sturen!
2. **Levenscyclus & Sub-Tick Interpolatie (`SpellFx.java`):**
   * Houdt een lijst bij van maximaal `MOST = 64` actieve `Fx`-objecten.
   * Tijdens `ClientTickEvent.Post` (`20 Hz`) volgt `SpellFx.follow` bewegende projectielen (zoals de vliegende Fireball) en slaat de laatste **14 posities** op in een `ArrayDeque<Vec3> trail`. Tegelijkertijd spawnt `Particles.tick` puur client-side ondersteunende Minecraft-particles (geschaald op de grafische instelling van de speler: *All = 100%*, *Decreased = 50%*, *Minimal = 15%*).
   * Tijdens `RenderLevelStageEvent` op `Stage.AFTER_TRANSLUCENT_BLOCKS` (op de volledige framerate van de speler, bijv. `144 FPS`) berekent `SpellFx.onRenderLevel` de exacte kommagetal-leeftijd:
     $$\text{age} = (\text{gameTime} - \text{fx.born}) + \text{partialTick}$$
   * Voert eerst een **Frustum Culling** check uit (`painter.visible(at, cullRadius)`) zodat effecten buiten beeld geen GPU-tijd kosten.
3. **De 3D Primitives van `ConstructPainter.java`:**
   * `ConstructPainter` erft van `PainterSolid` en `PainterLight` en schrijft rechtstreeks gekleurde en additief-oplichtende quads/driehoeken naar de OpenGL/Vulkan vertex-buffers:
     * **`lightTaper(a, b, rA, rB, color, alpha)` & `glowTaper(...)`:** Tekent een 3D taps toelopende lichtstraal tussen punt `a` en `b` met begindikte `rA` en einddikte `rB`. `glowTaper` tekent automatisch zowel een felle binnenkern als een zachte, bredere buitenhalo (`1.8x` zo breed op `35%` alpha). Door meerdere `glowTaper`-segmenten achter elkaar te koppelen ontstaan gebogen 3D-vlammen, windlinten en bliksemschichten.
     * **`lightDisc(at, radius, color, alpha)` & `glowDisc(...)`:** Camera-gerichte (billboarded) gloeiende bollen/schijven met een heldere kern en zachte rand.
     * **`flare(at, radius, color, alpha)`:** Een felle camera-gerichte ster-flits bij explosies en inslagen.
     * **`circle(center, normal, radius, width, color, alpha)`:** Een platte 3D-ring met instelbare normaalvector en lijndikte.
     * **`edge(a, b, color, alpha)` / `lightLine` / `glowLine`:** Scherpe 3D-lijnen voor runen en bliksemtakken.
     * **`haze(center, rx, ry, rz, color, alpha)`:** Een volumetrische, semi-transparante 3D-box voor mist- en plasmakoepels.
     * **`shape(Shape, Frame, fillRgb, edgeRgb, glowRgb, alpha, glowStrength)`:** Rendert een volledige **3D-mesh** (`Mesh.lathe`, `Mesh.cylinder`, `Mesh.torus`) getransformeerd door een orthonormaal 3D `Frame` (`center`, `right`, `up`, `forward`, `scale`), inclusief vlakvulling, wireframe-randen en buitengloed.

---

## 3. Diepte-Analyse per Spell

---

### 3.1 FIREBALL (`FireballSpell.java` & `FireFx.java`)
* **School:** `MagicSchool.FIRE` (`0xFF5511`)
* **Cooldown:** `40 ticks` (`2.0 seconden`)
* **Bestanden:**
  * Server: `src/main/java/nl/tivek/multiversepowers/spell/fire/FireballSpell.java`
  * Client VFX: `src/main/java/nl/tivek/multiversepowers/spell/fire/client/FireFx.java`

#### A. Server-Side Mechanica & Physics
1. **Spawn & Projectiel-Gedrag:**
   * Berekent het handpunt rechts voor de speler via `Targeting.handPoint(player)` en de kijkrichting `look = player.getLookAngle()`.
   * Maakt een **anonieme subklasse van vanilla `SmallFireball`** aan op `hand` met beginsnelheid `look.scale(SPEED)` waarbij `SPEED = 1.25` blokken/tick (`25 blokken/s`).
   * Zet `accelerationPower = 0.1` zodat de vuurbal tijdens zijn vlucht niet vertraagt.
   * **Maximale vliegtijd (`MAX_FLIGHT_TICKS = 90`, `4.5s`):** Als de vuurbal na 90 ticks nog niets heeft geraakt (bijv. in de lucht geschoten), triggert hij automatisch `impact(level, position())` en verwijdert zichzelf (`discard()`).
   * **Water-interactie (`douse`):** Elke tick controleert `tick()` of `this.isInWaterOrBubble()`. Zo ja, dan dooft de vuurbal direct met een sissend geluid (`FIRE_EXTINGUISH`), `20 CLOUD`-stoomwolken, `14 BUBBLE_POP` en `10 SMOKE` particles, zonder te exploderen.
2. **Hit Detection, Brandstichting & Blast-Schade:**
   * **Blok-inslag (`onHitBlock`):** Controleert eerst via `mayInteract(level, pos)` of de speler op die chunk mag bouwen (spawn-protectie) en of `GameRules.RULE_DOFIRETICK` aan staat (`mayBurnAt`). Daarna roept `spreadFire(level, hitPos)` `igniteAt` aan op het geraakte blok én met **50% kans** op elk van de 8 omliggende kolommen in een `3x3` gebied (`dx ∈ [-1..1], dz ∈ [-1..1], dy ∈ [0, -1, 1]`).
   * **Entity-inslag (`onHitEntity`):** Als een entity direct wordt geraakt en brand is toegestaan, zet `super.onHitEntity` het doelwit `5 seconden` in brand en doet directe vuurbal-schade.
   * **Explosie (`blast` binnen `BLAST_RADIUS = 2.5` blokken):**
     * Zoekt alle `LivingEntity` binnen een straal van `2.5` blokken die door `SpellTargets.hits(caster, e)` én een **Line-of-Sight raycast** (`Targeting.clearPath(level, at, eyePosition)`) komen (muren blokkeren de explosie).
     * Markeert elk slachtoffer met **`DeathStyles.mark(target, DeathStyles.Style.ASH)`** zodat ze bij een dodelijke treffer in as uiteenvallen.
     * **Afstands-geschaalde schade:**
       $$\text{falloff} = 1.0 - 0.6 \times \frac{\text{afstand}}{2.5}$$
       $$\text{damage} = 4.0 \times \text{PowerRules.damage()} \times \text{falloff}$$
       Op het middelpunt doet de explosie dus `4.0` schade (2 harten) bovenop de directe treffer, aflopend naar `40%` (`1.6` schade) aan de uiterste rand.
     * Zet slachtoffers `3 seconden` (`BURN_SECONDS = 3`, `60 ticks`) in brand en duwt ze radiaal weg (`strength = 0.45 * falloff`, `lift = 0.25 * falloff`).

#### B. Server-Side Particle Effecten (`FireballSpell.java`)
* **1. Hand-Ontsteking (`ignition`, duurt `8 ticks`):**
  * Tekent voor de hand van de caster een krimpende, draaiende gouden schijf (`ParticleFx.disc`, `radius = 0.55 * (1.0 - age / 9.0)`, `18` punten, draaisnelheid `age * 0.45 rad`) én op exact dezelfde plek een **tegendraaiend 5-puntig gouden pentagram** (`ParticleFx.discStar` met `5` punten, `skip = 2`, draaisnelheid `-age * 0.35 rad`) dat overloopt van `GOLD` (`0xFFD36B`) naar `FLAME` (`0xFF6A1A`).
  * Op `age == 0` zuigt een `ParticleFx.implosion` `16 SMALL_FLAME` particles van `0.6` blokken naar het handpunt toe.
* **2. Vlucht-Spoor (`trail`, elke tick zolang de vuurbal vliegt):**
  * **Kern:** Een roterende mini-bol (`ParticleFx.sphere`, `r = 0.25`, `6` punten) van `CORE` (`0xFFE27A`) + een overgangs-dust van `FLAME` (`0xFF6A1A`) naar `EMBER` (`0x7A1F0A`).
  * **Dubbele DNA-Helix rond de vuurbal:** Berekent met `ParticleFx.basis(direction)` de twee loodrechte assen `u` en `v` loodrecht op de vliegrichting en tekent **2 tegenoverliggende spiraalarmen** (`strand = 0..1`, `step = 0..2`) op straal `0.45` blokken (`angle = age * 0.9 + strand * PI + step * 0.45`) van `FLAME` en `SMALL_FLAME`.
  * **Hitte-staart:** `3` stappen achter de vuurbal (`0.35` blokken per stap) met krimpende `GOLD -> EMBER` dust, `LARGE_SMOKE` en `35%` kans op vallende `LAVA` / `FALLING_LAVA` druppels.
* **3. Inslag-Animatie (`impact`, duurt `50 ticks` = `2.5s`):**
  * **Tick 0:** `FLASH`, `EXPLOSION`, een Fibonacci-bol (`sphereOut`) van **`70 FLAME`** (`speed = 0.32`) + **`22 LARGE_SMOKE`** (`speed = 0.18`), `35 LAVA` spetters, een 5-puntig `groundStar` pentagram op de grond (`r = 2.0`), een radiale `shockwave` (`36 FLAME` punten) en `GENERIC_EXPLODE` + `FIRECHARGE_USE` geluid.
  * **Tick 1 t/m 10:** Uitdijende grondringen (`ParticleFx.ring`) die groeien van `0.35` naar `2.85` blokken. Op `age == 6` verschijnt een donkere schroeiring (`SCORCH = 0x2A1A12`, `40` punten op `r = 2.3`).
  * **Tick 2 t/m 34:** Een opstijgende rookkolom (`CAMPFIRE_COSY_SMOKE` met `dy = 0.07` + `EMBER -> SCORCH` roetdeeltjes) die tot `3.6` blokken hoog kringelt.

#### C. Client-Side Custom 3D VFX (`FireFx.java`)
Kleurenpalet in `FireFx`: `WHITE (0xFFF6D6)`, `YELLOW (0xFFD84A)`, `ORANGE (0xFF6A1A)`, `RED (0xC8230C)`, `EMBER (0x5A1408)`, `SMOKE (0x261B16)`.
1. **Vliegende Vuurbal (`FireFx.fly` via `SpellFxPayload.Kind.FIREBALL_FLY`):**
   * **Sub-tick interpolatie:** Berekent de exacte positie tussen de vorige en huidige tick via `pos = fx.was.lerp(fx.at, partialTick)`.
   * **14-Punts Lichtspoor (`trail`):** Loopt door de `ArrayDeque<Vec3> trail` van de afgelopen 14 ticks en verbindt elk segment met een dubbele taps toelopende straal: een brede `glowTaper` (`ORANGE -> RED`, dikte tot `0.32`) en een felle `lightTaper` kern (`YELLOW -> ORANGE`, dikte tot `0.15`). Om de 3 punten tekent hij een opstijgende `SMOKE` `lightDisc`.
   * **6 Wapperende Vlam-Linten (`ribbon = 0..5`):** Achter de vuurbal hangen 6 procedurele linten van elk `5` segmenten (`1.3` blokken lang). Elk lint golft met een sinusbeweging loodrecht op de vliegrichting (`wave = sin(phase + u * 5.0) * 0.18 * u`) en versmalt van `0.16` naar `0.02`.
   * **Kokende 3D Vuurkern (`FireFx.ball`):**
     * Buitenste `glowDisc` (`RED` + `ORANGE`, `1.9r`), daarbinnen **4 geanimeerde vuurlobben** die elke frame van richting veranderen via `Noise.direction(seed + k * 19, step)` (`ORANGE` + `YELLOW` `lightDisc`), en in het hart een `YELLOW` + `WHITE` kern-`glowDisc` met een `flare`.
     * **Camera-Fade (`close`):** Als de vuurbal dichter dan `2.2` blokken bij de camera van de speler is (bijv. vlak na het afvuren), schaalt `close = clamp((camDist - 0.6) / 1.6, 0.2, 1.0)` de helderheid van de witte kern omlaag zodat het scherm van de caster niet verblind wordt.
2. **Explosie (`FireFx.burst` via `SpellFxPayload.Kind.FIREBALL_BURST`, duurt `40 ticks`):**
   * **Tick 0–4:** Een grote `WHITE`/`YELLOW` `flare` (`r = 3.6`) en `ORANGE` `glowDisc` (`r = 3.0`).
   * **Tick 0–22 (`7` opstijgende vuurbollen):** `7` losse `ball()`-lobben zwellen op met een veer-curve (`Ease.backOut(age / 6.0)`), drijven naar buiten en stijgen op (`rise = age * (0.04 + 0.03 * n)`).
   * **Tick 0–10 (Hittering op de grond):** Een platte `circle` die uitdijt van `0.6` naar `4.8` blokken (`ORANGE` + `YELLOW`).
   * **Tick 0–18 (`10` Dansende Vuurtongen - `FireFx.tongue`):** Rond het inslagpunt staan `10` procedurele vuurtongen in een cirkel. Elke vuurtong bestaat uit `5` aan elkaar gekoppelde `glowTaper` + `lightTaper` segmenten die tot `2.4` blokken hoog reiken en in 3D kronkelen via `sin(age * 0.45 + v * 4.0 + index)` en `cos(...)`.
   * **Tick 3–40 (Paddenstoel-Rookwolk):** `9` donkere `SMOKE` `lightDisc`-wolken stijgen op tot `2.7` blokken boven de krater en waaieren bovenin breder uit.

---

### 3.2 LIGHTNING STRIKE (`LightningSpell.java` & `StormFx.java`)
* **School:** `MagicSchool.LIGHTNING` (`0x00D2FF`)
* **Cooldown:** `160 ticks` (`8.0 seconden`)
* **Bestanden:**
  * Server: `src/main/java/nl/tivek/multiversepowers/spell/lightning/LightningSpell.java`
  * Client VFX: `src/main/java/nl/tivek/multiversepowers/spell/lightning/client/StormFx.java`

#### A. Server-Side Mechanica: Aim-Tracking, Virtuele Bliksem & Chain Lightning
1. **14-Tick Oplaadfase met Dynamische Entity-Tracking (`CHARGE = 14 ticks` = `0.7s`):**
   * Bij het casten voert de server `Targeting.aim(player, level, RANGE)` uit met **`RANGE = 40.0` blokken**.
   * `Targeting.aim` geeft een `AimTarget` terug. Als de speler bij het casten op een bewegende vijand mikte, roept de tick-loop tijdens de hele oplaadfase (`age < 14`) elke tick **`target[0] = aim.current()`** aan. De oplaad-rune volgt het doelwit dus terwijl het probeert weg te rennen!
   * Stuurt direct `SpellFxPayload.Kind.LIGHTNING_CHARGE` (voor de grond-rune en donderwolk) en speelt `BEACON_ACTIVATE` + opbouwende `CREEPER_PRIMED` statische tikjes waarvan de pitch oploopt van `1.2` naar `2.04`.
2. **De Inslag op `age == 14` (`strike`):**
   * **Waarom de vanilla `LightningBolt` NIET in de wereld wordt gespawnd:**
     De mod wil de lelijke, blokkerige vanilla Minecraft-bliksemschicht vervangen door de eigen `StormFx`-bliksem, maar wil wél alle speciale vanilla bliksem-interacties behouden (Creepers opladen tot Charged Creeper, Villagers veranderen in Witches, Pigs in Zombified Piglins, Mooshrooms wisselen, Schildpadden voor schilden).
   * **De Oplossing in `strike()`:**
     * Maakt een `LightningBolt` aan in het geheugen via `EntityType.LIGHTNING_BOLT.create(level)` **zonder `level.addFreshEntity(bolt)` aan te roepen**.
     * Zet `bolt.moveTo(spot)` en `bolt.setCause(caster)`.
     * Zoekt alle entities in een `6x12x6` box rond het inslagpunt (`spot.x ± 3, spot.y - 1 .. spot.y + 6, spot.z ± 3`).
     * Controleert per entity `EventHooks.onEntityStruckByLightning(struck, bolt)` — deze event wordt onderschept door `SpellCasting.onStruckByLightning`, die de treffer **annuleert** als het de caster zelf is, een teamgenoot, of een niet-targetbare speler!
     * Markeert vijanden met `DeathStyles.Style.ASH` en roept handmatig **`struck.thunderHit(level, bolt)`** aan (doet 5 vanilla bliksemschade, zet in brand en triggert alle mob-transformaties).
   * **Grond-schok & Terrein-brokstukken (`shock` & `igniteAround`):**
     * Alle vijanden binnen `SHOCK_RADIUS = 3.0` blokken krijgen **`Slowness III`** (`MobEffects.MOVEMENT_SLOWDOWN`, amplifier `2`) voor `SHOCK_TICKS = 30` (`1.5s`) en een opwaartse schok (`strength = 0.35`, `lift = 0.25`).
     * Op `NORMAL` en `HARD` difficulty steekt `igniteAround` het inslagpunt en tot `4` willekeurige plekken eromheen in brand (`BaseFireBlock.getState`).
     * Leest het blok onder het inslagpunt (`level.getBlockState(ground)`) en schiet `40 BlockParticleOption(ParticleTypes.BLOCK, groundState)` brokstukken de lucht in.
3. **Chain Lightning na de inslag (`age > 14`, `chainStep`):**
   * Elke **`CHAIN_EVERY = 2 ticks` (`0.1s`)** na de hoofdinslag zoekt de bliksem vanaf het laatst geraakte punt (`chainFrom[0]`) naar de dichtstbijzijnde vijand binnen **`CHAIN_REACH = 6.0` blokken** die:
     1. Nog niet in `Set<UUID> hit` zit;
     2. Geldig doelwit is volgens `SpellTargets.hits(caster, e)`;
     3. Vrije zichtlijn heeft vanaf het vorige doelwit (`Targeting.clearPath`).
   * **Weer-afhankelijke sprongen (Storm Bonus):**
     * Normaal springt de bliksem maximaal **`CHAIN = 3` keer** door naar extra vijanden.
     * Als het regent of onweert op het inslagpunt (`level.isRainingAt(BlockPos.containing(spot))`), verhoogt de limiet naar **`STORM_CHAIN = 5` sprongen**!
   * Elke geketende vijand krijgt:
     * `DeathStyles.Style.ASH` markering;
     * **`4.0 * PowerRules.damage()`** `damageSources().lightningBolt()` schade;
     * **`Slowness III`** voor `1.5s` en een ketting-schok (`0.25` push, `0.15` lift);
     * Een server-side dubbele `ParticleFx.zigzag` (`16` stappen `ELECTRIC_SPARK` + `12` stappen `CYAN -> DEEP` dust) én een client-side `SpellFxPayload.Kind.LIGHTNING_ARC` tussen de borsthoogtes (`y + height * 0.6`) van beide doelwitten.

#### B. Client-Side Custom 3D VFX (`StormFx.java`)
Kleurenpalet in `StormFx`: `WHITE (0xF5FCFF)`, `PALE (0xB8F2FF)`, `CYAN (0x48DBFB)`, `BLUE (0x1B9CFC)`, `DEEP (0x0A3D91)`, `STORM (0x1E272E)`, `GREY (0x3A4650)`.
1. **Fase 1: Oplaad-Rune & Donderwolk (`StormFx.charge`, `14 ticks`):**
   * **3-Rings Arcane Grond-Rune (`RUNE = 2.2` blokken):** Tekent op `y + 0.04` drie concentrische ringen (`r = 0.45, 0.72, 1.0 * 2.2`) via `arcOnGround` (`40` segmenten). De ringen tekenen zichzelf geleidelijk vol naarmate de oplaadtijd `p = age / 14.0` vordert (`sweep = p * 2 * PI`) en draaien om en om tegen elkaar in (`spin * (r % 2 == 0 ? 1.0 : -1.3)`).
   * **8 Radiale Rune-Spaken met Glief-Haakjes:** Zodra `p > 0.25`, groeien er 8 spaken vanuit de binnenring naar de buitenring met aan elk uiteinde een schuin rune-haakje (`hook = angle + 0.22 rad`).
   * **Knetterende Grond-Vonken (`jag`):** `3` tot `8` kleine gekartelde bliksemboogjes (`5` segmenten) die willekeurig over de rune verspringen.
   * **16-Delige Draaiende Donderwolk (`CLOUD = 18.0` blokken hoog):** Op `18` blokken boven het doelwit ontstaat een donkere onweerswolk uit `16` roterende `STORM` (`0x1E272E`) en `GREY` (`0x3A4650`) `lightDisc`-wolken (`radius = 1.6..3.2`), met interne `CYAN`/`WHITE` weerlicht-flitsen in de wolkenbuik.
   * **Geleidingskanaal (`p > 0.7`):** In de laatste `0.2s` voor de inslag verschijnt een flinterdunne `CYAN`/`WHITE` `glowTaper` + `lightTaper` draad die de grond-rune met de wolk op `18` blokken hoogte verbindt.
2. **Fase 2: De Blikseminslag (`StormFx.bolt`, `32 ticks`):**
   * Simuleert **echte bliksem-fysica** in meerdere fasen:
   * **Stepped Leader (`age < LEADER = 1.5 ticks`):** Eerst schiet een zwakkere voorloper (`leader`) in `1.5` tick van `18` blokken hoogte naar de grond langs een gekarteld pad van `22` segmenten (`StormFx.jagged`), berekend met `Noise.direction(seed, i)` vermenigvuldigd met `sin(PI * u)` zodat de boven- en onderkant exact op hun plek blijven.
   * **3 Return Strokes (`STROKES = 3`, `STROKE_GAP = 2.0 ticks`):**
     * Vanaf `age = 1.5` vuren er kort na elkaar **3 verblindende hoofdontladingen** door het kanaal, elk met een iets ander gekarteld pad (`shape = seed + stroke * 31`).
     * Elke hoofdontlading wordt getekend uit **3 concentrische lagen** over alle `22` segmenten:
       1. Buitenste corona: `DEEP` blauwe `glowTaper` (`dikte * 4.5`, `35%` alpha);
       2. Middelste plasmalaag: `CYAN` `glowTaper` (`dikte * 1.8`, `85%` alpha);
       3. Kern: `WHITE` `lightTaper` (`dikte * 0.36`, `100%` alpha).
     * **5 Zijtakken (`b = 0..4`):** Vanuit 5 knooppunten op de hoofdschicht schieten gekartelde zijtakken (`6` segmenten lang) schuin naar buiten en beneden (`branchEnd`), die eveneens uit `CYAN` `glowTaper` + `WHITE` `lightTaper` bestaan.
   * **Sky-Flash, Plasmakoepel & Gloeiende Grondscheuren:**
     * Zet `Minecraft.getInstance().level.setSkyFlashTime(2)` zodat de hele Minecraft-lucht kort oplicht zoals bij echt onweer.
     * Tekent op het inslagpunt een `5.0`-blok `flare`, een uitdijende 3D-plasmakoepel (`painter.haze` die groeit tot `2.9` blokken breed en `2.0` hoog), een platte schokgolf-`circle` (`tot 4.5` blokken) en **9 radiale gekartelde grondscheuren** (`jag`) op een zwartgeblakerde grondvlek (`0x0D1117`) die kwadratisch afkoelen (`cool * cool`) van witheet naar donkerblauw.
3. **Fase 3: Chain-Lightning Boog (`StormFx.arc`, `9 ticks`):**
   * Tekent tussen twee geketende vijanden twee in elkaar gevlochten gekartelde bliksembogen (`10` segmenten, een primaire boog met `0.14 * len` uitwijking en een secundaire boog met `0.05 * len` uitwijking en seed `+77`).
   * De seed verandert elke `~0.67` tick (`frameSeed = seed + (int)(age * 1.5)`), waardoor de ketting-bliksem levendig door de lucht knettert, met `flare`-flitsen op beide uiteinden.
---

### 3.3 POISON AREA (`PoisonSpell.java` & `PoisonFx.java`)
* **School:** `MagicSchool.NATURE` (`0x2ED573`)
* **Cooldown:** `240 ticks` (`12.0 seconden`)
* **Bestanden:**
  * Server: `src/main/java/nl/tivek/multiversepowers/spell/nature/PoisonSpell.java`
  * Client VFX: `src/main/java/nl/tivek/multiversepowers/spell/nature/client/PoisonFx.java`

#### A. Server-Side Mechanica: Parabolische Flacon-Worp & Escalerend Gif
1. **Wiskundige Parabolische Worp (Geen Vanilla Potion Entity):**
   * Raycast tot **`RANGE = 24.0` blokken** via `Targeting.aimPoint(player, level, 24.0)` vanaf `hand = Targeting.handPoint(player)`.
   * Berekent de vliegtijd op basis van de horizontale/3D afstand:
     $$\text{flight} = \text{clamp}\left(\text{round}\left(\frac{\text{distance}}{1.2}\right),\ 6,\ 18\right) \text{ ticks } (0.3\text{s} - 0.9\text{s})$$
   * Berekent de top van de boog (`peak`):
     $$\text{peak} = 1.2 + \text{distance} \times 0.15 \text{ blokken}$$
   * Elke tick (`t = (age + 1) / flight`) ligt de exacte positie van de flacon op:
     $$\text{point}(t) = \text{lerp}(\text{hand}, \text{target}, t) + \left(0,\ \sin(t \cdot \pi) \times \text{peak},\ 0\right)$$
   * Terwijl de flacon vliegt, laat hij op de server een spoor achter van `TOXIC -> FOG` dust, groene `ENTITY_EFFECT` druppels en `SLIME_BALL` item-particles.
2. **Het Breken van de Flacon (`shatter` op `t >= 1.0`) & De Gifwolk (`160 ticks` = `8.0s`):**
   * Zodra `t >= 1.0` bereikt is, spat de flacon uiteen op `target` met:
     * `SPLASH_POTION_BREAK`, `BREWING_STAND_BREW` en `SLIME_SQUISH` geluiden;
     * `24 ItemParticleOption(ParticleTypes.ITEM, Items.SLIME_BALL)` en **`10 ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_BOTTLE)`** glasscherven;
     * Een radiale `ParticleFx.shockwave` van `36` felgroene `ENTITY_EFFECT` particles;
     * Een `SpellFxPayload.Kind.POISON_CLOUD` packet voor een duur van **`DURATION = 160 ticks` (`8.0s`)**.
   * **Uitdijen & Vervagen (`currentRadius`):**
     * De wolk heeft een maximale straal van **`RADIUS = 3.5` blokken**.
     * In de eerste `SPREAD_TIME = 8 ticks` (`0.4s`) groeit de straal van `30%` (`1.05m`) naar `100%` (`3.5m`).
     * In de laatste `FADE_TIME = 25 ticks` (`1.25s`) krimpt de actieve straal geleidelijk terug naar `40%` (`1.4m`).
   * **Gas-Oprispingen (`belch`):** Elke `14 ticks` (`age % 14 == 7`) borrelt er op een willekeurige plek in het moeras een gasbel open (`8 SNEEZE` + `10 FOG -> DARK` dust + `SLIME_SQUISH_SMALL` geluid).
3. **Escalerende Gif-Sterkte (`poison`, elke `PULSE = 10 ticks` = `0.5s`):**
   * Houdt een `Map<UUID, Integer> inside` bij die per slachtoffer optelt hoeveel ticks hij al in de gifwolk staat (`inside.merge(uuid, 10, Integer::sum)`).
   * Elke `0.5s` krijgen alle vijanden in de cilinder (`r = currentRadius`, hoogte `-0.5 .. +2.0` blokken):
     * **`Slowness I`** (`MobEffects.MOVEMENT_SLOWDOWN`, amplifier `0`) voor `30 ticks` (`1.5s`).
     * **Standaard (`soaked <= 40 ticks` / `2.0s`):** **`Poison I`** (`MobEffects.POISON`, amplifier `0`) voor `60 ticks` (`3.0s`).
     * **Geëscaleerd (`soaked > STRONGER_AFTER = 40 ticks` / langer dan `2.0s` in de wolk):** Het gif upgrade automatisch naar **`Poison II`** (`amplifier = 1`), speelt `SoundEvents.WITCH_DRINK` af op het slachtoffer en schiet `10` gifgroene `ENTITY_EFFECT` sporen uit zijn lichaam!

#### B. Client-Side Custom 3D VFX (`PoisonFx.java`)
Kleurenpalet in `PoisonFx`: `BRIGHT (0xB8FF6B)`, `TOXIC (0x63E63A)`, `FOG (0x3F7A2A)`, `MURK (0x1F3A14)`, `CORK (0x9C6B3B)`.
1. **Echte 3D Glazen Flacon Mesh (`PoisonFx.VIAL` & `PoisonFx.CORK_SHAPE`):**
   * In plaats van een plat 2D sprite-plaatje bouwt `PoisonFx` bij het opstarten een **echte 3D-mesh van een alchemie-flacon** met behulp van een **omwentelingslichaam (`Mesh.lathe`)** met `14` segmenten rondom en `9` profiel-ringen van bodem tot flessenhals:
     * Bodem: `(r=0.02, y=-0.26)` $\rightarrow$ `(r=0.17, y=-0.22)`
     * Bolle buik: `(r=0.24, y=-0.12)` $\rightarrow$ `(r=0.22, y=0.0)`
     * Versmallende schouder & hals: `(r=0.14, y=0.09)` $\rightarrow$ `(r=0.07, y=0.16)` $\rightarrow$ `(r=0.07, y=0.22)`
     * Uitstaande schenkrand: `(r=0.10, y=0.24)` $\rightarrow$ `(r=0.09, y=0.26)`
   * Bovenop de hals zit een tweede 3D-mesh (`CORK_SHAPE`): een **10-zijdige 3D-cilinder (`Mesh.cylinder`)** van `y = 0.20` tot `y = 0.27` in kurkkleur (`CORK = 0x9C6B3B`).
   * **Vlucht & Tuimeling (`PoisonFx.vial`):**
     * Berekent exact dezelfde parabolische boog `arc(from, to, t)` als de server, plus het punt `0.04` verderop om de raaklijn (`ahead`) te bepalen.
     * Bouwt een 3D `ConstructPainter.Frame` dat met de vliegrichting meedraait én om zijn dwars-as tuimelt met `spin = age * 0.9 rad`.
     * Rendert het glazen flesje semi-transparant (`alpha = 0.55`, `FOG` vulling, `BRIGHT` randen, `TOXIC` gloed), de kurk (`alpha = 0.85`), én **binnenin de buik van de fles** (`frame.at(0, -0.05, 0)`) een klotsende `TOXIC`/`BRIGHT` vloeistof-`glowDisc` + `lightDisc`!
     * Achter de vliegende fles hangt een `5`-segments druppelend gifspoor (`glowTaper`).
2. **Het 3D Gifmoeras (`PoisonFx.cloud`):**
   * **Grond-Rune met Draaiend Pentagram:** Op `y + 0.04` ligt een `TOXIC` buiten-`circle`, een `MURK` moerasbodem en een langzaam roterend **5-puntig ster-pentagram** (`5` punten verbonden van hoekpunt `k` naar `k + 2` met `painter.edge` in `BRIGHT`).
   * **Volumetrische Mistbox:** Een `painter.haze` over de volledige straal (`1.3` blokken hoog) die het gebied in een groene nevel hult.
   * **22 Rollende Mistbanken (`i = 0..21`):** `22` grote `FOG` en `MURK` `lightDisc`-wolken die met `Noise` langzaam in cirkels drijven en op en neer deinen.
   * **5 Opstijgende Spiraal-Gasflarden (`t = 0..4`):** `5` slierten van elk `8` gekoppelde `lightTaper` + `glowTaper` segmenten die tot `1.6` blokken omhoog kronkelen en bovenin vervagen (`1.0 - u`).
   * **14 Zwellende & Openknappende Gifbellen (`b = 0..13`):**
     * Elke bel heeft een eigen cyclus `cycle = ((time * speed) + offset) % 1.0`.
     * Tijdens de eerste `90%` van de cyclus (`cycle < 0.9`) stijgt de bel op tot `1.4` blokken hoogte en zwelt hij op van `0.06` naar `0.20` (`TOXIC` `glowDisc` + `BRIGHT` glansstipje).
     * In de laatste **`10%` van de cyclus (`cycle >= 0.9`) knapt de bel uit elkaar**: de straal schiet tot `2.2x` naar buiten terwijl de alpha naar `0` vervaagt!

---

### 3.4 WIND GUST (`WindGustSpell.java` & `WindFx.java`)
* **School:** `MagicSchool.AIR` (`0xDDEEF2`)
* **Cooldown:** `100 ticks` (`5.0 seconden`)
* **Bestanden:**
  * Server: `src/main/java/nl/tivek/multiversepowers/spell/air/WindGustSpell.java`
  * Client VFX: `src/main/java/nl/tivek/multiversepowers/spell/air/client/WindFx.java`

#### A. Server-Side Mechanica: Zelf-Redding, Voortschrijdende Kegel & Projectiel-Reflectie
1. **Directe Effecten op de Caster zelf (`cast`):**
   * **Vuur doven:** Als de caster in brand staat (`player.isOnFire()`), roept de spell direct `player.clearFire()` aan en speelt `FIRE_EXTINGUISH`.
   * **Valbreker / Luchtkussen (`CUSHION = 20 ticks` = `1.0s`):** Als de caster in de lucht valt (`!player.onGround() && player.getDeltaMovement().y < 0`), wordt `player.resetFallDistance()` aangeroepen, krijgt de speler **`Slow Falling`** voor `1 seconde` en spawnt er een ring van `16 CLOUD` particles onder zijn voeten.
2. **Voortschrijdende 120° Drukgolf-Kegel (`8 ticks`, `RANGE = 8.0` blokken, `WAVE_SPEED = 1.0` blok/tick):**
   * De windvlaag is geen instant-hit, maar een fysieke drukgolf waarvan het front (`front = (age + 1) * 1.0`) met `20 blokken/s` naar voren raast tot `8.0` blokken ver.
   * **Kegelfilter via Inwendig Product (Dot Product):**
     * De visuele halve kegelhoek is `HALF_ANGLE = 50°` (`100°` totaal); de fysieke hitbox gebruikt `HALF_ANGLE + 10 = 60°` aan weerszijden (**`120°` totale kegelbreedte**, `cos(60°) = 0.5`).
     * Voor elke entity wordt gecontroleerd:
       $$\text{afstand} \le \text{front} \quad \wedge \quad \widehat{(\text{pos} - \text{origin})} \cdot \vec{\text{look}} \ge \cos(60^\circ)$$
     * Daarnaast controleert `Targeting.clearPath(level, origin, eyePosition)` of er geen muur tussen de caster en het doelwit staat.
   * **Knockback op Vijanden (`push`):**
     * Een `Set<UUID> pushed` zorgt dat elke vijand maar **één keer** door dezelfde windvlaag wordt geraakt zodra het golffront hem bereikt.
     * Hoe dichter bij de caster, hoe harder de klap:
       $$\text{closeness} = 1.0 - \frac{\text{afstand}}{8.0} \times 0.6 \quad (\text{loopt van } 100\% \text{ naar } 40\%)$$
       $$\text{strength} = 2.2 \times \text{closeness}, \qquad \text{lift} = 0.55$$
     * Als een geraakte entity in brand stond, blaast de wind het vuur ook bij hem uit (`entity.clearFire()`).
   * **Projectiel-Reflectie & Losse Items Wegblazen (`blowAway`):**
     * **Items & XP Orbs (`ItemEntity`, `ExperienceOrb`):** Losliggende items en XP-bolletjes in de kegel worden met `+0.6 * look` en `+0.25 Y` weggeblazen.
     * **Projectielen Deflecteren & Overnemen (`Projectile`):**
       * Alle vliegende projectielen (pijlen, vuurballen, drietanden, snowballs) in de kegel die niet van de caster of een teamgenoot zijn, worden **in de lucht omgekeerd**!
       * De nieuwe snelheid wordt `look.scale(max(0.8, huidigeSnelheid * 0.9)) + (0, 0.1, 0)`.
       * **`shot.setOwner(caster)`:** De caster wordt de nieuwe eigenaar van het projectiel (zodat een teruggekaatste pijl of vuurbal de oorspronkelijke schutter raakt en kill-credit aan de caster geeft!).
       * Zet `shot.hasImpulse = true`, `shot.hurtMarked = true`, speelt `SoundEvents.BREEZE_DEFLECT` en spawnt `6 CRIT` + `4 CLOUD` particles op het afbuigpunt.

#### B. Client-Side Custom 3D VFX (`WindFx.java`, duurt `LIFE = 14 ticks`)
Kleurenpalet in `WindFx`: `WHITE (0xFFFFFF)`, `PALE (0xEAF6FA)`, `SKY (0xB8E0EE)`, `DEEP (0x78B4CC)`.
1. **3 Spiraal-Windlinten rond de Caster (`WindFx.swirl`, tick `0..9`):**
   * Tekent bij het afvuren **3 spiraalvormige luchtstromen** (`r = 0..2`) van elk `16` segmenten die vanaf de voeten van de caster in `1.5` omwenteling (`3 * PI`) tot `2.3` blokken hoogte om de speler heen wervelen (`WHITE` `lightTaper` + `SKY` `glowTaper`).
2. **De 3D Windkegel (`WindFx.gust`):**
   * Bouwt vanuit de kijkrichting `ahead = fx.to.normalize()` een orthonormaal 3D-stelsel `(ahead, side, up)`.
   * **1. Vier Gelaagde Boog-Sikkels (`layer = 0..3`):**
     * Er vliegen **4 gebogen wind-sikkels** achter elkaar aan (met `0.55` blokken tussenruimte).
     * Elke sikkel bestaat uit `18` segmenten over de volle kegelbreedte van `-50°` tot `+50°`.
     * **3D Welving (`bow` & `wave`):** Het midden van elke sikkel bolt naar voren uit via `bow = d * (1.0 + 0.12 * cos(yaw * 1.8))` en golft verticaal op en neer via `wave = sin(u * PI * 3.0 + time * 0.5 + layer) * 0.12`.
     * De uiteinden van de sikkel vervagen zachtjes via `tip = sin(PI * u)` en elk segment wordt opgebouwd uit **3 lagen**: een `SKY` `glowTaper` (`0.22`), een `WHITE` `lightTaper` (`0.09`) en een verschoven `PALE` highlight-rand (`+0.04 up`).
   * **2. Zes Rollende Luchtwervels (`WindFx.curl`, `w = 0..5`):**
     * Op de voorrand van de drukgolf tollen **6 spiraal-wervels** (`curl`) mee. Elke wervel bestaat uit `10` segmenten die in `1.6 * PI` radialen naar binnen krullen (`r = size * (1.0 - 0.6 * u)`).
   * **3. Twaalf Razendsnelle Windstrepen (`s = 0..11`):**
     * `12` naalddunne snelheidslijnen (`1.8` blokken lang, lopend van dikte `0.0` achteraan naar `0.07` vooraan) schieten met `16 blokken/s` door de kegel heen.

---

### 3.5 VOID WALK (`VoidWalkSpell.java`, `VoidFx.java`, `ClientVoidState.java` & `void_world.fsh`)
* **School:** `MagicSchool.DARK` (`0x6C5CE7`)
* **Cooldown:** `600 ticks` (`30.0 seconden`)
* **Duur:** `200 ticks` (`10.0 seconden`)
* **Bestanden:**
  * Server: `src/main/java/nl/tivek/multiversepowers/spell/dark/VoidWalkSpell.java`
  * Client 3D VFX: `src/main/java/nl/tivek/multiversepowers/spell/dark/client/VoidFx.java`
  * Client Shader & Glow Controller: `src/main/java/nl/tivek/multiversepowers/spell/dark/client/ClientVoidState.java`
  * GLSL Fragment Shader: `src/main/resources/assets/welcomescreen/shaders/program/void_world.fsh`

#### A. Server-Side Mechanica: Echte Onzichtbaarheid, Equipment-Spoofing, AI-Blindheid & Ambush
1. **Staat-Opslag (`Walk` record in `ACTIVE` map):**
   * Slaat bij de start in `ACTIVE.put(uuid, new Walk(wasSilent, beforeInvis, gameTime))` op of de speler al stil was (`player.isSilent()`) en of de speler vóór het casten al een eigen `INVISIBILITY` potion-effect had (inclusief de resterende duur).
2. **Volledige Onzichtbaarheid + Netwerk Equipment-Spoofing (`hideEquipment`):**
   * In standaard Minecraft blijft bij het `INVISIBILITY` effect je gedragen harnas, zwaard en schild gewoon zichtbaar voor andere spelers.
   * `VoidWalkSpell` lost dit op door:
     1. Een verborgen `MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false, true)` te geven (`visible = false`, dus **geen** potion-bubbels rond de speler!);
     2. `player.setSilent(true)` te zetten (geen voetstap- of zwemgeluiden);
     3. Een tijdelijke **`+50%` Movement Speed modifier** (`SPEED_BONUS = 0.5`, `Operation.ADD_MULTIPLIED_TOTAL`) toe te voegen;
     4. **Alle 6 equipment-slots (`MAINHAND`, `OFFHAND`, `HEAD`, `CHEST`, `LEGS`, `FEET`) voor andere spelers te verbergen** door handmatig een `ClientboundSetEquipmentPacket(player.getId(), emptySlots)` met `ItemStack.EMPTY` te broadcasten via `chunkSource.broadcast(player, packet)`!
   * **Beveiliging tegen tussentijds wisselen of nieuwe kijkers:**
     * Als een andere speler tijdens jouw Void Walk binnen render-afstand komt (`PlayerEvent.StartTracking` $\rightarrow$ `VoidWalkSpell.seenBy`), stuurt de server direct dat lege equipment-packet naar die nieuwe kijker.
     * Als de Void Walker tijdens zijn wandeling van wapen of armor wisselt (`LivingEquipmentChangeEvent` $\rightarrow$ `VoidWalkSpell.equipmentChanged`), stuurt de server op het einde van de tick direct opnieuw het lege packet zodat zijn item nooit zichtbaar flitst!
3. **Volledige Mob-AI Blindheid:**
   * Bij het betreden van de Void zoekt de server alle `Mob` entities binnen `64` blokken die de caster als doelwit hebben (`mob.getTarget() == player`) en zet hun target op `null`.
   * Zolang `VoidWalkSpell.isInVoid(entity)` `true` is, annuleert `SpellCasting.onChangeTarget` elke `LivingChangeTargetEvent` waarbij een mob de Void Walker probeert te targeten.
4. **Caster-Only Fluisteringen, Voetstappen & Vijand-Markeringen (`whisper`):**
   * Terwijl andere spelers niets zien, stuurt de server via de speler-specifieke `level.sendParticles(player, ...)` (alleen zichtbaar voor de caster zelf!):
     * Elke `3 ticks` tijdens het lopen: paarse `REVERSE_PORTAL` voetstap-slierten;
     * Elke `10 ticks`: een paarse `VIOLET` dust + `REVERSE_PORTAL` markering `0.5` blokken boven het hoofd van elke vijand binnen **`MARK_RADIUS = 32.0` blokken**;
     * Op `age == 160` (`2 seconden` voordat Void Walk afloopt): een privé waarschuwingsgeluid (`BEACON_DEACTIVATE`) zodat de speler weet dat hij bijna weer zichtbaar wordt.
5. **De Ambush-Aanval (`VoidWalkSpell.ambush` op `LivingIncomingDamageEvent`):**
   * Als de speler vanuit de Void een directe aanval (`event.getSource().getDirectEntity() == player`) op een doelwit landt:
     * Wordt de volledige schade van de klap vermenigvuldigd met **`AMBUSH = 1.5F` (`+50%` totale schade)**!
     * Krijgt het slachtoffer **`Blindness I`** (`MobEffects.BLINDNESS`) én **`Slowness II`** (`amplifier = 1`) voor **`BLIND_TICKS = 40` (`2.0 seconden`)**.
     * Speelt `WARDEN_SONIC_BOOM` + `ENDERMAN_TELEPORT` + `PLAYER_ATTACK_CRIT` af, spawnt een `40`-punts Fibonacci-bol van `REVERSE_PORTAL` + `SOUL_FIRE_FLAME` + `SCULK_SOUL` en stuurt `SpellFxPayload.Kind.VOID_AMBUSH`.
     * Roept direct `leave(player)` aan waardoor de Void Walk eindigt.
6. **Terugkeer & Herstel (`leave` & `restore`):**
   * Wordt aangeroepen na `200 ticks`, bij een Ambush, of als het Invisibility-effect voortijdig wordt verwijderd (bijv. door melk te drinken).
   * Herstelt `player.setSilent(walk.wasSilent())`, verwijdert de `+50%` snelheidsbonus, herstelt een eventueel eerder aanwezig Invisibility-drankje met exact de resterende tijd (`left = before.getDuration() - elapsed`), en stuurt een nieuw `ClientboundSetEquipmentPacket` met de **echte items** van de speler naar alle omstanders.

#### B. Client-Side Custom 3D VFX (`VoidFx.java`)
Kleurenpalet in `VoidFx`: `BLACK (0x040008)`, `ABYSS (0x07000E)`, `DEEP (0x4A1A99)`, `VIOLET (0x9B5CFF)`, `PALE (0xE2CCFF)`, `WHITE (0xFAF5FF)`.
1. **De "Bol van Niets" (`VoidFx.sphere`):**
   * In tegenstelling tot gewone gloedbollen (die licht optellen) creëert `VoidFx.sphere` een **zwart gat** door buitenste violette gloedlagen te combineren met een pikzwarte kern:
     1. Buitenste `DEEP` (`0x4A1A99`) `glowDisc` op `1.9x` straal;
     2. Middelste `VIOLET` (`0x9B5CFF`) `glowDisc` op `1.25x` straal;
     3. Twee dekkende `ABYSS` (`0x07000E`) zwarte `lightDisc`-lagen op `1.05x` en `0.80x` straal die het achterliggende beeld verduisteren;
     4. Een haarscherpe `VIOLET` + `PALE` **gravitatie-lens ring** (`painter.circle` op `1.08x` straal, altijd loodrecht op de kijkrichting van de camera gericht).
2. **Void Enter — Implosie gevolgd door Explosie (`VoidFx.enter`, `30 ticks`):**
   * **Fase 1 (`age < PULL = 6 ticks` — De Implosie):** `18` violette lichtstralen (`lightTaper` + `glowTaper`) worden vanaf `3.6` blokken afstand naar het borstpunt van de caster gezogen terwijl de zwarte `sphere()` in het midden groeit van `0.15` naar `1.1` blokken.
   * **Fase 2 (`age >= 6 ticks` — De Uitbarsting):** Op `t = 0` klapt de bol met een `Ease.backOut`-veer uit tot `1.45` blokken straal met een `3.0` `flare`, twee uitdijende schokgolf-ringen (op de grond tot `6.0` blokken en op borsthoogte tot `3.8` blokken) en **`18` rondvliegende `ABYSS`/`VIOLET` scheur-scherven (`shards`)**.
3. **Void Leave (`VoidFx.leave`, `16 ticks`):**
   * Een korte `flare`, een zwarte `sphere()` die krimpt van `1.35` naar `0.05` blokken, een krimpende grondring en `10` vervagende lichtstrepen.
4. **Void Ambush — 3 Diagonale Klauw-Scheuren in de Ruimte (`VoidFx.strike`, `14 ticks`):**
   * Berekent de slagrichting `ahead` van de aanvaller naar het slachtoffer en het loodrechte vlak `(right, up)` via `Vectors.across(ahead)`.
   * Scheurt **3 parallelle diagonale klauw-wonden** (`k = -1, 0, 1`) open over het doelwit (`lengte = 1.9` blokken).
   * Elke klauw-scheur bestaat uit twee helften die in het midden breed zijn en naar beide punten toe scherp weglopen (`0.0 -> 0.6 -> 0.0` `VIOLET` `glowTaper` met daarbinnen een pikzwarte `0.0 -> 0.22 -> 0.0` `ABYSS` `lightTaper` kern en een `PALE` snijlijn).

#### C. De Custom GLSL Post-Processing Shader & Wallhack-Glow (`ClientVoidState.java` & `void_world.fsh`)
Wanneer de caster zelf in de Void stapt, ontvangt zijn client `VoidStatePayload(200)`. Dit activeert `ClientVoidState`:
1. **Wallhack Vijand-Markering (`ClientVoidState.markEnemies`):**
   * Zolang de speler in de Void zit, scant de client elke tick alle geladen entities binnen `32.0` blokken (`level.entitiesForRendering()`).
   * Elke vijand (`Relations.standing(player, living) == Standing.HOSTILE`) krijgt client-side de **Glowing outline** (`GlowFlag.setGlowing(entity, true)`) opgezet, zodat hun silhouet dwars door muren heen wordt gerenderd door Minecraft's outline-buffer! Zodra een vijand buiten bereik raakt of Void Walk stopt, zet `clearMarks` de glow weer netjes uit.
2. **Shader Lifecycle & Hartslag:**
   * Laadt de post-processing keten `welcomescreen:shaders/post/void_world.json` in `GameRenderer` en speelt elke `20 ticks` (`1.0s`) een lokale `WARDEN_HEARTBEAT` af.
   * Interpoleert de shader-uniform `Intensity` vloeiend: **`8 ticks` fade-in** bij het betreden en **`15 ticks` fade-out** na afloop (zodat de shader nooit abrupt aan of uit knippert).
3. **Hoe de GLSL Fragment Shader (`void_world.fsh`) elk beeldpunt (pixel) transformeert:**
   * **Stap 1 — Luminantie (`luma`):** Berekent de helderheid van de originele pixel via `dot(color, vec3(0.299, 0.587, 0.114))`.
   * **Stap 2 — 3x3 Sobel Edge-Detection Filter:**
     * Samplet de 8 omliggende pixels (`tl, tc, tr, ml, mr, bl, bc, br`) op afstand `oneTexel`.
     * Berekent de horizontale en verticale Sobel-gradiënten:
       $$g_x = -tl - 2\cdot ml - bl + tr + 2\cdot mr + br$$
       $$g_y = -tl - 2\cdot tc - tr + bl + 2\cdot bc + br$$
       $$\text{edge} = \text{clamp}\left(\sqrt{g_x^2 + g_y^2} \times 2.5,\ 0.0,\ 1.0\right)$$
   * **Stap 3 — Schaduwwereld met Violette Contouren:**
     * Vervangt de normale wereldkleuren door een dieppaarse afgrond (`vec3(0.04, 0.015, 0.08) + vec3(0.16, 0.10, 0.24) * pow(light, 1.6)`) en tekent alle gedetecteerde blok- en wereld-randen fel violet (`+ vec3(0.62, 0.38, 1.0) * edge`).
   * **Stap 4 — Bloedrode Conversie van Gemarkeerde Vijanden (`outline`):**
     * De standaard glowing outline van Minecraft is spierwit (`R, G, B > 0.97`). De shader detecteert dit via `outline = smoothstep(0.97, 0.995, min(original.r, min(original.g, original.b)))` en kleurt deze pixels om naar **fel scharlakenrood (`vec3(1.0, 0.3, 0.35)`)**! Daardoor lichten vijanden bloedrood op tegen de donkerpaarse silhouet-wereld.
   * **Stap 5 — Ademende Puls & Vignette:**
     * Vermenigvuldigt het geheel met een `1 Hz` sinus-hartslag (`pulse = 0.9 + 0.1 * sin(Time * 6.28318)`) en trekt de schermranden naar het donkerpaars met een radiale vignette (`smoothstep(0.25, 0.95, length(fromCenter) * 1.3)`), gemixt met het originele beeld op basis van `Intensity`.

---

## 4. Samenvattende Vergelijkingstabel

| Eigenschap | **Fireball** (`FIRE`) | **Lightning Strike** (`LIGHTNING`) | **Poison Area** (`NATURE`) | **Wind Gust** (`AIR`) | **Void Walk** (`DARK`) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Basis Cooldown** | `2.0s` (`40t`) | `8.0s` (`160t`) | `12.0s` (`240t`) | `5.0s` (`100t`) | `30.0s` (`600t`) |
| **Bereik / Straal** | `25 m/s` vlucht (`4.5s` max), `2.5m` blast | `40m` raycast, `3.0m` schok, `6.0m` chain | `24m` worp, `3.5m` wolk (`8.0s` duur) | `8.0m` kegel (`120°`), `20 m/s` golf | Zelf (`10.0s` duur), `32m` vijand-radar |
| **Schade & Debuffs** | Direct + `4.0` AoE vuur, `3–5s` brand, `ASH` death | Vanilla `thunderHit` (`5.0`) + `4.0` per chain, `Slowness III` (`1.5s`), `ASH` | `Slowness I` + `Poison I` $\rightarrow$ na `2s` in wolk **`Poison II`** | Knockback (`tot 2.2`), dooft vuur, `Slow Falling` (`1s`), reflecteert projectielen | **Ambush: `1.5x` melee schade** + `Blindness I` & `Slowness II` (`2.0s`) |
| **Speciale Mechaniek** | Dooft in water met stoom; `3x3` vuurverspreiding | Volgt doelwit tijdens `0.7s` charge; **3 chains** (**5 in regen**); creeper/villager conversie zonder vanilla bolt | Wiskundige parabolische boog (`6–18t`); tijds-tracker per UUID in wolk | Verandert eigenaar (`setOwner`) van gekaatste pijlen/vuurballen naar caster | Spooft lege armor/hand packets naar andere spelers; wist mob targets |
| **Server `ParticleFx`** | Draaiend hand-pentagram, dubbele DNA-helix in vlucht, Fibonacci-bol (`92` p.) bij impact | Zigzag-kettingen, `40` blok-brokstukken van geraakte ondergrond | Parabolisch druppelspoor, glas/slijm-explosie, elke `14t` gas-oprisping | Voet-wolk bij val, kegel-wolken, `CRIT` bij projectiel-reflectie | Caster-only `REVERSE_PORTAL` voetstappen & vijand-bakens; Fibonacci-bol bij ambush |
| **Client 3D VFX (`ConstructPainter`)** | `14`-punts taper-trail, `6` golvende vlam-linten, kokende 3D vuurkern, `10` vuurtongen + rookpaddenstoel | `3`-rings zelfbouwende rune + `8` spaken, `16`-delige donderwolk op `18m`, stepped leader + `3` return strokes + `5` takken + `9` grondscheuren | Echte **3D Lathe-mesh flacon + kurk** met vloeistofkern, `5`-puntig moeras-pentagram, `5` gas-spiralen, **`14` knappende gifbellen** | `3` voet-spiralen, `4` gewelfde 3D wind-sikkels, `6` luchtwervels (`curl`), `12` snelheidsnaalden | Zwarte-gat bol (`ABYSS` kern + lens-ring), `18` implosie-stralen, `3` diagonale klauw-scheuren + **GLSL Sobel-shader (`void_world.fsh`)** met rode wallhack-outlines |
---

## 5. Diepte-Analyse: THUNDERCLAP Ability & Custom VFX (Thor)

Hoewel `Thunderclap` een character-ability van **Thor** is (`AbilitySlot.ABILITY_2`), deelt hij de onderliggende `SpellFxPayload`- en `SpellTargets`-architectuur met de spells, aangevuld met een eigen **full-body Inverse Kinematics (IK) animatiesysteem** en een **GLSL gravitatie-/tijd-lens shader**.

* **Bestanden:**
  * Configuratie & Dispatch: `src/main/java/nl/tivek/multiversepowers/character/GameCharacter.java` & `ThorPowers.java`
  * Server Mechanica: `src/main/java/nl/tivek/multiversepowers/character/thor/Thunderclap.java` & `ThorMoves.java`
  * Netwerk Packet: `src/main/java/nl/tivek/multiversepowers/spell/ClapPayload.java`
  * Client Input & Wind-up: `src/main/java/nl/tivek/multiversepowers/character/thor/client/ThorCombo.java`
  * Client 1st/3rd-Person Lichaams-Animatie: `src/main/java/nl/tivek/multiversepowers/spell/client/ClientClaps.java`
  * Client 3D VFX & Tijd-Bubble: `src/main/java/nl/tivek/multiversepowers/spell/client/ClapFx.java`
  * Client Lens-Refractie Shader: `src/main/java/nl/tivek/multiversepowers/engine/client/render/Lens.java` & `shaders/core/lens.fsh`

---

### 5.1 Activatie, Voorwaarden & Wind-Up Synchronisatie
1. **Vereisten (`GameCharacter.THOR`):**
   * **Input:** Linkermuisknop ingedrukt houden (`CharacterAbility.Input.LEFT`, `holdOnly(ThorPowers.CLAP_HOLD)` waarbij **`CLAP_HOLD = 15 ticks` = `0.75 seconden`**).
   * **Condities:** Alleen op de grond (`When.GROUND`), ongewapend zonder Mjolnir in de hand (`ThorPowers.UNARMED`), en met lege handen (`takesMouse`).
   * **Stats:** Basis-cooldown **`200 ticks` (`10.0 seconden`)**, basis-schade **`5.0`** (wordt vermenigvuldigd met `ThorCharge.fists(player)` wanneer Thor opgeladen is!).
2. **Slimme Scheiding tussen Combo-Slag en Thunderclap Wind-Up (`ThorCombo.windUp`):**
   * Omdat een korte klik op Linkermuisknop een vuistslag uit `ThorCombo` uitvoert (`Tap.RELEASE`), wacht `ThorCombo.windUp` tot de knop minstens **`CHARGE_SHOWN = 0.2F` (`20%` van `15 ticks` = `3 ticks` = `0.15s`)** ingedrukt is én de ability niet op cooldown staat.
   * Zodra `held >= 0.2F`:
     1. Start de client direct lokaal de oplaad-houding via `ClientClaps.charging(player.getId(), true)`;
     2. Stuurt de client een `AbilityActionPayload` met vlag `Characters.CHARGE` naar de server;
     3. De server zet in `ThorMoves.charging(player, true)` de bitflag `ThorStatePayload.CHARGING` aan en broadcast dit naar alle andere spelers (met een veiligheids-timeout van `CHARGE_LONGEST = 40 ticks` mocht het loslaat-pakket ooit wegvallen).
   * Terwijl een speler oplaadt, speelt `ClientClaps.onClientTick` elke `5 ticks` een aanzwellend elektrisch gezoem af op borsthoogte (`COPPER_BULB_TURN_ON`, volume `0.25 + 0.35 * charge`, pitch `1.5 + 0.5 * charge`).

---

### 5.2 Procedurele Lichaams- & Hand-Animatie (`ClientClaps.java`)
`ClientClaps` bestuurt zowel het **3rd-person spelermodel (`PlayerModel`)** als beide **1st-person armen (`RenderHandEvent`)** met één wiskundig `Shape(float up, float open, float drive, float shock)` model:

1. **De 4 Animatie-Parameters (`Shape`):**
   * **Tijdens het inhouden van de muisknop (`charge`, duurt `WIND = 12.0 ticks`):**
     * `up = Ease.smooth(charge)` (loopt van `0.0` naar `1.0`);
     * `open = 1.0` (armen wijd gespreid);
     * `drive = 0.0`, `shock = 0.0`.
   * **Zodra de klap afgaat (`ClapPayload`, na `15 ticks` inhouden):**
     * De handen slaan dicht in **`MEET = ClapPayload.HANDS_MEET + 1.0F = 4.0 ticks`** (`HANDS_MEET = 3` server-ticks):
       $$\text{shut} = \min\left(1.0,\ \frac{\text{age}}{4.0}\right), \qquad \text{open} = 1.0 - \text{shut}^2$$
       Door de kwadratische curve ($\text{shut}^2$) komen de armen eerst op gang vanuit de wijde stand en versnellen ze maximaal op het moment dat de handpalmen elkaar raken!
     * **Voorwaartse Lichaams-Drive (`drive`):** Het bovenlichaam werpt zich naar voren tijdens de klap en blijft **`HOLD = 12.0 ticks`** (`MEET + 8`) voorover staan voordat het in `7.0 ticks` (`END = 19.0 ticks`) terugkeert naar rust.
     * **Terugslag-Schok (`shock`):** Gedurende `6.0 ticks` na het raken van de handen (`age >= MEET`) geeft `Ease.bump((age - MEET) / 6.0)` een terugslag-schok door het lichaam.
2. **3rd-Person Inverse Kinematics (`ClientClaps.pose` & `body` via `Stance`):**
   * **Hoofd:** Kantelt tijdens de wind-up achterover (`HEAD_BACK = -0.5 rad`).
   * **Benen & Heupen:** Thor zakt diep door zijn knieën (`drop = up * (1.6 + 1.8 * drive) - 0.6 * shock` pixels) en zet zijn voeten breed en schuin uit elkaar (`±3.3` pixels op X, `+1.2` en `-1.0` op Z) via `Stance.leg(model, right, foot, KNEE)`.
   * **Romp & Rug:** Leunt tijdens de wind-up ver achterover met holle rug (`LEAN_BACK = -0.34 rad` borst, `ARCH_BACK = -0.24 rad` middel) en klapt bij de slag krachtig naar voren (`+0.26 * drive` borst, `+0.20 * drive` middel).
   * **Armen & Ellebogen (`Stance.arm`):** Interpoleert de handdoelen in het lokale borst-frame van wijd achter het lichaam (`WIDE = (13.5, -3.0, 1.5)`) naar vlak voor de borst (`MET = (2.0, 0.5, -8.5)`), met bijbehorende elleboog-vectoren (`WIDE_ELBOW` $\rightarrow$ `MET_ELBOW`).
3. **1st-Person Dubbele Arm-Renderer (`ClientClaps.onRenderHand`):**
   * Annuleert op `EventPriority.HIGHEST` de standaard rechterhand van Minecraft en tekent met `FirstPersonArm.arm` **beide armen tegelijk** in beeld.
   * Tijdens het opladen bewegen de handen naar de buitenranden van het scherm (`SPREAD = (±1.02, 0.08, -0.38)` vanuit schouderpunt `WIDE_FROM = (±1.25, -0.5, 0.2)`) en bij het vuren slaan ze recht voor de camera tegen elkaar op **`CLAP = (±0.05, -0.1, -0.75)`**!

---

### 5.3 Server-Side Mechanica & Voortschrijdende Kegel (`Thunderclap.java`)
1. **De 3-Tick Aanloop (`age < MEET`, waarbij `MEET = 3`):**
   * Zodra `Thunderclap.cast` start, stuurt de server `ClapPayload.send(player)` naar alle spelers in zicht zodat ieders client direct de arm-slag naar voren start.
   * Gedurende tick `0, 1, 2` volgt de server nog live de positie (`feet`, `eye`), kijkrichting (`ahead`) en het mikpunt (`aimed`, een raycast van `9.0` blokken die op het midden van een vijand of `0.3` blokken vóór een muur stopt).
   * Op `age == 0` klinkt het suizen van de armen door de lucht (`TRIDENT_RIPTIDE_1`, pitch `1.6`).
2. **Het Moment van Impact (`age == 3`, `t == 0` — `boom`):**
   * Berekent het botspunt van de handen: `hands = eye + ahead * 0.6 + (0, -0.3, 0)`.
   * Verpakt de hoogte van de handen boven de voeten in **honderdsten van een blok** (`drop = round((clap.y - feet.y) * 100)`) in het `ticks`-veld van `SpellFxPayload.CLAP`.
   * Speelt **4 gelaagde donder- en drukgolf-geluiden** tegelijk af:
     1. `BREEZE_WIND_CHARGE_BURST` (volume `2.0`, lage pitch `0.55`);
     2. `LIGHTNING_BOLT_THUNDER` (volume `3.0`, pitch `0.8`);
     3. `LIGHTNING_BOLT_IMPACT` (volume `2.0`, pitch `1.1`);
     4. `TRIDENT_THUNDER` (volume `1.5`, pitch `1.2`).
3. **De Razendsnelle Schokgolf-Kegel (`push`, `t >= 0`):**
   * Het golffront raast naar voren met **`WAVE_SPEED = 2.25` blokken/tick (`45 blokken/s`)** tot **`RADIUS = 9.0` blokken** (bereikt de volle 9 meter in `4 ticks` = `0.2s`).
   * **Kegel-oorsprong achter de caster (`CONE_BACK = 1.0`):** Het kegelpunt ligt `1.0` blok *achter* de ogen van Thor (`origin = eye - ahead * 1.0`), zodat vijanden die schuin naast Thor staan ook vol worden meegenomen.
   * **Kegel-hoek & Box-projectie (`HALF_ANGLE = 0.8 rad` $\approx 45.8^\circ$, totale kegel $\approx 91.7^\circ$):**
     * In plaats van alleen naar het middelpunt van een vijand te kijken, berekent `nearest(AABB, origin, ahead)` het **dichtstbijzijnde punt op de hitbox van de vijand tot de middenlijn van de kegel**. Grote mobs worden daardoor altijd eerlijk geraakt zodra een deel van hun lichaam in de kegel staat.
   * **Schade & Knockback:**
     * Afstands-factor: $\text{close} = 1.0 - 0.5 \times \frac{\text{afstand}}{9.0}$ (loopt van `100%` dichtbij naar `50%` op 9 meter).
     * Zet `target.invulnerableTime = 0` (negeert hurt-frames), doet `damage * close` `playerAttack`-schade, en slingert het doelwit weg via `SpellTargets.push` met kracht `1.6 * (0.5 + 0.5 * close)` en opwaartse lift `0.45 + max(0, way.y) * 0.8`, plus `14 ELECTRIC_SPARK` particles op het lichaam.
4. **De Verre Donder-Echo (`t == DISTANT = 9 ticks` na de klap):**
   * Precies `9 ticks` (`0.45s`) nadat de handen op elkaar klapten, rolt er een diepe donder-echo na in de verte (`LIGHTNING_BOLT_THUNDER` op `SoundSource.WEATHER`, volume `2.0`, diepe pitch `0.6`).

---

### 5.4 Client-Side Custom VFX, Tijd-Bubble & GLSL Lens-Shader (`ClapFx.java`, `Lens.java`, `lens.fsh`)
1. **1st-Person Hand-Synchronisatie (`ClapFx.seen`):**
   * Omdat de 1st-person handen met een eigen camera-projectie worden getekend, onderschept `SpellFx.add` het `CLAP`-pakket op het scherm van de caster zelf (als hij in 1st-person speelt) en verplaatst het startpunt `from` exact naar de plek waar de 1st-person handpalmen elkaar voor de camera raken (`camera.pos - look * 0.75 - up * 0.1`).
2. **Fysieke Scherm-Schok & Flits voor Omstanders (`ClapFx.felt` op `age < 1.0`):**
   * Elke speler binnen **`SHAKE_REACH = 32.0` blokken** voelt de klap via een kwadratische camera-shake: `CameraShake.add(5.0 * shake^2, 14 ticks)`.
   * Elke speler binnen **`FLASH_REACH = 20.0` blokken** ziet een ijsblauwe schermflits: `ScreenFlash.add(0xE6F4FF, 0.7 * flash, 10 ticks)`.
3. **De 5 Visuele Lagen van `ClapFx.draw` (`LIFE = 46 ticks` = `2.3s`):**
   * **Laag 1 — Hand-Flits (`light`, tick `0..8`):** Een felle `3.2`-blok `flare`, een `5.0`-blok `ICE` (`0x8FD3FF`) `glowDisc` en een `1.4`-blok `WHITE` (`0xF4FBFF`) `glowDisc` tussen de handpalmen.
   * **Laag 2 — De Lichtbuigende Tijd-Bubble (`bubble`, tick `0..12` + `Lens.java` & `lens.fsh`):**
     * Zwelt in `12 ticks` (`0.6s`) met een kubische ease-out (`1.0 - (1.0 - u)^3`) op van `0.5` naar **`BUBBLE_SIZE = 4.0` blokken straal**, terwijl de bol voor de handen uit rolt (`center = hands + ahead * radius * 0.8`).
     * Roept **`Lens.bubble(center, radius, on, PALE, 0.0)`** aan en tekent een dunne `PALE` (`0xBFE8FF`) 3D-schil (`painter.shell`) langs de rand.
     * **Hoe de GLSL Lens-Shader (`Lens.java` + `shaders/core/lens.fsh`) werkt:**
       1. Op `RenderLevelStageEvent.Stage.AFTER_LEVEL` kopieert `Lens.draw` de volledige gerenderde wereld (zowel de Color-buffer als de Depth-buffer via `GL30._glBlitFrameBuffer`) naar een offscreen `TextureTarget`.
       2. Voor elke geregistreerde `Bubble` berekent `lens.fsh` per schermpixel een 3D-straal (`viewPoint(uv, 1.0)`) en lost de **straal-bol snijdingsvergelijking** op (`tIn = b - h`, `tOut = b + h`).
       3. Vergelijkt de afstand tot het boloppervlak `t` met de diepte uit `DepthSampler`: blokken of spelers die *vóór* de tijdbubble staan worden niet vervormd (`if (scene < t) discard;`).
       4. Buigt het beeld achter de bol als een glazen drukgolf-lens (`bend`), gemoduleerd door **concentrische geluidsgolven (`wave = sin(off * 20.0 - Clock * 0.6)`)**.
       5. Samplet het achtergrondbeeld **12 keer in een Fibonacci-spiraal (`turn = k * 2.39996`)** met **chromatische aberratie** (Rood op `at - fringe`, Groen op `at`, Blauw op `at + fringe`) zodat licht aan de randen van de schokgolf in kleurrijke prisma-randen uiteenvalt, afgewerkt met een **Fresnel-gloed (`pow(off, 5.0)`)** en een scherpe randlijn!
   * **Laag 3 — Tijd-Vertraagde Dondervonken (`streaks`, `STREAKS = 190`):**
     * Gebruikt de tijdfunctie **`slowed(age)`**:
       $$\text{slowed}(\text{age}) = \begin{cases} 0.35 \times \text{age} & \text{als } \text{age} < 12 \text{ (binnen de tijdbubble)} \\ 12 \times 0.35 + (\text{age} - 12) & \text{als } \text{age} \ge 12 \text{ (nadat de bubble uiteenspat)} \end{cases}$$
     * Zolang de tijdbubble bestaat (`age < 12`), loopt de tijd voor alle **`190` vonk-strepen (`glowTaper` + `lightTaper`)** dus op slechts **35% snelheid (`SLOWED = 0.35`)**! De vonken lijken bijna bevroren in de lucht te zweven binnen de lens-bol en schieten op volle snelheid verder zodra de bol op `age == 12` openbarst.
   * **Laag 4 — Gevorkte Bliksem-Ontladingen in de Golf (`arcs` & `jagged`, tick `0..12`, `ARCS = 14`):**
     * Terwijl het golffront met `2.25` blokken/tick naar voren rolt, knetteren er tot `14` bliksembogen tegelijk midden in de drukgolf (elke tick op een nieuwe plek via `s = seed * 31 + tick * 97 + k`).
     * Elke boog bestaat uit `5` geknikte segmenten (`WHITE` `lightLine` + `BLUE` `glowLine`) en heeft halverwege (`i == kinks / 2`) **60% kans om te splitsen (`fork`)** in een zijtak van `3` segmenten.
   * **Laag 5 — De Uitrollende Mistmuur (`dust`, tick `0..46`, `PUFFS = 30`):**
     * `30` grote `DUST` (`0xC9D6E0`) `lightDisc` nevelwolken rollen met `Ease.smooth(age / 10.0)` over de volle `9.0` meter uit en vervagen kwadratisch (`left * left`).

---

## 6. De Oplaad- & Cooldown-Cirkel rond de Crosshair (`ThunderGauge.java` & `GuiShapes.java`)

Bestanden:
* `src/main/java/nl/tivek/multiversepowers/character/thor/client/ThunderGauge.java`
* `src/main/java/nl/tivek/multiversepowers/engine/client/gui/GuiShapes.java`

### 6.1 Hoe de Cirkel-Geometrie wordt opgebouwd (`GuiShapes.java`)
Alle ringen, bogen en bliksemstreepjes rond de crosshair worden procedureel getekend met `RenderType.gui()` quads (zonder PNG-textures):
* **Automatische Winding-Order Correctie (`GuiShapes.quad`):** Omdat `RenderType.gui()` backface-culling gebruikt, berekent `GuiShapes.quad` eerst het 2D kruisproduct (`turn = (x0*y1 - x1*y0) + ...`). Als `turn > 0`, draait hij de 4 hoekpunten automatisch om zodat elk segment altijd zichtbaar is.
* **Procedurele Cirkelbogen (`GuiShapes.arc`):** Verdeelt elke boog van `fromDegrees` tot `toDegrees` (waarbij `0°` recht bovenaan de crosshair ligt en met de klok mee draait: $x = c_x + \sin(\theta)\cdot r,\ y = c_y - \cos(\theta)\cdot r$) in stappen van maximaal **`STEP_DEGREES = 5.0°`** en vult elk stapje tussen binnenstraal `inner` en buitenstraal `outer` met een quad.
* **Lijnstukken met Dikte (`GuiShapes.stroke`):** Berekent de loodrechte normaalvector `(-alongY, alongX)` op een lijnstuk en tekent een rechthoekige quad van breedte `width`.

---

### 6.2 Werking van de Crosshair-Cirkel (`ThunderGauge.java`)
`ThunderGauge` tekent de cirkel precies gecentreerd rond de crosshair op `(guiWidth * 0.5, guiHeight * 0.5)` met binnenstraal **`INNER = 7.5px`** en buitenstraal **`OUTER = 9.5px`**.

De cirkel reageert dynamisch op de actieve Linkermuisknop-hold ability van Thor (`leftHold`: **Thunderclap** wanneer ongewapend op de grond, **Hammer Uppercut** met Mjolnir op de grond, of **Air Shockwave** tijdens het vliegen) en doorloopt **4 fasen**:

1. **Fase 1 — Opladen (`charge`, terwijl je Linkermuisknop ingedrukt houdt):**
   * Wordt zichtbaar zodra `MouseHold.progress(clap, partialTick)` voorbij **`SHOWN = 0.08F` (`8%`)** is (zodat een snelle klik voor een normale vuistslag de cirkel niet laat knipperen).
   * Tekent eerst een zwarte schaduw-ring (`6.5px .. 10.5px`, `35%` alpha) en een donkerblauwe achtergrond-ring (`DARK = 0x0B1A2E`, `7.5px .. 9.5px`, `85%` alpha) over `0° .. 360°`.
   * Tekent daaroverheen de **oplaad-boog** van `0°` (bovenaan) met de klok mee tot `end = 360° * filling`, waarbij `filling = (progress - 0.08) / 0.92`. De kleur van de boog mixt tijdens het vullen vloeiend van `STORM` (`0x3F8CFF`) naar helder cyaan `BOLT` (`0x9FE8FF`).
   * **Knetterende Vonken op het Oplaad-Punt:** Elke `45 ms` (`flicker = now / 45L`) schieten er op de bewegende punt van de boog (`end ± 6°`) **2 witte elektrische vonk-streepjes (`GuiShapes.stroke`)** dwars door de ring naar buiten (`r0 = 6.0px` tot `r1 = 10.5 .. 13.0px`).
2. **Fase 2 — Volledig Opgeladen Uitbarsting (`pop`, duurt `POP_MS = 380 ms` zodra `progress >= 1.0`):**
   * Op het exacte moment dat de cirkel `100%` bereikt (`fullAt = now`):
     * Flitst de hele ring spierwit (`WHITE = 0xF4FBFF`);
     * Dijt er een tweede schokgolf-ring (`BOLT`) vanuit de crosshair naar buiten uit van `9.5px` naar **`19.5px`** (`out = OUTER + 10.0 * u`);
     * Springen er **6 gekartelde bliksemschichten (`bolt()`, elk bestaande uit `3` zigzag-stappen met `Noise.of(seed, i, 7)`)** radiaal rondom de cirkel naar buiten (tot `15.5 .. 21.5px` ver)!
3. **Fase 3 — Cooldown Aftel-Ring (`cooldown > 0`):**
   * Zolang de ability op cooldown staat (`ClientCharacter.cooldownLeft(clap.slot()) > 0`), verschijnt er een dunne buitenring net buiten de oplaadring (**`10.0px .. 11.5px`**, `OUTER + 0.5` tot `OUTER + 2.0`).
   * De booglengte krimpt vloeiend (met `partialTick` interpolatie) van `360°` terug naar `0°` op basis van:
     $$\text{left} = \frac{\text{cooldown} - \text{partialTick}}{\text{cooldownFrom}}$$
4. **Fase 4 — "Ready" Glans-Rotatie (`ready`, duurt `READY_MS = 450 ms` zodra `cooldown == 0`):**
   * Op de milliseconde dat de cooldown van `> 0` naar `0` springt (`readyAt = now`), suist er in `0.45s` één lichtglans volledig (`360°`) rond de buitenring (`10.0px .. 11.5px`), opgebouwd uit een `50°` lange cyaan staart (`BOLT`, `at - 50° .. at`) en een `8°` felle witte kop (`WHITE`, `at - 8° .. at`).