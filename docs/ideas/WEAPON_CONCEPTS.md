# Construct Wheel: Huidige Wapens & Nieuwe Wapenconcepten

Dit document bevat de analyse van de huidige wapens in het Construct Wheel (Green Lantern) en gedetailleerde concepten voor 5 nieuwe wapens met elk 4 vaardigheden/aanvallen.

---

## Deel 1: Huidige Wapens in het Construct Wheel

Alle wapens in het wiel volgen het universele besturingssysteem van de mod:
- **Linksklik Tap:** Snelle basisaanval / primaire slag / schot
- **Linksklik Hold (2 sec drempel):** Zware aanhoudende aanval / kanaal-aanval
- **Rechtsklik Tap:** Tactische actie / utility / secundaire stoot
- **Rechtsklik Hold (2 sec drempel):** Defensieve houding / schild / gebiedsbeheersing

---

### 1. Zwaard & Schild (SWORD_SHIELD)
- **Type:** Tweehandig (zwaard in rechterhand, schild in linkerhand).
- **Status:** Volledig geïmplementeerd.
- **Vier Vaardigheden:**
  1. **Linksklik Tap (Zwaardslagen):** Combo-systeem met 12 dynamische slagen (o.a. Slash, Cleave, Uppercut, Overhead, Lunge, Spin). Schade schaalt per slagtype.
  2. **Linksklik Hold (Flurry):** Brengt het schild voor de borst (blokkeert 60% frontale schade) en vuurt een razendsnelle reeks van 12 opeenvolgende stoten/steken af over het hele front.
  3. **Rechtsklik Tap (Schildstorm / Shield Charge):** Stormt met 12 blokken/s voorwaarts en beukt vijanden opzij met 1 van 6 ram-variaties (1,5 hart). Eindigt met een schokgolf (3 harten) bij botsing of na 1,4 s.
  4. **Rechtsklik Hold (Schildblokkering):** Heft het hard-licht schild en blokkeert 85% van alle binnenkomende frontale schade zolang de knop wordt vastgehouden.

---

### 2. Energiezweep (ENERGY_WHIP)
- **Type:** Eénhandig (opgerolde energiezweep aan de rechtervuist).
- **Status:** Functioneel in code, momenteel vergrendeld voor visuele/prestatie rework.
- **Vier Vaardigheden:**
  1. **Linksklik Tap (Zweepslag):** 12 gevarieerde zweeptechnieken (Forehand, Backhand, Overhands, Enkelslag, Cowboyknal, etc.). Doet 3,5 harten (+30% aan het uiteinde/tip) en slaat vijanden in de slagrichting.
  2. **Linksklik Hold (Wervelstorm):** Zweept in het rond boven het hoofd; raakt alle vijanden rondom (1,5 hart per tick, gooit vijanden weg). Loslaten levert een zware eindknal op (5 harten).
  3. **Rechtsklik Tap (Lasso):** Schiet tot 10 blokken ver, bindt een vijand vast in lichtlussen, sleurt het doelwit naar je toe en smakt het voor je voeten tegen de grond (3 harten + 2 sec vertraging).
  4. **Rechtsklik Hold (Draaischild):** Roteert de zweep als een snelle propeller voor je; ketst vijandelijke projectielen af en reduceert frontale schade met 60%.

---

### 3. Plasma Vlammenwerper (FLAMETHROWER)
- **Type:** Zwaar tweehandig (pistoolgreep achter, voorgreep onder de ronde plasma-tank).
- **Status:** Volledig geïmplementeerd met hittemeter en oververhittingsmechaniek.
- **Vier Vaardigheden:**
  1. **Linksklik Tap (Vlammenzwaai):** Brede horizontale zwaai met groen plasma (82 graden breedte, 5,5 blokken bereik); brengt 2 harten schade toe en duwt doelwitten opzij.
  2. **Linksklik Hold (Inferno):** Continue brullende plasmastraal (10 blokken ver, 1,5 hart per 4 ticks). Bouwt warmte op op de HUD-hittemeter; raakt na 15 seconden continu vuren oververhit en blaast af.
  3. **Rechtsklik Tap (Vlammenmuur):** Veegt de vuurmond over de grond en werpt een barrière van vuur op (5,5 breed, 2,6 hoog, 3 seconden actief) die projectielen verbrandt en vijanden tegenhoudt.
  4. **Rechtsklik Hold (Vlammenwervel):** Richt naar de grond en laat vlammen om je heen opstijgen in een vurige tornado; halveert inkomende schade en explodeert bij loslaten naar buiten in een ring van vuur.

