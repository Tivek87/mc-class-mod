# Engine-rework: alles uitgelegd

*24 september 2026 · commit `261fccc` · Multiverse Powers*

Dit document legt uit wat er aan de engine van de mod is veranderd en waarom. Ook: wat het doet met snelheid,
characters, abilities en mogelijkheden, en hoe alles getest is. Een technisch woord staat de eerste keer uitgelegd
tussen haakjes, en alle woorden staan nog eens in de woordenlijst onderaan (hoofdstuk 15).

---

## In één minuut

- **Voor de speler is niets veranderd.** Alles speelt en ziet er precies hetzelfde uit. Dat is gecontroleerd: de
  tekenmotor maakt bit voor bit dezelfde beelden, en dezelfde test met 64 screenshots gaf op de oude en de nieuwe
  code hetzelfde beeld.
- **De code is opnieuw ingedeeld.** Alles zit nu in mappen per onderdeel: `engine/` (gereedschap voor iedereen),
  `character/` (met een map per character), `spell/`, `classes/` enzovoort. Het grootste bestand (4408 regels) is
  opgesplitst in 11 bestanden.
- **De engine is losgemaakt van Green Lantern.** De tekenmotor kent geen vast groen meer: kleuren komen uit een
  `Material`. Green Lanterns eigen vormen (vuist, schild, koepel, straal...) zitten in zijn eigen `LanternPainter`.
- **Eén systeem** voor alles wat blijft lopen, één voor cooldowns en één plek voor opruimen. Characters en spreuken
  klikken erin via een vast koppelstuk, zonder lijstjes "als dit character, doe dat".
- **Snelheid, eerlijk gemeten:** de rekentijd van het tekenen is gelijk gebleven (±1%). Het afvalgeheugen
  (tijdelijke objecten die Java later moet opruimen) is gehalveerd. Dat geeft minder kans op korte haperingen, maar
  het is geen belofte van meer fps.
- **Sneller bouwen:** een nieuw character raakt nu 2 plekken in plaats van 7; een nieuwe spreuk 1 plek in plaats
  van 2.

---

## Inhoud

1. Wat is "de engine"?
2. De nieuwe mapindeling
3. De tekenmotor (`ConstructPainter`) in detail
4. De engine aan de serverkant
5. Rekenhulp: `engine/math`
6. Performance: wat is er echt sneller?
7. Wat betekent dit voor de characters?
8. Wat betekent dit voor abilities?
9. Creativiteit en nieuwe mogelijkheden
10. Sneller bouwen
11. Veiligheid en compatibiliteit
12. Hoe is alles getest?
13. Wat is níet veranderd
14. Grenzen en volgende stappen
15. Woordenlijst
16. Bijlage: wat is waarheen verhuisd

---

## 1. Wat is "de engine"?

Een engine is het **gereedschap** waarmee je iets bouwt. De **content** is wat je ermee bouwt.

| | Engine (gereedschap) | Content (wat je ermee bouwt) |
|---|---|---|
| Tekenen | een vorm tekenen als dichte massa met gloeiende randen | de vuist van Green Lantern |
| Tijd | iets elke tick laten doorlopen tot het klaar is | de luchtaanval die 20 seconden vliegt |
| Wachten | bijhouden wanneer iets weer klaar is | "Giant Fist: 4 seconden cooldown" |

Een tick is één stapje van de spelklok; er gaan er 20 in een seconde.

### Hoe het was

De mod had al een goede "mini-engine", maar die lag verspreid en zat vast aan één character:

- De tekenmotor (`ConstructPainter`) en de vormenbouwer (`Mesh`) zaten in de map van Green Lantern
  (`client/character/lantern/`), tussen 40 andere Green Lantern-bestanden. Het groen zat er vast in, en ook de
  vuist, het schild, de koepel en de straal.
- De motor die effecten laat doorlopen zat in `SpellCasting`, dus in de spreuken-map, terwijl Green Lantern en
  Doctor Octopus hem net zo goed gebruikten.
- Het character-systeem (`Characters`) had lijstjes van het soort "als Doctor Octopus, doe dit; als Green Lantern,
  doe dat" (in Java heet dat een `switch`).
- Hetzelfde kleine rekensommetje (`smooth`, een vloeiende overgang van 0 naar 1) stond op 11 plekken los
  gekopieerd.

### Hoe het nu is

Alles wat voor meer dan één power nuttig is, staat in `engine/`. Die map noemt **nooit** een character, spreuk of
class bij naam. De content (Green Lantern, Doctor Octopus, spreuken, classes) staat in eigen mappen en gebruikt de
engine.

---

## 2. De nieuwe mapindeling

De package (de Java-mapnaam) is nu `nl.tivek.multiversepowers`; die was `nl.tivek.welcomescreen`. Het **mod-id
blijft `welcomescreen`**. Werelden, instellingen en toetsen hangen aan dat id, dus die blijven gewoon werken.

