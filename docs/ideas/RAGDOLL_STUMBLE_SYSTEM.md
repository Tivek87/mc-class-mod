# Active Ragdoll, Stumbling & Interactive Realism System (RDR2 / Euphoria / Bodycam Style)

Dit document beschrijft het volledige ontwerp voor een **Euphoria / Red Dead Redemption 2 / Bodycam-achtig Active Ragdoll & Stumble systeem** in Minecraft voor *Multiverse Powers*.

In plaats van dat een entity pas bij `0 HP` een slappe ragdoll wordt en bij levende hits alleen rood knippert met een simpele velocity-push, reageren levende entities bij **non-fatal damage** fysiek en zelfbehoudend met hun hele lichaam, zwaartekracht, balans en de blok-omgeving om hen heen.

---

## 1. Kernconcept & Filosofie

### Wat maakt een Active Ragdoll (Euphoria / RDR2 / Bodycam) anders?
Een gewone ragdoll is 100% slap (dood gewicht). Een **Active Ragdoll** combineert **fysieke krachten** (impact, zwaartekracht, momentum, botsingen met blokken) met **spierkracht en zelfbehoud-instincten** (Inverse Kinematics / IK, voeten verzetten, armen uitsteken, wonden vastgrijpen en randen pakken):

1. **Non-Fatal Balance & Voetplaatsing (`FootPlanting` + `Limbs` + `Ik`):**
   - Zolang een entity leeft, proberen zijn benen altijd onder zijn verschoven **Center of Mass (CoM / zwaartepunt)** te stappen.
   - Wordt zijn torso door een klap naar achteren of opzij geduwd, dan glijden zijn voeten niet stijf over de grond, maar nemen zijn benen razendsnelle corrigerende struikelstappen achteruit of zijwaarts om overeind te blijven.
2. **Gelokaliseerde Impact & Pijnreacties (Hit-Zone Tracking):**
   - De server berekent bij elke hit exact **welk lichaamsdeel** is geraakt op basis van de inslaghoogte en hoek op de hitbox (`HEAD`, `CHEST`, `STOMACH`, `LEFT_ARM`, `RIGHT_ARM`, `LEFT_LEG`, `RIGHT_LEG`).
   - Het geraakte lichaamsdeel krijgt als eerste de fysieke impuls én triggert een specifieke spierreactie (zoals handen die naar een buikwond grijpen of een knie die wegknikt bij een beenschot).
3. **Omgevings-interactie (Grijpen, Steunen, Botsen & Hangen):**
   - Omdat Minecraft uit een blokgrid (`BlockState` / `VoxelShape`) bestaat, kan een struikelende of vallende entity razendsnel binnen armlengte (~1.5 blok) scannen naar muren, blokranden (ledges), hekken, boomstammen en takken.
   - Armen steken automatisch uit om een val tegen een muur op te vangen, of grijpen in paniek naar een klifrand of boomtak tijdens een val.
4. **Overgang tussen Balans → Struikelen → Vallen → Opkrabbelen:**
   - **Lichte/Medium hit:** Uit balans → corrigerende stappen + bovenlichaam deinst mee → herstelt balans.
   - **Zware non-fatal hit / Beenschot / Steile helling:** Raakt te ver buiten balans → struikelt echt over zijn eigen benen of een blokrand → valt als tijdelijke physics-ragdoll op de grond → krabbelt na ~1–2 seconden fysiek weer op zijn voeten.


---

## 1B. Uitgebreide Rigging: Extra Hand-, Voet-, Bekken- en Schouder-Bones (Alle Entities, Spelers & de Mech)

Om dit systeem er écht uit te laten zien als *RDR2* / *Euphoria* / *Bodycam*, kunnen we meteen **extra bones (gewrichten)** toevoegen bovenop de bestaande boven-/onderarm en boven-/onderbeen splitsing (`BentParts` / `Limbs`) voor **alle humanoïde entities, mobs, spelers én de Hard-Light Mech**:

1. **Hand Bones (Polsgewrichten / Wrists):**
   - **Waarom essentieel:** Als een entity zich opvangt tegen een muur, op handen en knieën valt, of aan een klifrand/boomtak hangt, kan de hand nu bij de **pols** tot $90^\circ$ buigen en vlak tegen het blok aanliggen in plaats van dat een stompe onderarm-kubus schuin in de muur prikt.
   - Ook bij het **vastgrijpen van een buik- of borstwond** vormen de handen zich vlak tegen het lichaam aan.
2. **Feet Bones (Enkelgewrichten / Ankles & Toes):**
   - **Waarom essentieel:** Bij achteruit strompelen, afzetten bij een sprong, of staan op een schuine helling/trap blijven de voetzolen via enkel-rotatie **vlak op de ondergrond geplant** (`FootPlanting`), terwijl het onderbeen erboven schuin kan kantelen.
   - Bij struikelen of wegglijden op ijs zie je de voet bij de enkel dubbelklappen of wegschieten.