---

## Deel 2: Concepten & Ideeën voor de Nieuwe Wapens

---

### 1. RPG / Raketwerper (ROCKET_LAUNCHER)
- **Klasse:** **Zwaar tweehandig (Heavy 2-handed)**.
- **Visueel & Hantering:** Een kolossale, futuristische hard-licht lanceerbuis die op de rechterschouder rust. De linkerhand ondersteunt de voorgreep en het digitale richtvizier. Achterop zit een zware straalpijp (exhaust port) waar bij elk schot een ring van energie uit blaast. Speler loopt iets trager en zwaarder tijdens het dragen.

#### De 4 Vaardigheden:
1. **Linksklik Tap: Heavy Rocket Blast (Primair schot)**
   - **Werking:** Vuurt direct een zware energie-raket af langs de vizierlijn. De raket vliegt met lichte acceleratie en slaat in met een massieve explosie (radius van 4 blokken).
   - **Schade & Effect:** 6 harten directe inslag + flinke spat- en schokgolfschade. Vijanden vliegen in ragdoll/knockback achteruit.
   - **Kosten & Tempo:** Hoge kracht per schot (bijv. 2,5 power), vuurtempo circa 1 schot per 1,2 seconde met herlaad-animatie (een nieuwe raket kristalliseert in de buis).

2. **Linksklik Hold: Multi-Lock Cluster Barrage (Gerichte Salvo)**
   - **Werking:** Door linksklik 2 seconden in te houden verschijnen er lock-on ringen op maximaal 4 vijanden in beeld. Bij loslaten lanceert de RPG 4 snelle micro-raketten tegelijk die in sierlijke bogen naar de vergrendelde doelen suizen.
   - **Schade & Effect:** 2,5 harten per raket (totaal 10 harten als alles op 1 baas vergrendeld is). Verspreidt over meerdere doelen voor krachtige crowd-control.

3. **Rechtsklik Tap: Blast Jump & Ground Concussion (Raketsprong)**
   - **Werking:** De speler richt de uitlaat/monding direct naar beneden en triggert een gerichte ontlading tegen de grond.
   - **Schade & Effect:** Slaat nabije vijanden hard achterover (2 harten schade), terwijl de speler 8 blokken hoog de lucht in gelanceerd wordt (uitstekend te combineren met flight of smash attacks).

4. **Rechtsklik Hold: Flak Interceptor Field (Defensief Afweersysteem)**
   - **Werking:** De RPG wordt schuin omhoog geheven en projecteert een waaiervormig flak-veld van knetterende energieladingen voor de speler.
   - **Schade & Effect:** Vernietigt automatisch alle naderende projectielen (pijlen, vuurballen, vijandelijke bolts) en deelt aanhoudende micro-schokken uit aan vijanden die te dichtbij komen.

---

### 2. Armkanon (ARM_CANNON)
- **Klasse:** **Zwaar énhandig (Heavy 1-handed)**.
- **Visueel & Hantering:** Vormt zich naadloos rond de gehele rechtervoorarm van de speler tot over de hand als een zware cybernetische loop van hard licht met koelplaten, kernreactor-gloed en expanderende segmenten. De linkerhand is volledig vrij (bijv. voor stoten, zwaaien of balans).

#### De 4 Vaardigheden:
1. **Linksklik Tap: Kinetic Cannon Shot (Kanonkogel van Licht)**
   - **Werking:** Een zware, ronde energiekogel die met een donderende knal en felle terugslag wordt gelanceerd.
   - **Schade & Effect:** 4 harten schade. Doordringt tot 2 vijanden en veroorzaakt bij elke inslag een mini-schokgolf die doelwitten 2 blokken achteruit duwt.

2. **Linksklik Hold: Mega Buster / Siege Cannon (Oplaadbaar belegeringsschot)**
   - **Werking:** De loopsegmenten schuiven open en zuigen lichtenergie aan met een stijgende zoemtoon en trilling. Bij release na 2 seconden vuurt het kanon een reusachtige energiegolf af die een spoor van ontreddering trekt.
   - **Schade & Effect:** 8 harten directe schade; slaat door hele rijen vijanden heen en vernietigt losse projectielen in zijn pad.

3. **Rechtsklik Tap: Concussive Impact / Ground Pound (Schokgolf-stoot)**
   - **Werking:** De speler slaat met het zware kanon krachtig op de grond of voor zich uit.
   - **Schade & Effect:** Een halfronde schokgolf over de vloer (3 blokken radius, 3 harten schade) die vijanden de lucht in lanceert en kort ontwapent/stunt.