```
nl/tivek/multiversepowers/
├── MultiversePowers.java        start van de mod (server en spel); ruimt alles op als de server stopt
├── MultiversePowersClient.java  start van de mod op je eigen spel (instellingenscherm, pak-laag)
│
├── engine/                      gereedschap voor iedereen; noemt nooit een character
│   ├── effect/                  Effect, Effects: alles wat een tijdje doorloopt
│   ├── ability/                 Cooldowns: wanneer iets weer klaar is
│   ├── entity/                  HeldMobs: wezens die vastgehouden worden
│   ├── fx/                      ParticleFx: deeltjesvormen (lijnen, ringen, bollen, spiralen...)
│   ├── target/                  Targeting: waar iemand op mikt, wie je mag raken
│   ├── math/                    Ease, Noise, Colors, Vectors: rekenhulp
│   └── client/
│       ├── render/              ConstructPainter, Mesh, Material: de tekenmotor
│       └── gui/                 GuiShapes, DirtBackgroundScreen: onderdelen voor schermen
│
├── character/                   het character-systeem: GameCharacter, Characters, CharacterPowers...
│   ├── client/                  toetsen, het paneel rechtsonder, het power-wiel
│   ├── docock/                  Doctor Octopus (+ client/)
│   └── greenlantern/            de ring en wat hij over het netwerk stuurt
│       ├── ability/             zijn 17 abilities (server)
│       └── client/              wat je eigen spel bijhoudt: ring, pak, constructs, vliegen
│           ├── render/          LanternPainter en alles wat zijn licht tekent
│           ├── slam/            de 32 slam-constructs
│           ├── body/            pak, ring aan de vinger, houdingen, armen, benen
│           └── hud/             construct-wiel, balk boven de hotbar, keuzescherm
│
├── spell/        spreuken (+ client/)
├── classes/      classes, welkomstscherm, keuze, uitrusting (+ ceremony/, client/)
├── stamina/      de stamina-balk (+ client/)
├── config/       de instellingenbestanden (+ client/: het instellingenscherm)
├── network/      ModNetwork: de ene plek waar elk bericht wordt aangemeld
├── registry/     wat de mod aan het spel toevoegt (het mob-effect, de items)
└── mixin/        kleine haakjes in Minecrafts eigen code
```

Twee regels bepalen de indeling:

- **Per onderdeel, niet per soort bestand.** Alles van Green Lantern staat bij Green Lantern. Een netwerkbericht (een
  payload: een klein pakketje gegevens tussen server en spel) staat bij het onderdeel dat hem verstuurt. Er zijn er
  20; vroeger stonden ze alle 20 in één map.
- **Code die alleen op je eigen spel draait, staat altijd in een map `client`.** Een server zonder scherm (een
  dedicated server) laadt die nooit; anders zou hij crashen.

De hoofdmap van het project is ook opgeruimd: de skin en de twee voorbeeldvideo's staan nu in `docs/reference/`.

### Cijfers

| | Voor | Na |
|---|---|---|
| Java-bestanden | 160 | 180 |
| Regels code | 44.193 | 45.003 |
| Grootste bestand | `ClassEffects`, 4408 regels | `OctoRig` (Doctor Octopus), 2333 regels, onveranderd |
| Green Lantern op je eigen spel | 42 bestanden in één map | 41 bestanden in 5 mappen, de engine eruit |
| Tekenmotor | 1929 regels, met Green Lantern erin | engine 1445 regels + `LanternPainter` 667 regels |

Er zijn 21 bestanden bijgekomen (de nieuwe engine-klassen en de gesplitste ceremonies) en 1 ongebruikt bestand is
weg (`DisplayHelper`). Het aantal regels steeg een beetje door uitleg in de code en de nieuwe klassen; tegelijk zijn
er kopieën weggehaald.

---

## 3. De tekenmotor (`ConstructPainter`) in detail

### 3.1 Hoe tekent hij?

Elk frame (tot 60 keer per seconde) verzamelt de painter de hoekpunten van alles wat hij tekent in **drie lagen**.
Een hoekpunt is één punt in de ruimte, met een kleur en een doorzichtigheid.

| Laag | Wat erin komt | Hoe het wordt getekend |
|---|---|---|
| **Massa** | de dichte vlakken van een construct | verbergt wat erachter zit: je kunt er "op kloppen" |
| **Licht** | heldere lijnen: randen, stralen, vonken | doorzichtig; verbergt niets en verbergt elkaar niet |
| **Gloed** | de zachte waas eromheen | opgeteld bij wat erachter zit, zoals echt licht; valt het meest op in het donker |

Aan het eind van het frame stuurt `finish()` elke laag in één keer naar de videokaart. Dat zijn drie draw calls per
painter (een draw call is één tekenopdracht aan de videokaart). Wat je in first person in je eigen handen houdt,
krijgt een eigen licht- en gloedlaag. Het licht van de wereld is dan namelijk al op het scherm gezet.

### 3.2 Wat hij al kon (bestond al, niet veranderd)

De oude painter was al slim gebouwd. Dit is gebleven:

- **Buiten beeld overslaan** (frustum culling): om elke vorm ligt een onzichtbare bol. Valt die bol helemaal buiten
  beeld, dan wordt de vorm niet uitgerekend.
- **Ver weg eenvoudiger**: is de straal van een vorm kleiner dan 1,2% van zijn afstand (maar een paar pixels), dan
  wordt zijn zachte gloed weggelaten.
- **Dichtbij vervagen**: wat binnen 0,2 tot 0,75 blok van de camera komt, vervaagt. Zo vult een construct die langs
  je ogen zwaait nooit je hele scherm.
- **Een geheugen per model**: voor een model van blokjes wordt één keer uitgerekend welke randen tegen een ander
  blokje liggen. Dat gebeurt vanaf 4 blokjes; het wordt vergeten zodra het model zelf weg is.
- **Buffers hergebruiken**: de geheugenruimte voor hoekpunten begint bij 1024 punten, verdubbelt als het vol is, en
  gaat van frame naar frame mee in plaats van steeds opnieuw aangemaakt te worden.
- **`Mesh`** voor ronde en schuine vormen (draaivorm, torus, buis, vleugel, loft...). Hun richtingen en randen
  worden één keer uitgerekend.

### 3.3 Wat er nieuw is

**a) Engine en Green Lantern uit elkaar.**
De engine-painter kent alleen nog algemene technieken. Alles wat alleen Green Lantern heeft, zit in `LanternPainter`
(een subklasse: een uitbreiding die alles van de engine erft en er eigen dingen bij doet):