3. **Pelvis / Bekken-Bone (Onafhankelijke Heup- & Onderrug-rotatie):**
   - **Waarom essentieel:** Splitst de stijve Minecraft-torso op in een **bovenborst (Chest)** en een **bekken/heupstuk (Pelvis)**.
   - Bij een Green Lantern mini-bolt in de buik kan de buik/onderrug nu écht naar achteren knikken (holle/bolle rug) terwijl het bekken meedraait met de stappende benen. Ook bij manken (*Wounded Walk*) of achteruit vallen zakt één kant van de heup realistisch omlaag.
4. **Shoulder / Clavicle Bones (Schoudergordel & Sleutelbeen-heffing):**
   - **Waarom essentieel:** Laat schouders onafhankelijk optrekken, naar voren rollen of naar achteren klappen voordat de arm zelf beweegt.
   - Wanneer iemand aan één of twee armen aan een klifrand hangt, zie je het lichaam echt **in zijn schouders hangen** (schouders hoog opgetrokken langs het hoofd). Bij een schot tegen de schouder klapt het hele schoudergewricht naar achteren.
5. **Universeel voor Mobs, Spelers én de Hard-Light Mech:**
   - **Alle Mobs & Spelers:** De procedurele mesh/model-splitter (`BentParts` + `Limbs`) kan deze extra gewrichtspunten automatisch op elk standaard Biped/Player/Zombie/Skeleton model toepassen zonder dat handmatige modellen per mob nodig zijn.
   - **De Hard-Light Mech (`MechWalk` / `MechDrive` / `MechPainter`):** De Mech heeft al vinger-rigs (`MechHandRig`) en been-IK; door ook pols-, enkel-, bekken- en schouder-rotaties aan het Mech-skelet te koppelen, kan de Mech bij zware treffers, landingen of schokgolven net zo zwaar en fysiek meeveren, schrap zetten met zijn voeten of met een hand steun zoeken tegen een gebouw/klifwand.


---

## 1C. Diepgaande Physics: Omgeving, Zwaartekracht, Gewicht & Instinctieve Reacties

### 1. Zwaartekracht (Gravity), Center of Mass (CoM) & Slinger-fysica (Pendulums)
- **Zwaartepunt vs. Steunvlak (Base of Support):**
  - Elke entity heeft een dynamisch berekend **Center of Mass (CoM)** dat afhangt van de positie van zijn torso, bekken, hoofd en ledematen.
  - Zolang de verticale projectie van het CoM (de zwaartelijn) binnen het vlak tussen de twee voeten (met `Feet Bones`) valt, staat de entity stabiel.
  - Duwt een aanval het CoM buiten de voeten, dan trekt de **zwaartekracht ($g$)** het bovenlichaam met een toenemend kantelmoment ($\tau = m \cdot g \cdot d$) naar beneden. Hoe schuiner de entity hangt, hoe sterker de zwaartekracht hem tegen de grond trekt.
- **Per-Limb Gravity & Pendulum Dynamics:**
  - Zwaartekracht trekt niet alleen aan de entity-hitbox als geheel, maar aan **elk afzonderlijk lichaamsdeel** (hoofd, bovenarm, onderarm, hand, bovenbeen, onderbeen, voet).
  - Buigt iemand voorover, dan hangen zijn armen door de zwaartekracht loodrecht omlaag richting de vloer. Hangt iemand met één hand aan een klifrand, dan werkt zijn hele lichaam onder die hand als een zware fysieke slinger (pendulum) die heen en weer zwaait.

### 2. Gewicht, Massa-verdeling & Traagheid (Mass & Inertia)
- **Massa per Lichaamsdeel (Segment Mass Ratio):**
  - Niet elk bot weegt evenveel: het **bekken + torso (~50% van het lichaamsgewicht)** en de **bovenbenen (~20%)** zijn zwaar en traag; het **hoofd (~8%)**, de **onderarmen + handen (~6%)** en **onderbenen + voeten (~16%)** zijn lichter en reageren veel sneller op klappen.
- **Gewichtsverschil per Entity & Character:**
  - **Lichte Entities (bijv. Skeletons, lichte mobs):** Lage massa → vliegen sneller achteruit bij een klap, stuiteren harder tegen muren en worden makkelijker van hun voeten geveegd.
  - **Standaard Humanoids (Spelers, Zombies, Villagers, Illagers):**Realistische menselijke massa (~75–85 kg gevoel) met gebalanceerde stap-snelheid en traagheid.
  - **Zware Entities (Iron Golems, Ravagers, Thor in volle wapenrusting, de Hard-Light Mech):** Enorme massa en traagheid → lichte aanvallen geven alleen een bovenlichaam-schok, terwijl zware aanvallen trage, zware dreunen veroorzaken waarbij de grond trilt en blok-stof omhoog spat bij elke herstelstap.