4. **Rechtsklik Hold: Aegis Barrier / Arm Fortress (Uitschuifbare Schermbepantsering)**
   - **Werking:** De zijkanten van het armkanon klappen wijd open tot een massief zeshoekig energieschild rond de arm.
   - **Schade & Effect:** Absorbeert 75% van alle schade van voren en reflecteert 30% van de geabsorbeerde schade terug in een ring rond de speler wanneer het schild wordt losgelaten.

---

### 3. Minigun (MINIGUN)
- **Klasse:** **Zwaar tweehandig (Heavy 2-handed)**.
- **Visueel & Hantering:** Een kolossale 6-loops roterende gatling gun. De speler draagt hem laag bij de heup met een stevige beugelhandgreep bovenop en een bedieningsgreep achter. De roterende lopen bouwen momentum op voordat het vuurgeweld losbarst. Bewegen is merkbaar trager tijdens het spinnen.

#### De 4 Vaardigheden:
1. **Linksklik Tap: Spin & Burst (Snelle 8-schots kogelregen)**
   - **Werking:** Een snelle spin van de loop die een strakke salvo van 8 energetische kogels lanceert met hoge precisie.
   - **Schade & Effect:** 0,75 hart per kogel (tot 6 harten bij volledige treffer). Ideaal om snel opduikende doelwitten uit te schakelen.

2. **Linksklik Hold: Bullet Hell / Lead Storm (Continu Spervuur)**
   - **Werking:** De minigun spint op tot topsnelheid (hoorbaar huilend geluid) en spuugt een ononderbroken muur van lichtkogels (15 kogels per seconde) zolang de knop wordt vastgehouden.
   - **Schade & Effect:** Onstuitbare DPS op grote groepen en bosses. Duwt doelwitten continu achteruit. Heeft een loop-temperatuur die na 12 seconden de vuursnelheid tijdelijk vertraagt.

3. **Rechtsklik Tap: Shredder Sweep (Spreidingsveeg)**
   - **Werking:** De speler maakt een snelle, krachtige horizontale zwaai met de spinnende loop, waarbij kogels in een brede waaier van 120 graden worden gesproeid.
   - **Schade & Effect:** 3 harten voor iedereen in de waaier; veegt kleine mobs (zoals spinnen en zombies) in één keer van het veld.

4. **Rechtsklik Hold: Gyro Stabilization & Deflector Stance (Vaste Vuurbasis)**
   - **Werking:** De speler plant beide voeten vast in de grond. De extreem snelle rotatie van de lopen wekt een magnetisch lichtkrachtveld op aan de voorzijde.
   - **Schade & Effect:** Volledige immuniteit tegen knockback, 50% minder inkomende schade, en projectielen die de roterende loop raken ketsen verbrijzeld af.

---

### 4. Sawed-off Shotgun (SHOTGUN)
- **Klasse:** **Middelzwaar tweehandig (Mid 2-handed)**.
- **Visueel & Hantering:** Een compact, afgezaagd dubbelloops jachtgeweer met klassieke houten/lichte kolf en twee dikke lopen. Zeer mobiel en agressief. Bedoeld voor close-quarters combat (hit-and-run).

#### De 4 Vaardigheden:
1. **Linksklik Tap: Buckshot Spread (Enkelloops hagelschot)**
   - **Werking:** Vuurt één van de twee lopen af: een brede kegel van 10 lichtpellets.
   - **Schade & Effect:** Van dichtbij dodelijk (tot 5 harten bij vol contact), zwakt snel af op afstand. Stevige punch die doelwitten een meter terugwerpt.

2. **Linksklik Hold: Double-Barrel Obliteration (Beide lopen tegelijk)**
   - **Werking:** Na 2 seconden focussen haalt de speler beide trekkers gelijktijdig over. Een oorverdovende knal met een enorme vuurflits.
   - **Schade & Effect:** 9 harten schade van dichtbij. De enorme terugslag duwt de speler zelf 2 blokken achteruit en blaast vijanden finaal omver (ragdoll/stumble effect).

3. **Rechtsklik Tap: Stock Strike & Barrel Jab (Kolf-slag & Stoot)**
   - **Werking:** Een bliksemsnelle mêlee-uithaal met de kolf of de stalen loopmonding.
   - **Schade & Effect:** 2 harten schade, verbreekt de aanval van de tegenstander (stagger/interrupt) en zet de vijand direct op perfecte afstand voor een hagelschot.