| In `LanternPainter` (alleen Green Lantern) | Wat het is |
|---|---|
| `fist` | de vuist: 28 blokjes plus de ring om de middelvinger |
| `bolt` | het lichtkogeltje van de linkermuisknop |
| `shield` | het ronde schild, met een ring klinknagels die langzaam draait |
| `dome` | de koepel om hem heen |
| `ram` | de ramkegel waarmee hij vliegend wezens wegramt |
| `beam` | de lichtdraad van de ring naar elke construct |
| `beamOfLight` | de grote lichtstraal (Light Beam, en de pilaar van de luchtaanval) |
| specks | de lichtspikkels die op een vuist landen terwijl hij oplaadt |
| `HARD_LIGHT` | zijn kleuren (zie hieronder) |

**b) `Material`: kleuren los van de techniek.**
Een material is een setje van vier kleuren. Elke tekenfunctie van de engine gebruikt nu de kleuren van het material
van de painter, in plaats van vast groen.

| Kleur | Waarvoor | Green Lantern (`HARD_LIGHT`) |
|---|---|---|
| `mass` | de dichte massa (de painter maakt de kant van het licht af donkerder) | `#4BEF78` |
| `edge` | de heldere randen, en de stralen van een flare | `#6CFF8E` |
| `glow` | de zachte gloed | `#3CE86A` |
| `hot` | het witte hart van vers licht, en een construct die oplicht als hij inslaat | `#E4FFEA` |

Je kunt zelfs midden in een frame van material wisselen (`painter.material(...)`). Zo kan één painter constructs
van verschillende powers naast elkaar tekenen.

Zo zou een andere power eruit kunnen zien. **Dit zijn verzonnen voorbeelden; ze zitten niet in de mod:**

| Voorbeeld | mass | edge | glow | hot |
|---|---|---|---|---|
| rood-goud | `#C8342A` | `#FFD27A` | `#FF7A3C` | `#FFF4DC` |
| blauw-wit (bliksem) | `#3C7CFF` | `#BFE6FF` | `#4FA8FF` | `#F2FAFF` |
| paars (kosmisch) | `#6A3CD8` | `#C9A8FF` | `#8A5CFF` | `#F4EEFF` |

**c) Open bouwstenen.**
Eerst waren de lagen en de lijn- en vlaktekenaars privé: alleen de painter zelf kon ze gebruiken. Een nieuw soort
effect betekende dus dat je het engine-bestand zelf moest openbreken. Nu zijn dit de openbare bouwstenen:

| Bouwsteen | Wat het tekent |
|---|---|
| `model` | een vorm van blokjes (zoals de vuist) |
| `mesh`, `shape` | ronde en schuine vormen, of blokjes en ronde delen samen |
| `shattered` | een vorm die in dichte stukken uit elkaar vliegt (nooit wegfaden) |
| `seeThrough` | een doorzichtige versie, voor een construct vlak voor je eigen ogen |
| `chunk` | een tuimelend blok (brokken die opvliegen bij een klap) |
| `side`, `massQuad` | een dicht vlak met vier hoeken **(nieuw open)** |
| `lightLine`, `glowLine`, `lightQuad` | een lichtlijn, een gloedlijn, een vlak van zwak licht **(nieuw)** |
| `edge`, `sheet` | een heldere rand met gloed; een lichtveeg zoals een zwaard achterlaat |
| `flare`, `circle`, `band` | een lichtvonk met stralen; een ring van licht; een platte ring om iets heen |
| `trail`, `exhaust`, `chain`, `haze` | een lichtstreep achter een vlieger; een straalvlam; een ketting; een gloeiende nevel |
| `glare`, `fling`, `ambient` | lichter opgloeien bij inslag; hoe ver stukken wegvliegen; licht van binnenuit |

`seeThrough`, `chain` en `band` bestonden al, maar alleen de painter zelf kon ze gebruiken; ook die zijn nu open.
Voor painter-uitbreidingen zoals `LanternPainter` zijn er nog twee extra's: `chargedModel` (een blokjesmodel met de
golvende gloed van opladen) en `nearFade` (dichtbij laten vervagen).

**d) Handen in first person.** `ConstructPainter.hand(...)` krijgt nu een material mee; `LanternPainter.hand(...)`
geeft meteen Green Lanterns kleuren.

### 3.4 Wat er sneller is gemaakt in de painter

Alle drie de verbeteringen laten het beeld **bit voor bit gelijk** (zie hoofdstuk 6 en 12).

| Onderdeel | Voor | Na | Wat het doet |
|---|---|---|---|
| `Frame.at` | 6 nieuwe objecten per punt | 1 | rekent uit waar een punt van een model in de wereld ligt |
| `box` | meer dan 100 tijdelijke objecten per blokje | 0 | tekent één blokje: 6 zijden en de zichtbare randen |
| `draw` | 1 nieuw object per hoekpunt | 0 | stuurt de hoekpunten naar de videokaart |

Hoe blijft het beeld exact gelijk? Een computer rekent met kommagetallen die afronden. `a + b + c` in een andere
volgorde kan daardoor een piepklein ander getal geven. Daarom is elke som in precies dezelfde volgorde overgenomen
als Minecrafts eigen `Vec3` hem deed. Ook de grens waarbij `Vec3` een richting "te kort" vindt (0,0001) is exact
nagemaakt. Een checksum (een controlegetal over alle hoekpunten) bewijst dat het klopt.

---

## 4. De engine aan de serverkant

### 4.1 `Effects`: alles wat een tijdje doorloopt

Een **effect** is iets dat een tijdje doorloopt: de server tikt het 20 keer per seconde aan, tot het zegt "ik ben
klaar". Voorbeelden: de vliegende Giant Fist, het vliegtuig van de luchtaanval, de Light Bubble, de tentakels van
Doctor Octopus, een vuurbal, de gifwolk. Er zijn 16 klassen die een effect zijn, en 23 bestanden starten effecten.