### 3. Omgevings-bewustzijn (Environment Geometry & Voxel Contact)
- **Volledige VoxelShape-interactie (`RigidBlocks`):**
  - Het systeem kijkt niet alleen of een heel blok $1\times1\times1$ vol of leeg is, maar leest de **exacte collision-vorm (`VoxelShape`)** van trappen (`StairBlock`), halve platen (`SlabBlock`), hekken (`FenceBlock`), muren (`WallBlock`) en deuren.
- **Hoeken, Plafonds & Smalle Ruimtes:**
  - Wordt een entity in een lage mijn-gang (2 blokken hoog) omhoog geslagen, dan botst zijn hoofd en bovenrug echt tegen het plafond aan en knikt zijn nek/torso terug omlaag.
  - Bij het vallen tegen een scherp blok of een aanbeeld/kist plooit het lichaam zich rond het obstakel in plaats van er half in te verdwijnen.

### 4. Instinctieve Zelfbehoud-Reacties (Euphoria Reflexes)
- **Windmill Reflex (Molenwieken bij Balansverlies):** Zodra een entity op het randje van een klif of bij een harde duw zijn balans dreigt te verliezen, draaien zijn armen (vanuit de `Shoulder Bones`) in cirkels om tegen-impuls te genereren en het evenwicht te redden.
- **Head Protection Reflex (Hoofd Beschermen):** Als een entity met hoge snelheid achterover of voorover richting een stenen muur of de grond valt, trekt hij instinctief zijn kin op de borst en brengt hij zijn onderarmen/handen voor zijn gezicht of achter zijn hoofd om de schedelklap op te vangen.
- **Look-At-Danger / Ground Tracking:** Tijdens het vallen draait het hoofd naar het punt waar hij gaat landen of naar de klifrand die hij probeert te grijpen.

---

## 2. Basis-Scenario's (Uitgewerkte Kernvoorbeelden)

### Scenario A: Green Lantern Mini-Bolt in de Buik (Wond Grijpen & Achteruit Strompelen)
- **Wat er gebeurt:** Je schiet een snelle Green Lantern mini-bolt midden in de buik (`STOMACH` zone) van een vijand.
- **Visuele & Fysieke Reactie:**
  1. **Impact-klap (Tick 0–3):** Het punt op de buik wordt naar achteren geslagen; de onderrug en torso vouwen in een holle knik naar achteren terwijl het hoofd en de schouders door traagheid eerst een fractie achterblijven en dan meeklappen.
  2. **Wond grijpen (Tick 3–18):** Door de pijnreactie klapt de torso daarna licht voorover (dubbelvouwen) en gaan **1 of 2 handen via IK (`Limbs` + `BentParts`) direct naar de exacte plek op de buik** waar de bolt insloeg om de wond vast te houden.
  3. **Balans-gevecht:** Door het achterwaartse momentum ligt het zwaartepunt achter zijn hielen. De entity zet 3 tot 4 snelle, wankele stappen achteruit om niet achterover te vallen. Als er een muur of blok achter hem staat, klapt zijn rug of schouder ertegenaan en duwt hij zichzelf met één vrije hand van de muur af om overeind te blijven.

### Scenario B: Schot in het Been/Voet bij een Klif of Kloof (Op z'n Bek Vallen & Klifrand / Boomtak Grijpen)
- **Wat er gebeurt:** Een entity staat vlak bij de rand van een diepe kloof (ravine) of klif. Je schiet met veel impact laag in zijn been of voet (`LEFT_LEG` / `RIGHT_LEG`).
- **Visuele & Fysieke Reactie:**
  1. **Been knikt weg:** Het geraakte onderbeen wordt onder hem vandaan geveegd en de knie verliest direct zijn draagkracht.
  2. **Faceplant richting de afgrond:** Doordat zijn steunbeen wegvalt, klapt zijn bovenlichaam door de zwaartekracht vol voorover op zijn bek richting de rand van de klif.
  3. **Klifrand (Ridge) grijpen:** Terwijl hij over de rand van de steile blokmuur schuift/valt, detecteert de Ledge-Scanner het bovenste randblok van de klif. Zijn beide armen schieten met IK omhoog en **grijpen de rand van de klif vast**.
  4. **Bungelen & Loslaten:** De entity hangt nu een paar seconden met zijn handen aan de rand; zijn benen bungelen in de lucht en trappelen tegen de steile klifmuur (`RigidBlocks` collision). Naarmate zijn grip-stamina leegraakt (of als hij nog een klap krijgt), glijden eerst één hand en daarna de tweede hand los en stort hij de diepte in.
  5. **Mid-Air Boomtak Grijpen:** Als er halverwege zijn val een boomstam of tak (`Logs` / `Leaves` / `Fences`) uit de klifwand steekt binnen armlengte, probeert hij die in zijn val met één of twee armen te grijpen—waardoor zijn val met een harde ruk wordt afgeremd (of de bladeren breken na een korte vertraging onder zijn gewicht door).

