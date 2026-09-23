# Green Lantern

---

## 1. Transformatie & Uniform

- **Veranderen in Green Lantern:**
  - Een groene lichtlijn glijdt van je hoofd naar je voeten omlaag.
  - Dit laat het Green Lantern pak achter over je eigen kleding: groen met zwart, het lantaarnsymbool op de borst, witte handschoenen en een groen masker voor je ogen.
  - Je eigen gezicht en haar blijven zichtbaar.
  - De Power Ring verschijnt om de middelvinger van je rechterhand.
- **Terugveranderen:**
  - De lichtlijn glijdt weer omhoog en het pak valt uit elkaar in groene vonken.
- **Zichtbaarheid:**
  - Zichtbaar in first-person en third-person, en voor iedereen om je heen.

---

## 2. Power Ring

- De ring heeft maximaal **100 power**.
- Een volle ring gloeit fel; hoe leger hij raakt, hoe minder fel hij brandt, en als hij bijna leeg is hapert het licht.
- De ring is massief, nooit doorzichtig: zilveren band, donkere zetting, groene steen die zelf gloeit.
- Rechterhand valt aan (linksklik), linkerhand verdedigt (rechtsklik).
- **Pak licht op, alleen terwijl de ring werkt:** de lantaarn op je borst gloeit als een kern, groene energie loopt vanuit de kern over veel rechte lijnen door het hele pak, met pulsen naar buiten: de dikste en snelste over je rechterschouder en rechterarm naar de ring, de rest over borst en rug, langs je zij, je andere arm, je benen en de achterkant van je hoofd, en de ring straalt fel. Hoe harder de ring werkt, hoe feller. Een ring in rust laat het pak gewoon.
- **Paneel:** een balk met de power en het getal ernaast, met streepjes op een kwart, de helft en driekwart. Niets knippert:
  - Betaalt de ring iets, dan zakt de balk meteen en blijft wat eraf ging nog even als gouden stuk staan, dat daarna wegloopt. Opladen laat de balk omhoog glijden.
  - Tijdens het opladen van de Giant Fist is wat hij gaat kosten een vast, gestreept stuk aan het eind van de balk, met een wit streepje waar je uitkomt (`100 -2.2`).
  - Loopt de ring vanzelf leeg (vliegen, schild, koepel, laser), dan staat er in goud hoe snel (`97 -0.08/s`), en in de lucht hoeveel seconden vliegen er nog over zijn (`90 -1.07/s 85s`).

---

## 3. Muisknop Systeem: Click vs. Hold (Drempelwaarde)

Voor **alle** linksklik- (aanval) en rechtsklik-acties (verdediging) geldt een apart systeem met twee abilities:
- **Click / Tap (onder drempelwaarde):** De directe actie bij een losse klik of kort indrukken.
- **Hold (boven drempelwaarde, bijv. 2 sec):** Activeert een krachtige, continue hold-ability zolang je de knop ingedrukt houdt, totdat je loslaat of je energie op is.
- **Geldigheid:** Dit werkt op **alle** left en right click abilities:
  - Zowel **standaard** (lege handen / alleen de ring).
  - Als in **Construct Mode** (elk hard-licht wapen uit het Construct Wheel).
- **Gelijktijdig gebruik (Aanval + Verdediging):** Je kunt verdediging (rechtsklik) klikken of inhouden *terwijl* je aanvalt (linksklik). Ze kunnen allebei **exact tegelijkertijd** actief zijn (bijv. bolts of de laser afvuren terwijl je het schild of de krachtveld-koepel omhoog houdt).
- **Uitzondering:** Werkt **niet** op keybinds (zoals R voor Giant Fist, Z voor Recharge of V voor het wiel); die hebben hun eigen besturingslogica.

---

## 4. Basis (geen ability actief / lege handen)

### Left Click (Aanval)
- **Click / Tap (Light Bolt):**
  - **Actie:** Schiet een compacte bolt / kogel af van geconcentreerde energie / construct.
  - **Schade:** 3 harten per schot.
  - **Kosten:** 0,16 power per schot.
  - **Cooldown:** 0,3 sec tussen losse schoten.
- **Hold na 2 sec (Continuous Laser / Beam):**
  - **Actie:** Bundelt de energie in een continue, felle groene laserstraal recht vooruit.
  - **Werking:** Blijft ononderbroken vuren zolang je linksklik inhoudt na de drempel van 2 seconden.
  - **Schade:** 2,5 harten per 0,25 sec (10 harten per seconde) aan alles in het pad van de laser: dwars door een hele rij wezens, tot de eerste muur, max 40 blokken.
  - **Kosten:** 0,8 power per seconde.
  - **Stopt:** Zodra je linksklik loslaat, de ring leeg is of je een Giant Fist begint.