De regels van `Effects`:

- Een nieuw effect begint op de **volgende** tick. Een effect mag zelf nieuwe effecten starten (het gifflesje wordt
  een gifwolk).
- Effecten lopen in de volgorde waarin ze gestart zijn.
- Een effect hoort bij de wereld (dimensie) waarin het begon. Is die wereld weg, dan stopt het effect.

Wat is **nieuw**:

- **Eigen plek**: de motor zat in `SpellCasting` (spreuken) en is nu `engine/effect/Effects`, voor iedereen.
- **Een fout breekt niet alles meer af**: in de echte mod (de jar) wordt een effect dat een fout maakt alleen zelf
  gestopt. De fout komt in het log en de server draait door. Vroeger ging dan de hele server (of je singleplayer-spel)
  plat. Tijdens het ontwikkelen crasht het nog wel, zodat je een fout nooit mist.
- **Efficiënter opruimen**: klaar zijnde effecten gaan in één ronde uit de lijst. Vroeger schoof de lijst bij elk
  weggehaald effect op; met veel tegelijk lopende effecten werd dat kwadratisch duurder. Met een paar tientallen
  effecten tegelijk merk je dat niet, met honderden wel.

### 4.2 `Cooldowns`: wanneer is iets weer klaar?

Eén klasse houdt per speler bij op welke servertick iets weer klaar is. Voor characters: 11 plekken per character
(de 11 abilitytoetsen). Voor spreuken: 1 per spreuk. Vroeger hadden characters en spreuken elk hun eigen tabel en
eigen code. De uitkomst is hetzelfde gebleven: "nog over" is `klaar-op` min `nu`, nooit onder nul.

| Functie | Wat het doet |
|---|---|
| `left` | hoeveel ticks er nog over zijn |
| `start` | start een cooldown vanaf nu |
| `forget` | alles van deze speler meteen klaar (bij doodgaan of uitloggen, voor characters) |
| `clear` | alles van iedereen weg (de server stopt) |

### 4.3 `CharacterPowers`: de stekker van een character

Een character klikt nu in het systeem via één koppelstuk, `CharacterPowers` (in Java een interface: een lijstje
vragen dat een klasse moet kunnen beantwoorden).

| Vraag | Verplicht? | Wat het character antwoordt |
|---|---|---|
| `enter` | ja | je wordt dit character: haal tevoorschijn wat erbij hoort |
| `leave` | ja | je stopt ermee: berg alles weer op |
| `use` | ja | een ability gebruiken; "ja" betekent dat de cooldown moet starten |
| `clear` | ja | de server stopt: vergeet alles |
| `ultimateLeft` | nee | hoeveel ticks de ultimate nog loopt (het paneel telt af) |
| `stance` | nee | hoe hij staat of beweegt (Doctor Octopus: op hoeveel tentakels) |
| `marks` | nee | hoeveel wezens hij heeft gemarkeerd (Doctor Octopus: voor Ground Strike) |
| `showTo` | nee | iemand ziet hem voor het eerst: vertel wat die moet weten om hem te tekenen |

`DocOckPowers` en `GreenLanternPowers` beantwoorden die vragen. `Characters` vraagt het nu gewoon aan
`character.powers()` en kent zelf geen enkel character meer bij naam. Voor een character zonder eigen entree is er
`Characters.transformFlash(...)`: een flits van deeltjes met een geluidje. Doctor Octopus gebruikt die, net als
vroeger, met toonhoogte 0,9.

Zo loopt één toetsdruk nu door het systeem:

```
jouw spel: je drukt R (ClientCharacter)
   ↓  AbilityActionPayload
server: ModNetwork → Characters.action
   ↓  cooldown nog bezig?  →  Cooldowns.left
   ↓  character.powers().use(...)            ← de stekker
Green Lantern: PowerRing.use → GiantFist.launch
   ↓  Effects.start(level, vuist)            ← de effect-motor tikt hem 20× per seconde
   ↓  elke tick: ConstructPayload naar iedereen in de buurt
jouw spel: ClientConstructs → LanternPainter.fist(...)   ← de tekenmotor, in Green Lanterns material
```

### 4.4 Spreuken klikken ook in

Elke regel in `Spell` draagt nu zijn eigen "cast"-code mee, bijvoorbeeld `FIREBALL(..., FireballSpell::cast)`.
`SpellCasting` checkt alleen nog de cooldown en roept die code aan. Vroeger stond daar weer een lijstje per spreuk.

De welkomstregel "[Magic] Feel free to send me whatever you want!..." stond als vaste tekst in de code. Die staat nu
in `en_us.json`, volgens de regel dat alle tekst daar hoort. Hij ziet er in het spel precies hetzelfde uit: "[Magic]"
in lichtblauw, de rest geel.

### 4.5 Vasthouden en mikken: `HeldMobs` en `Targeting`

- `HeldMobs` houdt wezens vast met hun eigen AI uit, zodat een power ze helemaal bestuurt. Is een wezen opgeslagen
  terwijl het vastgehouden werd (chunk ontladen, crash), dan krijgt het bij het laden zijn AI terug. Dat stukje zat
  in `SpellCasting` en zit nu in `HeldMobs` zelf.
- **Nieuw: grepen aanmelden.** Een power die op zijn eigen manier vasthoudt, meldt dat aan met `addHolder`. De
  klauwen van Doctor Octopus houden bijvoorbeeld ook spelers vast. `isHeldByAnyone` kent zo elke greep in de mod.
- `Targeting` (was `SpellTargeting`) bepaalt waar iemand op mikt en wie je mag raken of grijpen: geen harnasstandaard,
  geen boss, niet iets dat al vastgehouden wordt, en spelers alleen als PvP aan staat. Vroeger vroeg dat rechtstreeks
  aan Doctor Octopus of iets vastgehouden werd; nu vraagt het `HeldMobs`, zonder een character bij naam te noemen.