---

## 3. Vijf Nieuwe Scenario's Waar het Systeem Duidelijk Zichtbaar Is

### Scenario 1: Thor's Thunderclap / Wind Gust in een Kamer met Muren en Meubels (Wall-Catch & Brace)
- **Situatie:** Je gebruikt een schokgolf-ability (zoals Thor's *Thunderclap* of de *Wind Gust* spell) tegen een groepje vijanden in een stenen gang of huis.
- **Wat je ziet:**
  - De vijanden worden niet als stijve planken achteruit geschoven. Hun bovenlichamen worden achterover geblazen terwijl hun voeten wanhopig achteruit rennen/struikelen om bij te blijven.
  - Een vijand die schuin richting een muur wankelt, **steekt automatisch zijn dichtstbijzijnde hand en onderarm uit tegen de muur** (met gebogen elleboog als schokdemper) om zichzelf op te vangen en niet met zijn hoofd tegen het steen te klappen.
  - Een vijand die met zijn kuiten tegen een laag blok (zoals een trap, hek of kist) achteruit strompelt, **struikelt over het lage blok heen**: zijn voeten blijven haken achter de rand van het blok en zijn bovenlichaam kantelt achterover over het obstakel heen.

### Scenario 2: Schampschot / Inslag tegen de Schouder tijdens Volle Sprint (Spin-Out & Roll)
- **Situatie:** Een vijand of speler-NPC rent op volle snelheid op je af en je raakt hem hard in zijn **rechter schouder of borsthelft** (`RIGHT_ARM` / `CHEST` rechts).
- **Wat je ziet:**
  - Omdat hij al hoge voorwaartse snelheid had en nu aan één zijkant geremd wordt, ontstaat er een **draai-impuls (koppel/torque)** rond zijn verticale as.
  - Zijn rechterschouder klapt naar achteren, waardoor zijn lichaam een halve draai maakt terwijl zijn benen in de knoop raken met zijn eigen sprintsnelheid.
  - Hij verliest zijn voetenwerk, rolt/schuift met zijn schouder en rug over het gras (met `ParticleFx` stofpluimen waar zijn lichaam de grond raakt), en zet daarna één hand op de grond om zichzelf weer omhoog te duwen.

### Scenario 3: Gevecht op een Trap of Steile Berghelling (Downhill Tumble & Stair-Step Collisions)
- **Situatie:** Je vecht op een lange stenen trap of een steile bergwand en geeft iemand bovenaan een flinke duw of uppercut.
- **Wat je ziet:**
  - Bij zijn eerste stap achteruit stapt zijn hiel in het luchtgat van de lagere traptrede. Zijn knie buigt diep door, maar het hoogteverschil is te groot om zijn balans te redden.
  - Hij kantelt achterover en **rolt/stuitert trede voor trede van de trap af**, waarbij zijn rug, heupen en ledematen daadwerkelijk op elke traptrede botsen (`RigidBlocks`) in plaats van door de blokken heen te clippen.
  - Onderaan de trap blijft hij ~1 seconde versuft op zijn zij liggen, trekt zijn knieën op en staat weer op.

### Scenario 4: Zware Klap tegen het Hoofd / Kaak (Daze, Concussion Stumble & Hoofd Vasthouden)
- **Situatie:** Je raakt een entity met een zware ongewapende stoot, schildslag of hamer-tik tegen de zijkant van zijn hoofd (`HEAD` zone), net niet genoeg om hem te doden.
- **Wat je ziet:**
  - Zijn hoofd zwiept direct opzij in de richting van de klap, gevolgd door zijn nek en bovenste rugwervels.
  - Zijn evenwichtsgevoel is tijdelijk verstoord (**Concussion / Disorientation state**): in plaats van recht achteruit te stappen, zwalkt hij 1–2 seconden als een aangeslagen bokser schuin opzij met slappe knieën.
  - Eén hand grijpt naar zijn slaap/kaak terwijl de andere arm wijd uitsteekt om zijn evenwicht te zoeken. Als hij tijdens dit zwalken tegen een boom of paal botst, leunt hij daar even met zijn schouder tegenaan om bij te komen.

### Scenario 5: Explosie of Ground-Slam Vlakbij (Asymmetrische Blast Wave & Wegkruipen)
- **Situatie:** Een *Fireball* of *Lightning Ground Slam* slaat 2 blokken links van een entity in op de grond.
- **Wat je ziet:**
  - De drukgolf komt schuin van onderen en van links: zijn linkerbeen en linkerheup worden van de grond getild terwijl zijn rechterkant achterblijft.
  - Tijdens het neerkomen probeert hij zijn gezicht te beschermen door zijn armen voor zijn hoofd te kruisen.
  - Na de landing op de grond krabbelt hij niet in 1 frame recht overeind, maar steunt hij eerst op handen en knieën (terwijl hij nog even schudt/hoest van de klap) voordat hij weer op zijn voeten staat.

---

## 4. Tien Extra Ideas, Improvements & QOL voor het Stumble System

### 1. Dynamic Environment Prop Interaction (Bladeren Breken, Hekken & Ladders Grijpen)
- Niet elk blok waar een vallende entity naar grijpt is even sterk:
  - **Stenen/Houten randen, Ladders, Vines, Iron Bars & Fences:** Geven 100% stevige grip waar een entity echt aan kan blijven hangen.
  - **Leaves (Boombladeren), Cobwebs & Sweet Berry Bushes:** Als een vallende entity een bladerblok grijpt halverwege een val, houdt het blok hem ~0.3 seconden vast (remt zijn valsnelheid flink af met ritselende blad-particles), waarna het bladerblok **breekt** en hij met veel lagere snelheid verder valt.

### 2. Multi-Entity Domino & Crowd Collisions (Tegen Elkaar Aan Struikelen)
- Wanneer een hard geraakte entity achteruit strompelt of valt en tegen een **andere entity** achter hem aanbotst (`RigidCrowd`):
  - De achterste entity krijgt een deel van het momentum overgedragen, raakt zelf ook uit balans en probeert met zijn handen de vallende maat op te vangen of struikelt met hem mee op een hoopje.
  - Maakt gevechten tegen groepen mobs in smalle gangen extreem filmisch.

### 3. Surface Friction & Slippery Terrain (IJs, Modder, Zand & Water)
- Het succes van de balans-stappen hangt af van het blok onder de voeten (`BlockState` frictie):
  - **Op IJs (`Ice` / `Packed Ice` / `Blue Ice`):** Als een entity achteruit probeert te stappen om zijn balans te houden, glijden zijn voeten onder hem weg en klapt hij veel sneller achterover op zijn rug of billen.
  - **In ondiep Water / Modder (`Mud` / `Soul Sand`):** Stappen kosten meer tijd door weerstand, waardoor een zware hit sneller tot een plons/val leidt (met bijbehorende water-splash particles).

### 4. "Get-Up" Animaties vanuit de Echte Eind-Pose (Seamless Recovery)
- In plaats van dat een gevallen ragdoll terug-snapt naar de standaard staande Minecraft-pose, leest het systeem of de ragdoll **op zijn buik, op zijn rug, of op zijn zij** ligt:
  - **Op de buik:** Zet beide handen naast de borst op de grond → drukt zich op → trekt één been onder zich → staat op.
  - **Op de rug:** Rolt eerst zijn torso overeind in zitpositie → zet één hand achter zich op de grond → staat op.
  - Tijdens het opstaan is de entity nog kwetsbaar: een trap of schot tijdens het opkrabbelen duwt hem meteen weer terug tegen de vlakte.

### 5. Weapon & Held-Item Bracing (Wapen Gebruiken als Steunpunt)
- Als een entity een zwaard, bijl of speer in zijn hand heeft en zwaar achteruit strompelt door een klap:
  - In plaats van zijn wapenhand naar zijn wond te brengen, gebruikt hij zijn **vrije linkerhand** voor de wond of muur, en kan hij bij een diepe knieval zelfs de punt van zijn wapen op de grond zetten om op te steunen terwijl hij overeind komt.

### 6. Ledge Pull-Up vs. Slip-Off (Slimme Klif-Overleving voor Sterke Mobs/NPC's)
- Wanneer een entity aan een klifrand hangt (zoals in Scenario B):
  - **Zwakke of zwaar gewonde entities (< 30% HP):** Hebben niet genoeg kracht meer, glijden na 1–3 seconden van de rand af en vallen naar beneden.
  - **Gezonde/Sterke entities of Bosses:** Kunnen zichzelf na 1.5 seconde hangen **daadwerkelijk aan hun armen omhoog optrekken (mantle/pull-up)** en weer op de klif klimmen, tenzij jij op hun vingers slaat of ze nog een schot geeft terwijl ze hangen!

### 7. Contextual Wall-Pin & Slump (Tegen een Muur Zakken bij Zware/Fatale Hits)
- Als een entity met zijn rug vlak tegen een muur staat en een zware (of net fatale) klap krijgt in zijn borst/buik:
  - In plaats van voorover te vallen of door de muur te clippen, klapt zijn achterhoofd en rug tegen de muur aan en **glijdt/zakt hij langzaam met zijn rug langs de muur omlaag** tot hij in een zittende houding op de grond tegen de muur eindigt (precies zoals in *RDR2* en *Bodycam*).

### 8. Spiervermoeidheid & "Adrenaline Decay" (Cumulatieve Stumble Meter)
- Elke keer dat een entity kort achter elkaar klappen opvangt zonder te vallen, loopt een interne **Balance/Fatigue waarde** op:
  - De eerste mini-bolt vangt hij nog strak op met 2 snelle passen achteruit.
  - Bij de 3e en 4e bolt vlak erna worden zijn stappen slordiger, buigen zijn knieën dieper door en zwaaien zijn armen wijder.
  - Bij de 5e bolt zijn z'n benen "op" en zakt hij door zijn hoeven.

### 9. Audio- & Particle-Feedback Gekoppeld aan Lichamelijke Botsingen (QOL)
- Elke fysieke interactie van het lichaam met de wereld geeft directe zintuiglijke feedback:
  - **Hand grijpt klifrand of muur:** Kort schrapend steen-/houtgeluid + klein stofpluimpje bij de vingers.
  - **Torso/Rug botst tegen muur of grond:** Doffe body-thud sound waarvan het volume schaalt met de botsingssnelheid + ring van blok-stof (`ParticleFx`).
  - **Voeten die slippen bij achteruit strompelen:** Kort schuif-geluid over het bloktype (grind, gras, steen, zand).

### 10. Performance Budget, Crowd LOD & `ClientSettings` Toggles (QOL)
- Zodat de FPS altijd hoog blijft, ook bij 50 zombies tegelijk:
  - **Afstands-LOD (Level of Detail):**
    - **Dichtbij (< 24 blokken):** Volledige Active Ragdoll + Voxel/Ledge scanning + vinger/hand IK.
    - **Middellange afstand (24–48 blokken):** Vereenvoudigde balans- en wondgrijp-lagen zonder blok-raycasts per vinger.
    - **Ver weg (> 48 blokken):** Standaard snelle hit-reactie.
  - **Max Active Stumblers Cap:** Maximaal de `N` dichtstbijzijnde geraakte entities draaien tegelijk volledige omgevings-collision, zodat grote AOE-aanvallen nooit lag veroorzaken.
  - **Instelbaar in `ClientSettings`:** Spelers kunnen de intensiteit kiezen (*Subtle / Realistic RDR2 / Over-the-Top Euphoria*) of onderdelen los aan/uit zetten.


---

## 5. Tien Meeslepende Omgevings-, Materiaal- en Physics-Factoren (IJs, Water, Lava & Meer)

### Factor 1: IJs & Spiegelgladde Oppervlakken (`Ice`, `Packed Ice`, `Blue Ice`, Gepolijste Blokken)
- **Extreem lage wrijving ($\mu \approx 0.02\text{–}0.08$):**
  - Wanneer een entity op ijs een klap krijgt en een corrigerende stap achteruit probeert te zetten, **schiet zijn geplante voet onder hem weg** (zichtbaar via de `Feet` en `Knee` bones).
  - Zijn benen splijten uit elkaar of zwiepen omhoog, waardoor hij keihard op zijn rug of heupen op het ijs smakt.
  - Eenmaal gevallen glijdt de ragdoll nog meterslang over het ijs door, terwijl hij tijdens het glijden tevergeefs met zijn handen en voeten grip probeert te vinden op het gladde oppervlak.

### Factor 2: Water, Drijfvermogen & Stroming (`Water`, Hydrodynamics & Buoyancy)
- **Plons-impact & Onderwater-Weerstand (Drag):**
  - Valt een struikelende of neergeschoten entity van een brug of klif in het water, dan remt de water-inslag zijn snelheid af met een grote splash en schakelt de ragdoll over naar **hydrodynamische physics**.
  - Onder water bewegen alle ledematen met hoge vloeistof-demping (trage, zwevende bewegingen in plaats van snelle lucht-vallen).
- **Drijfvermogen (Buoyancy) & Waterstroming (`FluidState.getFlow`):**
  - De borstkas (gevuld met lucht) heeft positief drijfvermogen terwijl benen en bepantsering iets zwaarder zijn: een bewusteloze/dode ragdoll drijft langzaam naar het wateroppervlak met zijn rug/borst boven en armen en benen slap omlaag hangend in het water.
  - Als het water stroomt (bijv. richting een waterval), drijft het lichaam mee met de stroomvector en botst het onderweg met zijn schouders en benen tegen rotsblokken in de rivierbedding.

### Factor 3: Lava, Magma & Vuur-Paniek (`Lava`, `Magma Block`, `Fire`, `Soul Fire`)
- **Dikke Viscositeit van Lava:**
  - Lava is gesmolten steen en veel dikker en zwaarder dan water. Een lichaam dat in lava valt plonst niet diep naar de bodem, maar blijft half op het stroperige oppervlak liggen en **zakt er langzaam en zwaar in weg**.
- **Thermische Pijn- & Paniek-Reflex (Non-Fatal Brand/Lava Contact):**
  - Raakt een levende entity met één voet een `Magma Block`, vuur of de rand van lava, dan trekt hij dat been met een directe schrikreflex omhoog (hinkelen).
  - Valt hij in vuur of tegen de rand van een lavapoel, dan probeert hij in blinde paniek met beide handen op de vaste oeverblokken te slaan en zichzelf uit de lava te trekken, of slaat hij met zijn handen naar zijn brandende armen/torso terwijl hij over de grond rolt.

### Factor 4: Zachte, Kleverige & Elastische Blokken (`Slime`, `Honey`, `Mud`, `Powder Snow`, `Cobweb`)
- **Slime Blocks (Elastische Bounce):** Een ragdoll of vallende entity die op een `Slime Block` klapt, veert echt terug de lucht in waarbij de armen en benen door de terugslag na-zwiepen.
- **Honey Blocks & Cobwebs (Kleef- en Trek-fysica):** Handen, voeten en rug blijven bij contact met honing of spinnenwebben kort "plakken". Als de entity achteruit wordt geslagen terwijl zijn voeten in een `Cobweb` of op `Honey` staan, blijven zijn voeten vastplakken en klapt zijn bovenlichaam als een katapult achterover.
- **Mud, Soul Sand & Powder Snow (Wegzakken & Enkel-Lock):** De voeten (`Feet Bones`) zakken enkele pixels diep in het blok weg. Bij een zijwaartse of achterwaartse klap kunnen de voeten daardoor veel moeilijker verplaatsen, waardoor de knieën sneller knikken en de entity zwaar in de modder of sneeuw valt (met sneeuw-/modder-spetters).

### Factor 5: Armor-Gewicht & Materiaal-Traagheid (Naked vs. Leather vs. Iron/Diamond vs. Netherite)
- Het systeem telt het daadwerkelijke gewicht van gedragen armor per lichaamsdeel mee (Helm op het hoofd, Chestplate op de torso, Leggings op bekken/bovenbenen, Boots op de onderbenen/voeten):
  - **Licht / Geen Armor:** Maximale wendbaarheid; snelle voetherstel-stappen, kan lang aan één hand aan een klifrand blijven hangen en trekt zich makkelijk op.
  - **Zwaar Armor (bijv. Volledig Iron, Diamond of Netherite):**
    - Verhoogt de traagheid: lichte aanvallen duwen je minder snel omver, maar **als je eenmaal valt of van een trap rolt**, klap je als een zware tank tegen de grond (met metalen kletter-geluiden en diepe knie-buigingen).
    - Aan een klifrand hangen met zwaar Netherite armor put je grip 3x zo snel uit, en zwakke boomtakken (`Leaves`) breken direct onder je extra gewicht!
    - In diep water zorgt zwaar metaal-armor ervoor dat je langzaam naar de bodem zinkt in plaats van blijft drijven.

### Factor 6: Hoogte-Val, Luchtweerstand & Skydiving Fysica (High-Altitude Freefall)
- Wanneer een entity van grote hoogte valt of hoog de lucht in wordt geslagen:
  - Zodra de valsnelheid toeneemt, duwt de **opwaartse luchtweerstand (Air Drag)** tegen de ledematen en kleding.
  - Een levende entity spreidt zijn armen en benen of maait in paniek door de lucht om zijn oriëntatie te zoeken, terwijl zijn lichaam langzaam kantelt tijdens de vlucht.
  - Bij de uiteindelijke grond-inslag wordt de kinetische energie ($\frac{1}{2} m v^2$) omgezet in een zware compressie-klap, grond-scheuren/stofwolken (`ParticleFx`) en een realistische doorrol of bounce afhankelijk van de hoek waarmee hij neerkomt.

### Factor 7: Over-de-Rand Bungelen & Gedeeltelijke Ondersteuning (Edge Overhang & Limb Droop)
- In standaard Minecraft zweeft een model stijf horizontaal als het midden van de hitbox op een blokrand staat.
- Met per-bone zwaartekracht en `RigidBlocks` raycasts:
  - Ligt een uitgeschakelde of gevallen entity met zijn borst op de rand van een klif, brug of dak terwijl zijn benen of hoofd + armen over de rand in het luchtledige steken, dan **hangen die uitstekende lichaamsdelen door de zwaartekracht echt slap naar beneden langs de rand**.
  - Ligt hij voor meer dan 55% van zijn lichaamsgewicht over de rand heen, dan kantelt hij langzaam over het kantelpunt heen en glijdt hij alsnog van het dak of de klif af.

### Factor 8: Hellingshoek & Zwaartekracht-Schuiven (Downhill Sliding & Rolling)
- Valt een entity neer op een schuine berghelling, een schuin dak van trappen, of een stapel blokken:
  - Het systeem berekent de lokale hellingsgradiënt onder het bekken en de torso.
  - Op een steile helling (of een gladde helling van ijs/natte steen) blijft een gevallen lichaam niet bevroren liggen, maar **schuift of rolt het door de zwaartekracht naar beneden** totdat het op een vlak stuk grond komt of achter een rotsblok/boom blijft haken.

### Factor 9: Elementaire Spier-Reacties (Elektriciteit / Vorst / Vergif / Krachtvelden)
- Het Active Ragdoll systeem reageert uniek op het **type schade** van de powers en spells in de mod:
  - **Lightning / Elektriciteit (Thor & Lightning Bolt spell):** Stroomstoot door het zenuwstelsel → alle spieren en gewrichten (`Shoulder`, `Elbow`, `Wrist`, `Pelvis`, `Knee`, `Ankle`) verkrampen en schokken op hoge frequentie (tetanische spierspasmen) terwijl de entity op zijn tenen verstijft en daarna dampend in elkaar zakt.
  - **Frost / Bevriezing:** De gewrichtswrijving (`jointDamping`) loopt hoog op → de entity beweegt houterig en stijf; bij een harde klap verstijven de ledematen bijna volledig tijdens de val.
  - **Poison / Necrotic / Wither:** Tast de spierkracht (`muscleStrength`) aan → de knieën trillen, het hoofd hangt zwaar voorover en één hand houdt de maag/borst vast terwijl hij nauwelijks op zijn benen kan blijven staan.
  - **Wind / Telekinese / Gravity Pull (Lightning Tornado / Void):** Je ziet de ledematen eerst richting de zuigkracht getrokken worden voordat de voeten hun grip op de grond verliezen en meegesleurd worden.

### Factor 10: Gelokaliseerde Gewrichtsschade & Anatomische Limieten (Joint Limits & Limb Disable)
- Elk gewricht heeft strikte **anatomische draailimieten** (knieën en ellebogen buigen maar één kant op; polsen, enkels, nek en onderrug hebben natuurlijke kegel-limieten zodat een ragdoll nooit in onmogelijke knopen draait).
- **Tijdelijke Limb Disable bij Gerichte Treffers:**
  - Wordt een entity vol in zijn **rechterarm/schouder** geraakt, dan verliest die ene arm tijdelijk zijn spierkracht en hangt die slap en bungelend langs het lichaam (en kan die hand even geen klifrand grijpen), terwijl de **linkerarm** nog wél volledig werkt om de wond vast te houden of een rand te pakken!
  - Wordt hij vol in zijn **linkerknie** geraakt, dan sleept dat been achter hem aan en moet het rechterbeen al het hinkel- en opvangwerk doen.

---

## 6. Technische Samenvatting (Koppeling met Onze Mod-Architectuur)

| Onderdeel | Bestaande / Nieuwe Mod-Systemen | Verantwoordelijkheid |
|---|---|---|
| **Hit-Zone & Impuls Sync** | Uitbreiding op `DeathBlows` payload | Stuurt bij non-fatal én fatal hits het exacte raakpunt (lokale `(x, y, z)` op de entity + impuls-vector) van server naar client. |
| **Balans & Voetstappen** | `FootPlanting` + `Limbs` + `Ik` | Berekent waar het zwaartepunt (CoM) heen valt en laat de voeten corrigerende passen zetten om onder het CoM te blijven. |
| **Wond Grijpen & Muur Steunen** | `Poses.layer` + Arm `Ik` + `BentParts` | Stuurt 1 of 2 handen naar het wond-ankerpunt op het lichaam óf naar het dichtstbijzijnde muurvlak binnen armlengte. |
| **Klifrand & Boomtak Grijpen** | Server Ledge-Check + Client Hand `Ik` | Server pauzeert/remt de val (`deltaMovement`) tijdens de hang-timer; client plaatst de handen op de blokrand en laat de rest van het lichaam als pendule/ragdoll hangen. |
| **Volledige Stumble & Wall Slump** | `Ragdolls` + `RigidWorld` / `RigidBlocks` | Schakelt naadloos van actieve balans naar tijdelijke physics-ragdoll (met blok-botsingen) en blendt via een Get-Up pose weer terug naar de levende entity. |