### Right Click (Verdediging)
- **Click / Tap (Light Shield, aan/uit):**
  - **Actie:** Een tik zet een klein rond schild van hard licht voor je in je kijkrichting, vastgehouden door je linkerhand. Het blijft staan tot je nog een keer tikt. Anderen zien het solid; alleen in je eigen first-person-beeld kijk je erdoorheen.
  - **Verdediging:** 70% minder schade van alles wat van voren komt. Schade die sowieso dwars door armor gaat (gif, vallen, verdrinken, de void) en doorborende pijlen gaan er ook dwars doorheen.
  - **Kosten:** 0,08 power per seconde zolang het staat.
  - **Stopt:** Nog een tik, recharge of een lege ring.
- **Hold na 2 sec (Forcefield Dome / Krachtveld-Koepel):**
  - **Actie:** Het schild vouwt open tot een koepel van hard licht rondom je hele lichaam (360°), zolang je rechtsklik inhoudt. Van buiten is hij solid; wie erin staat kijkt erdoorheen.
  - **Verdediging:** 40% minder schade uit alle richtingen (dekt meer dan het schild, houdt minder tegen). Dezelfde uitzonderingen als het schild.
  - **Kosten:** 0,24 power per seconde. Een schild dat al aan stond wacht onder de koepel en kost dan niets.
  - **Stopt:** Zodra je rechtsklik loslaat, recharge of een lege ring.

---

## 5. Giant Fist (toets R)

- **Actie:** Een grote vuist van groen licht verschijnt rechts naast je en laadt op zolang je R inhoudt.
- **Opladen:** Groeit langzaam zolang je R inhoudt. Wijkt zelf soepel uit naar boven of links als een muur of de grond in de weg zit.
- **Afvuren:** Laat R los om de vuist af te vuren. Gaat dwars door muren heen en raakt alle vijanden op zijn pad.
- **Richten:** Het midden van de vuist gaat precies naar waar je crosshair op richt; hij zwaait in een vloeiende boog vanaf rechts naar je richtlijn.
- **Grootte:** Tot 5,7 blokken breed bij een volle lading.
- **Schade:** 6 harten bij een korte tik, tot 14 harten bij een volle lading.
- **Kosten:** 1,6 power (tik) tot 3,2 power (volle lading). De ring betaalt pas als hij wordt afgevuurd.
- **Cooldown:** 4 sec na het afvuren.
- **Annuleren:** Bukken (crouch) tijdens het inhouden laat de vuist verdwijnen zonder schot, zonder kosten en zonder cooldown.
- **Let op:** Dit is een toets-ability (keybind) en gebruikt dus **geen** 2-seconde drempelwaarde.

---

## 6. Construct Wheel (toets V)

- **Actie:** Keuzewiel om hard-licht wapens in je handen te nemen.
- **Tikken:** Snelle wissel tussen lege handen en je laatste construct.
- **Inhouden:** Opent een wiel met 12 slots (midden is lege handen / alleen de ring).
- **Besturing per construct (Click vs. Hold):**
  - **Linksklik (Aanval):**
    - *Click / Tap:* Standaard wapenaanval (bijv. snelle zwaardhouw, hamerslag, speerstoot).
    - *Hold na 2 sec:* Continue zware aanval passend bij het wapen (bijv. whirlwind ronddraaiende aanval met zwaard/hamer, continue snelle piercing-stoten met speer).
  - **Rechtsklik (Verdediging):**
    - *Click / Tap:* Snelle parry of actieve blok met het construct.
    - *Hold na 2 sec:* Versterkte construct-verdediging (bijv. massieve energie-barricade of absorberend fort-schild).
  - **Gelijktijdig gebruik:** Net als met lege handen kun je ook met constructs exact tegelijkertijd aanvallen en verdedigen (klik of hold).

---

## 7. Recharge (toets Z)

- **Actie:** De lantaarn verschijnt in je linkerhand en je slaat er met je rechtervuist op.
- **Effect:** Groen licht schiet uit de voorkant en vult de ring bij (50 power per slag, tot 100).
- **Duur:** Duurt minder dan 2 seconden.
- **Cooldown:** 3 sec. Kan niet tijdens het opladen van de Giant Fist.
- **Ook in de lucht**, na het opstijgen: je blijft vliegen. De lantaarn hangt rechtop aan je hand hoe je lichaam ook ligt, in first person schudt hij in de wind, en de klap stuurt ringen van licht rond je vliegrichting. Opladen terwijl je met een lege ring zakt laat je weer vliegen.
- **Deeltjes:** helemaal groen, en ze vliegen niet ver (een uitbarsting voor de lantaarn).

---

## 8. Vliegen (toets C, ability 9)