### 4.6 `ParticleFx`: deeltjesvormen

Was `SpellFx`. Het tekent deeltjes (die de server naar iedereen stuurt) in vormen: lijn, ring, schokgolf, bol,
zigzag, schijf, ster, spiraal, magische cirkel en implosie. Spreuken, Green Lantern en Doctor Octopus gebruiken het
allemaal; daarom staat het nu in de engine.

### 4.7 Opruimen als de server stopt: één lijst

Alles wat de mod per server onthoudt, wordt nu op één plek vergeten: `MultiversePowers.onServerStopping`. Dat
gebeurt in deze volgorde, en altijd voordat de wereld wordt opgeslagen:

1. `Effects.clear`: alle lopende effecten weg
2. `Ceremonies.clear`: lopende class-ceremonies weg
3. `SpellCasting.clear`: cooldowns van spreuken, en niemand loopt nog in de leegte (Void Walk)
4. `Characters.clear`: elk character vergeet alles (`powers().clear()`), plus wie wie is en de cooldowns
5. `HeldMobs.releaseAll`: als laatste, omdat een wezen nooit zonder AI mag worden opgeslagen

Vroeger stond dit in `SpellCasting`: de spreuken ruimden daar Green Lantern en Doctor Octopus op. Daarnaast had
`ClassEffects` nog een eigen opruimer.

---

## 5. Rekenhulp: `engine/math`

Vier kleine gereedschapskistjes met rekensommen:

| Klasse | Wat erin zit |
|---|---|
| `Ease` | overgangscurves: `smooth` (zacht beginnen en eindigen), `smoother` (nog zachter), `backOut` (schiet iets door en veert terug) |
| `Noise` | getallen die toevallig lijken maar altijd hetzelfde zijn voor dezelfde invoer: `of`, `direction`. Een vonk houdt zo zijn plek zonder dat iets onthouden of verstuurd wordt |
| `Colors` | `alpha` (hoe zichtbaar, 0–255), `shade` (donkerder of lichter), `mix` (tussen twee kleuren) |
| `Vectors` | `UP` (recht omhoog), `spin` (een richting om een as draaien), `across` (twee richtingen haaks erop) |

Weggehaald: **13 losse kopieën in 11 bestanden** (onder andere 10× `smooth`, `noise`, `mix`), plus 8 hulpjes die in
de painter zaten. De formules zijn precies overgenomen. Een paar lijkende hulpjes zijn bewust **niet** samengevoegd,
omdat ze net iets anders rekenen:

- de `backOut` van de slam-constructs (een andere curve);
- de `ease` van de tentakels (zonder grens bij 0 en 1);
- de `basis` van de deeltjes (een andere draairichting).

Samenvoegen zou die bewegingen merkbaar veranderen.

Bestanden die in "kleine kommagetallen" (float) rekenden, doen dat nog steeds. Daar kan het resultaat hooguit één
laatste afrondingsstapje verschillen, onzichtbaar klein. Bestanden die in "grote kommagetallen" (double) rekenden,
zijn exact gelijk.

---

## 6. Performance: wat is er echt sneller?

### 6.1 De meting

De meting draaide **in het spel zelf**, want Minecrafts tekencode start niet buiten het spel. De oude painter (uit
git) en de nieuwe tekenden daar naast elkaar dezelfde drukke scène: 40 vuisten (de helft aan het opladen), 20
lichtkogels, 6 schilden, 2 koepels (één van binnen gezien), een ramkegel, 10 flares, 10 straalvlammen en een grote
lichtstraal. Samen zijn dat **300.600 hoekpunten**. Er waren twee aparte runs.

| Meting | Oud | Nieuw | Verschil |
|---|---|---|---|
| Hoekpunten massa / licht / gloed | 77.456 / 130.768 / 92.376 | precies gelijk | 0 |
| Checksum (controlegetal over alles) | `d1017092` | `d1017092` | gelijk |
| Rekentijd per scène, run 1 (mediaan) | 1905,5 µs | 1919,7 µs | +0,7% |
| Rekentijd per scène, run 2 (mediaan) | 1883,9 µs | 1899,5 µs | +0,8% |
| Afvalgeheugen per scène, run 1 | 736.304 bytes | 364.408 bytes | **−50,5%** |
| Afvalgeheugen per scène, run 2 | 708.304 bytes | 364.408 bytes | **−48,6%** |

Eén µs (microseconde) is een duizendste milliseconde. Bij 60 fps heeft één frame 16,7 ms. Deze scène vraagt dus zo'n
1,9 ms van dat frame.

### 6.2 Wat betekent dat?

- **Rekentijd: gelijk.** Het nieuwe systeem was in beide runs ~0,8% langzamer. Dat is heel klein, maar ik zeg het
  eerlijk: **het tekenen is niet sneller geworden.**
- **Afvalgeheugen: gehalveerd.** Java maakt voor veel tijdelijke rekenstapjes een object. De garbage collector (de
  "vuilnisman" van Java) moet die later opruimen, en soms geeft dat een mini-hapering in het beeld. Wie deze scène 60
  keer per seconde tekent, maakte eerst ongeveer 42–44 MB afval per seconde en nu ongeveer 22 MB.
- **Niet gemeten, wel verwacht:** het wegvallen van één object per hoekpunt bij het doorsturen naar de videokaart
  (`draw`). Dat zit niet in deze meting. Ook de fps in normaal spelen en de lengte van de haperingen zijn niet
  gemeten.

### 6.3 Waarom niet sneller?