4. **Rechtsklik Hold: Combat Crouch / Breacher Stance (Tactische Dekking)**
   - **Werking:** De speler duikt in een lage, defensieve vechthouding achter de shotgun en laadt een zware solid slug (pantserbrekende kogel).
   - **Schade & Effect:** 40% minder schade tijdens het hurken; bij het loslaten vuurt de shotgun een geconcentreerde slug af met extreem bereik en hoge pantserpenetratie.

---

### 5. Revolvers (REVOLVERS)
- **Klasse:** **Speciaal: 1-handig (Single) of 2-handig (Akimbo / Dual-Wield)**.
- **Hantering & Karakter:**
  - Kan gebruikt worden met **één revolver** (wendbaar, uiterst precies, snelle heuptikken of zware focus-schoten) of met **twee revolvers tegelijk** (Akimbo: maximale vuurdichtheid, dubbele schade, kruisvuur).
  - Ondersteunt zowel **Lichte/Snelle acties** (fanning, reflexen) als **Zware/Langzame acties** (gerichte magnum-kogels, zware executieschoten).

#### De 4 Vaardigheden:
1. **Linksklik Tap: Quick Draw / Fan the Hammer (Snel / Licht Schot)**
   - **1 Revolver (Single):** Schiet vliegensvlug uit de heup (fanning the hammer) met 3 snelle kogels achter elkaar (1,5 hart per schot, hoge precisie).
   - **2 Revolvers (Dual):** Beide revolvers vuren gelijktijdig een snel kruislings schot af (2x 1,8 hart) met een vloeiende dubbele terugslag.

2. **Linksklik Hold: High Noon / Deadeye Execution (Zwaar / Langzaam Schot)**
   - **1 Revolver (Single):** Richt over de korrel met maximale focus (zoomt licht in). Na 2 seconden volgt een loodzware, doordringende magnum-kogel (7 harten) die vijanden doorboort en armor negeert.
   - **2 Revolvers (Dual):** De speler kruist de armen en vuurt een zware dubbele barrage af in tegengestelde richtingen of geconcentreerd op één punt (2x 4,5 harten met massieve knockback).

3. **Rechtsklik Tap: Akimbo Toggle & Trick Spin (Wissel & Pistoolslag)**
   - **Werking:** Schakelt direct tussen **1 Revolver** (precisie en mobiliteit) en **2 Revolvers** (maximale vuurkracht).
   - **Mêlee-effect:** Als er een vijand binnen 2 blokken staat, gaat de wissel gepaard met een spectaculaire revolver-spin die de vijand met de kolf in het gezicht slaat (2 harten schade + korte desoriëntatie).

4. **Rechtsklik Hold: Gunslinger Drift & Bullet Parry (Defensieve Flow)**
   - **Werking:** De speler neemt een dynamische cowboy-stance aan. In plaats van een statisch schild, schiet de speler reflexmatig binnenkomende vijandelijke projectielen uit de lucht!
   - **1 Revolver:** Schiet projectielen vóór je kapot en verhoogt loopsnelheid met 20%.
   - **2 Revolvers:** Creëert een defensief kogelgordijn rondom dat 60% schade reduceert en rondslingerende ketsers (ricochets) veroorzaakt naar nabije aanvallers.

---

## Samenvattend Overzicht

| Wapen | Type & Draagwijze | Linksklik Tap | Linksklik Hold (2s) | Rechtsklik Tap | Rechtsklik Hold (2s) |
|---|---|---|---|---|---|
| **RPG** | Zwaar 2-handig | Heavy Rocket (Explosie) | Cluster Barrage (Multi-lock) | Blast Jump (Mobiliteit/Knockback) | Flak Interceptor (Projectielafweer) |
| **Arm Cannon** | Zwaar 1-handig | Kinetic Cannon (Doordringend) | Mega Buster (Enorme golf) | Ground Pound (Schokgolf-stoot) | Aegis Barrier (Armschild/Reflectie) |
| **Minigun** | Zwaar 2-handig | Spin & Burst (8 kogels) | Lead Storm (Continu spervuur) | Shredder Sweep (120° waaier) | Gyro Deflector (Vaste basis/Kogelafweer) |
| **Sawed-off Shotgun** | Midden 2-handig | Buckshot Spread (Kogelkegel) | Double-Barrel (Dubbel schot/Blast) | Stock Strike (Kolf-slag/Interrupt) | Breacher Stance (Dekking & Slug) |
| **Revolvers** | 1-handig of 2-handig (Akimbo) | Fan / Quick Draw (Snel/Licht) | Deadeye Execution (Zwaar/Traag) | Akimbo Toggle + Trick Spin | Gunslinger Drift (Parry/Kogelgordijn) |