- **Opstijgen:** Beide vuisten naar je borst met een kleine dip, dan zwaaien je armen omlaag langs je zij, je hoofd gaat omhoog en je stijgt een paar blokken, met een lichtflits aan je voeten. Daarna vlieg je zonder pauze door. Duurt iets meer dan 1 sec en vangt je ook op midden in een val.
- **Vliegen:** Vooruit inhouden = snelheid opbouwen in je kijkrichting, tot **35 blokken per sec** (iets sneller dan een elytra met raketten, die ongeveer 33 haalt). Loslaten = uitglijden tot zweven. Springen = stijgen, bukken = zakken, links/rechts = opzij, achteruit = terugdrijven. Je neemt je snelheid mee in bochten.
- **Animatie:** Zwevend sta je rechtop met armen iets uit. Hoe sneller, hoe meer je lichaam langs je vliegrichting ligt (armen langs je zij, benen bij elkaar); je helt over in bochten, duikt met je hoofd voorop en klimt met je hoofd omhoog. Een streep groen licht achter je bij hoge snelheid; laag vliegen blaast stof of water op.
- **Kosten:** Opstijgen minstens **0,8 power**. Vliegen kost **1,07 power per sec** (100 / 93,75): een volle ring houdt je **max 93,75 sec** (ruim anderhalve minuut) in de lucht. Alles wat je tijdens het vliegen doet kost daar bovenop.
- **Landen:** Rustig op de grond zakken = vanzelf landen, of C nog een keer (in de lucht val je dan vanaf daar). Daarna 1 sec cooldown.
- **Ring leeg in de lucht:** Je zakt langzaam omlaag met je armen omhoog, zonder sturen, en landt zonder schade. Opladen onderweg laat je weer vliegen.
- **Landen door een duik:** vlieg je de grond in terwijl je naar beneden kijkt, dan stop je automatisch met vliegen, ook als je niet volle snelheid gaat.
- **Landingsklap (automatisch; met X doe je hem zelf, zie 9):** duik je op (bijna) volle snelheid de grond in, dan stopt de vlucht met een superhelden-landing. Vlak voor de grond draai je rechtop, voeten eerst, ringvuist hoog geheven; dan kom je laag op één knie en ram je die vuist de grond in, die rondom openscheurt en brokken opgooit, je andere arm naar achteren. In first person knikt je blik even omlaag naar je vuist in de grond en weer omhoog op tijd voor de klap. De ring maakt een reuzenconstruct dat een schokgolf geeft (schade en terugslag rondom, standaard 6 harten in het midden, 5 blokken ver, kost 1,6 power; zonder genoeg power land je alleen hard). Elke keer willekeurig één van **32**:
  - *Uit de lucht:* vuist, strijdhamer, aambeeld, laars, gewicht van 1 ton, je eigen lantaarn, kluis, anker aan een ketting, stekelbal, halter, klok die luidt, slaande hand, zwaard dat blijft staan, piano, speelgoedsteen, stempel die het embleem in de grond drukt, blok TNT dat ontploft, brandende meteoor.
  - *Dichtklappend:* twee handen, twee vuisten, bekkens, berenklem, boek.
  - *Uit de grond:* uppercut (de grond scheurt en rommelt eerst, dan barsten vuist en arm eruit), twee ringen pieken, een pilaar die omhoog schiet en omvalt.
  - *Neergezwaaid:* vliegenmepper en pickaxe over je schouder, hamer van een rechter op zijn blok, drumstokken op een trommel.
  - Het embleem dat plat voorover valt, en een salvo van vijf raketten.
  - Elk construct is solid en groeit uit het licht van de ring; aan het eind breekt het in solid stukken (of zakt terug de grond in). De schokgolf is een lage ring van solid hard licht.
- **Vechten in de lucht (dezelfde knoppen):**
  - *Tik links:* bolt neemt jouw snelheid mee, dus je haalt je eigen schoten nooit in.
  - *Hold links:* de laser, voor strafing runs over vijanden en de grond.
  - *Tik rechts:* het schild wordt een puntige, gestroomlijnde **ramkegel** voor je, linkervuist vooruit. Houdt 70% van voren tegen; wat je raakt wordt weggeslingerd en krijgt **2 harten + 3 harten per blok per tick snelheid** (ongeveer 7 harten op topsnelheid). Zelfde wezen pas na 0,6 sec opnieuw.
  - *Hold rechts:* de koepel werkt als **luchtrem**: je snelheid halveert zolang je inhoudt.

---

## 9. Schokgolf (toets X, ability 8)

- **Actie:** de landingsklap wanneer jij wilt. Je ramt je ringvuist in de grond en de ring maakt een van de 32 reuzenconstructs (willekeurig), dat inslaat en een schokgolf over de grond stuurt.
- **Op de grond:** meteen, op één knie, vuist in de grond.
- **Vliegend:** je duikt recht omlaag op volle snelheid (je toetsen wachten tot je de grond raakt) en klapt waar je neerkomt. Niet tijdens het opstijgen.
- **Springend of vallend:** je schiet recht omlaag, rechtop met je vuist geheven, en klapt zodra je de grond raakt. De ring breekt je val: geen valschade, hoe hoog je ook was. In water gebeurt er niets.
- **Schade:** 6 harten in het midden, de helft aan de rand, 5 blokken ver; alles wordt weggeslingerd.
- **Kosten:** 1,6 power, betaald als de klap valt; zonder genoeg power land je alleen hard.
- **Cooldown:** 5 sec, vanaf het indrukken.
- **Kan niet** tijdens het opladen van de ring of het laden van de Giant Fist, en niet terwijl je vorige klap nog bezig is.
- **Instellingen:** schade, bereik, terugslag en kosten gelden ook voor de landingsklap bij het vliegen (één set, onder Schokgolf).
- **Paneel:** op weg naar beneden staat er `duikt` achter de toets.