Java heeft een slimme vertaler die tijdens het draaien de code optimaliseert (de JIT). Die ruimde een deel van de
tijdelijke objecten al zelf op. Het echte werk blijft: wortels trekken, sinussen, hoekpunten in de buffers zetten.
Voor echte extra snelheid zijn grotere ingrepen nodig; zie hoofdstuk 14.

### 6.4 Over "47,8% sneller" en "7,3× krachtiger"

Die getallen uit de chat waren schattingen, geen metingen. De getallen hierboven zijn wel gemeten, met de oude en de
nieuwe code in hetzelfde spel.

---

## 7. Wat betekent dit voor de characters?

### 7.1 Green Lantern en Doctor Octopus

Voor de speler verandert **niets**: dezelfde abilities, getallen, beelden en toetsen. In de code:

**Green Lantern**
- De kern van de ring staat in `greenlantern/`: `PowerRing`, `Construct`, `ConstructPath`, `PlanePath`, `Arrival`,
  zijn 4 payloads en `GreenLanternPowers`.
- Zijn 17 abilities staan in `greenlantern/ability/`. `Lantern` heet nu `Recharge`. Dat past bij de ability "recharge",
  en "Lantern" was verwarrend naast `LanternArms` en `LanternPose`, waar "Lantern" juist Green Lantern zelf betekent.
- Zijn client-kant is verdeeld over `render/` (tekenen), `slam/` (32 slam-constructs), `body/` (pak, ring, houdingen)
  en `hud/` (wiel en balk). Het commando `/constructshockwave` staat nu bij Green Lantern; het stond bij de classes.

**Doctor Octopus**
- `docock/` en `docock/client/`. `RobotArm` (die zijn armen naar iedereen stuurt) stond nog bij de spreuken,
  omdat de armen ooit als spreuk begonnen; hij staat nu bij Doctor Octopus.
- Zijn grijp- en gooi-berichten staan bij hem. Zijn klauwen zijn aangemeld als "greep" bij `HeldMobs`.
- Zijn tentakels tekent hij met een eigen tekenaar (`ArmPainter`), niet met de engine-painter. Dat was al zo en is zo
  gebleven.

### 7.2 Een nieuw character toevoegen: voor en na

| Voor | Na |
|---|---|
| 1. regel in `GameCharacter` met de abilities | 1. regel in `GameCharacter` met de abilities en `new XPowers()` |
| 2. `Characters.enter`: stukje in de lijst | 2. één klasse `XPowers` die de vragen van hoofdstuk 4.3 beantwoordt |
| 3. `Characters.leave`: stukje in de lijst | |
| 4. `Characters.action`: stukje in de lijst | |
| 5. `Characters.sync`: ultimate, benen en markeringen erbij | |
| 6. `Characters.onStartTracking`: soms een uitzondering | |
| 7. `SpellCasting`: opruimen bij server-stop | |
| **7 plekken in 3 bestanden** | **2 plekken** |

De Java-compiler helpt nu ook. Vergeet je `enter`, `leave`, `use` of `clear`, dan bouwt de mod niet. Vroeger bouwde
hij bij een vergeten stukje in `enter`, `leave` of `sync` gewoon, en deed het nieuwe character daar dan stilletjes
niets.

Het wiel, de toetsen, de cooldowns, het paneel en het instellingenscherm werken nog steeds vanzelf vanuit
`GameCharacter`. Dat kon al; het is een sterk, datagestuurd systeem (instellingen, eenheden, oude standaardwaarden).

**Nog niet algemeen:** op je eigen spel noemt `ClientCharacter` (toetsen en paneel) Green Lantern en Doctor Octopus
nog bij naam. Denk aan: de ringkracht-check, de muis met het zwaard, het construct-wiel, klimmen, de tentakel-benen en
de onderste regel van het paneel. Een nieuw character met een gewoon paneel heeft daar niets nodig. Eén met eigen
invoer of een eigen paneel wel. Zie hoofdstuk 14.

---

## 8. Wat betekent dit voor abilities?

Zo bouw je nu een ability, van begin tot eind. Bij elke stap staat wat al kon en wat nieuw is.

| Stap | Hoe | Bestond al / nieuw |
|---|---|---|
| 1. Beschrijven | in `GameCharacter`: toets, cooldown, schade, eigen instellingen met eenheid en uitleg (komen vanzelf in het instellingenscherm en in het `.toml`-bestand) | bestond al |
| 2. Gebruiken | in `use` van het character (via `CharacterPowers`) | **nieuw** koppelstuk |
| 3. Laten doorlopen | alles wat langer duurt dan één klik is een `Effect`, gestart met `Effects.start` | motor bestond, **nieuw**: eigen plek en foutbestendig |
| 4. Cooldown | start vanzelf als `use` "ja" zegt; of later met `Characters.startCooldown` | bestond al, **nieuw**: via `Cooldowns` |
| 5. Laten zien | payload met compacte gegevens (plek, richting, grootte, leeftijd), dan tekenen met de engine-painter in het eigen material | payload-aanpak bestond, **nieuw**: `Material` en open bouwstenen |
| 6. Opruimen | effecten stoppen zichzelf; `clear` in de powers; server-stop gaat centraal | **nieuw**: één lijst |

---

## 9. Creativiteit en nieuwe mogelijkheden

Wat nu makkelijker of pas mogelijk is:

1. **Elke power in zijn eigen kleuren, met dezelfde techniek.** Een nieuw `Material` is genoeg om dezelfde dichte
   vormen met gloeiende randen, flares, lichtstrepen en vlammen in een andere kleur te tekenen.
2. **Kleuren mengen in één frame**, bijvoorbeeld een rode kopie van een construct naast een groene.
3. **Nieuwe vormen en effecten zonder de engine open te breken**, met de open bouwstenen (lijnen, vlakken, gloed,
   stukken die wegvliegen).
4. **Een eigen painter per character.** `LanternPainter` is het voorbeeld: eigen vormen erbij, de engine blijft
   schoon.
5. **Effecten als bouwstenen.** Een ability kan meerdere effecten starten (opladen, vliegen, inslaan) die elk op
   zichzelf doorlopen en zichzelf opruimen.
6. **Dezelfde spelregels voor iedereen**: cooldowns, wie je mag raken en wie al vastgehouden wordt, gelden voor elke
   power hetzelfde.
7. **Aansluiten met weinig werk**: een spreuk is één regel, een character één regel plus één klasse.

Eerlijke grens: de **vormen** van Green Lantern (vuist, schild, koepel, straal) zijn van hem. Een ander character kan
ze niet zomaar gebruiken; daarvoor moet eerst een algemene versie naar de engine (zie hoofdstuk 14). De
**technieken** eronder (blokjesmodellen, ronde vormen, randen, gloed, uit elkaar vallen, doorzichtig voor jezelf)
zijn wel voor iedereen.

---

## 10. Sneller bouwen

- **Minder plekken aanraken**: character 7 → 2, spreuk 2 → 1, opruimen bij server-stop 1 vaste plek.
- **Sneller vinden**: mappen per onderdeel; de naam van de map zegt wat erin zit.
- **Minder kopieën**: rekenhulp op één plek. Een verbetering daar werkt meteen overal.
- **Claude leert het mee**: `CLAUDE.md` bevat nu de indeling en de engine-regels. Bijvoorbeeld: "wat een tweede power
  kan gebruiken, hoort in `engine/`" en "een optimalisatie moet het beeld exact gelijk laten, gecontroleerd met een
  checksum". Zo gebruikt een volgende sessie de engine in plaats van weer een eigen kopie te bouwen.
- **Testen als vaste methode**: hoe je oude en nieuwe code naast elkaar test, staat nu ook in `CLAUDE.md`.

Een percentage "zoveel sneller bouwen" geef ik niet; dat valt niet eerlijk te meten.

---

## 11. Veiligheid en compatibiliteit

| Onderdeel | Wat er gebeurt |
|---|---|
| Werelden | werken gewoon: het mod-id `welcomescreen` is niet veranderd |
| Opgeslagen gegevens | dezelfde namen: ringkracht `welcomescreen:ring_power`, vastgehouden wezens `welcomescreen_held_noai`, je class |
| Instellingen | dezelfde bestanden en sleutels in `config/welcomescreen/` |
| Toetsen | dezelfde namen, dus je eigen toetsen blijven staan |
| Netwerk | dezelfde bytes per bericht, protocolversie "15" ongewijzigd; alleen de namen in de code zijn anders |
| Mixins en de extra armhouding | de verwijzingen wijzen naar de nieuwe mappen; de naam van de houding (`WELCOMESCREEN_LANTERN`) is gelijk |
| Dedicated server | getest: start zonder fouten, er wordt geen client-code geladen |
| Fouten | een effect met een fout stopt alleen zichzelf (in de echte jar) |

---

## 12. Hoe is alles getest?

1. **Bouwen**: `gradlew build` slaagt. De jar bevat 291 klassen, niets uit de oude package en geen testcode.
2. **Checksum in het spel**: oude en nieuwe painter tekenden dezelfde scène van 300.600 hoekpunten; elke plek en
   kleur is gelijk.
3. **Tijd en afval**: de tabel in hoofdstuk 6, twee runs.
4. **Oud tegen nieuw, in het echte spel**: dezelfde automatische test draaide op de originele code (uit git, in een
   aparte map, met jouw instellingen en toetsen gekopieerd) en op de nieuwe. Elke run maakte 64 screenshots:
   - de knight-ceremonie, de ring die aankomt, de vuist in first person, lichtkogels, schild, lichtstraal, koepel,
     scan, Light Bubble en het neerbeuken, Lantern Flare, opladen, slam, vliegen en de luchtaanval;
   - Doctor Octopus en zijn tentakel-benen;
   - vuurbal, bliksem, windstoot, gif en Void Walk.

   Uitkomst: **0 fouten** in beide runs, en de beelden zijn gelijk. Per paar is het verschil gemeten. Meestal is dat
   2–4 op een schaal van 255: ruis van wolken en getallen in het paneel. De uitschieters komen door toeval: de
   luchtaanval (rondvliegend puin), Void Walk en de bliksem (de vorm van een bliksemschicht is steeds anders). Ook
   kiest de slam elke keer willekeurig één van de 32 constructs. De Lantern Flare is frame voor frame naast elkaar
   gelegd en is gelijk.
5. **Dedicated server**: gestart zonder fouten. `run-server` is daarna exact teruggezet.
6. Jouw `run/options.txt`, configs en werelden zijn onaangeroerd. De testwereld, de screenshots en de testklasse
   zijn verwijderd.

**Niet getest:**
- multiplayer met twee echte spelers;
- de fps in normaal spelen;
- elk van de 32 slam-constructs apart (per run één willekeurige);
- de andere 24 ceremonies: hun code is letterlijk verplaatst en bouwt, maar alleen de knight is in beeld gezien;
- het construct-wiel met het zwaard;
- de andere abilities van Doctor Octopus (grijpen, portaal, rampage...);
- het instellingenscherm.

---

## 13. Wat is níet veranderd

- Alle gameplay: schade, cooldowns, bereik, kosten, tijden.
- Alle beelden, animaties, geluiden en teksten (de welkomstregel is alleen verhuisd, hij ziet er gelijk uit).
- De besturing en de invoercode op je eigen spel (`ClientCharacter`).
- De tekenaar van de tentakels van Doctor Octopus (`ArmPainter`).
- Het instellingensysteem en de instellingenbestanden.
- Er zijn geen gevonden bugs gerepareerd: ik vond er geen die een fix nodig hadden.

---

## 14. Grenzen en volgende stappen

Ideeën, **niet gedaan**. Engine-werk mag volgens de projectregels zonder vragen; alles wat verandert hoe een
character speelt, vraagt eerst jouw OK.

| Idee | Wat het oplevert |
|---|---|
| `ClientPowers`: een stekker voor je eigen spel, zoals `CharacterPowers` voor de server | `ClientCharacter` hoeft geen character meer bij naam te kennen |
| De tentakels van Doctor Octopus op de engine-painter | ook hij krijgt `Material`, buiten-beeld-overslaan en de bouwstenen |
| Algemene straal, schild en koepel in de engine | een ander character kan ze gebruiken in zijn eigen kleuren |
| Eén gedeelde painter per frame | minder draw calls: nu tekent elke painter 3 keer |
| Echte detailniveaus (LOD): simpeler vormen op afstand | minder rekenwerk bij grote constructs ver weg |
| Eerst meten tijdens normaal spelen (fps, frame-tijd) | alleen optimaliseren waar het echt telt |

---

## 15. Woordenlijst

| Woord | Uitleg |
|---|---|
| engine | het gereedschap waarmee de content gebouwd wordt |
| content | wat je bouwt: characters, abilities, spreuken, classes |
| package | een Java-map met code; bepaalt de volledige naam van een klasse |
| mod-id | de naam waarmee Minecraft de mod kent (`welcomescreen`) |
| klasse | één bouwstuk code, meestal één bestand |
| interface | een lijstje vragen dat een klasse moet kunnen beantwoorden (zoals `CharacterPowers`) |
| subklasse | een uitbreiding van een klasse die alles erft en er dingen bij doet (zoals `LanternPainter`) |
| enum | een vaste lijst keuzes in code (zoals `GameCharacter`, `Spell`) |
| `switch` | een lijstje "als dit, doe dat" in code |
| tick | één stapje van de spelklok; 20 per seconde |
| frame | één beeld op je scherm; bij 60 fps 60 per seconde |
| hoekpunt | één punt van een getekende vorm, met kleur en doorzichtigheid |
| laag | de painter verzamelt hoekpunten in drie lagen: massa, licht, gloed |
| draw call | één tekenopdracht aan de videokaart |
| frustum culling | overslaan wat buiten beeld valt |
| LOD (level of detail) | ver weg minder detail tekenen |
| material | de vier kleuren waarin een power getekend wordt |
| effect | iets dat een tijdje doorloopt en elke tick wordt aangetikt |
| cooldown | wachttijd voordat iets opnieuw kan |
| payload | een klein pakketje gegevens tussen server en spel |
| client | jouw eigen spel (met scherm) |
| dedicated server | een server zonder scherm, alleen voor multiplayer |
| mixin | een klein haakje in Minecrafts eigen code |
| garbage collector | Java's "vuilnisman", die ongebruikte objecten opruimt; kan korte haperingen geven |
| afvalgeheugen | geheugen van tijdelijke objecten dat later opgeruimd moet worden |
| JIT | het deel van Java dat code tijdens het draaien sneller maakt |
| checksum | een controlegetal over een hoop gegevens; gelijk getal = gelijke gegevens |
| µs (microseconde) | een duizendste milliseconde |
| refactor | code anders indelen zonder dat de werking verandert |

---

## 16. Bijlage: wat is waarheen verhuisd

Paden binnen `nl/tivek/...`; links het oude pad (onder `welcomescreen`), rechts het nieuwe (onder `multiversepowers`).

| Oud | Nieuw |
|---|---|
| `WelcomeScreenMod` | `MultiversePowers` |
| `client/WelcomeScreenClient` | `MultiversePowersClient` |
| `spell/SpellEffect` | `engine/effect/Effect` |
| de effect-lijst in `spell/SpellCasting` | `engine/effect/Effects` |
| `spell/SpellFx` | `engine/fx/ParticleFx` |
| `spell/SpellTargeting` | `engine/target/Targeting` |
| `spell/HeldMobs` | `engine/entity/HeldMobs` |
| `client/character/lantern/ConstructPainter` | `engine/client/render/ConstructPainter` + `character/greenlantern/client/render/LanternPainter` |
| `client/character/lantern/Mesh` | `engine/client/render/Mesh` |
| `client/GuiShapes`, `client/DirtBackgroundScreen` | `engine/client/gui/` |
| `character/lantern/*` | `character/greenlantern/` en `character/greenlantern/ability/` |
| `character/lantern/Lantern` | `character/greenlantern/ability/Recharge` |
| `client/character/lantern/*` | `character/greenlantern/client/` + `render/`, `slam/`, `body/`, `hud/` |
| `client/character/docock/*` | `character/docock/client/` |
| `spell/RobotArm` | `character/docock/RobotArm` |
| `config/CharacterConfig` | `character/CharacterConfig` |
| `config/StaminaConfig` | `stamina/StaminaConfig` |
| `server/ServerEvents` | `classes/ClassEvents` |
| `server/ClassEffects` | `classes/ceremony/Ceremonies` + 10 bestanden (per groep, `DeathAndLevelUp`, `Fx`, `Glyph`) |
| `client/ClientEvents` | `classes/client/ClientWelcome` |
| `init/ModItems`, `init/ModEffects`, `effect/VoidInstabilityEffect` | `registry/` |
| `network/*Payload` (20 stuks) | bij hun eigen onderdeel |
| `spell/DisplayHelper` | weg (werd nergens gebruikt) |

**Nieuw:** `CharacterPowers`, `DocOckPowers`, `GreenLanternPowers`, `Material`, `LanternPainter`, `Cooldowns`,
`Effects`, `Ease`, `Noise`, `Colors`, `Vectors`, en de 10 ceremonie-bestanden.
