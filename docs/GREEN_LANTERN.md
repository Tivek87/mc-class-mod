# Green Lantern

---

## 1. Transformatie & Uniform

- **Veranderen in Green Lantern (de ring komt naar je toe):**
  - De ring verschijnt ergens 16 tot 32 blokken van je af, pulseert van licht en vliegt in 3 seconden naar je toe; hij blijft 3 blokken voor je in de lucht hangen.
  - Daar vormt hij je lantaarn uit zijn licht; de lantaarn vliegt naar je linkerhand en je vangt hem.
  - Dan vliegt de ring om je middelvinger. Zodra hij zit, rolt er een schokgolf van licht 16 blokken om je heen: de wezens van het duister daarin (ondoden, de Warden, vexes, endermen, endermites) rennen 10 seconden van je weg.
  - Tegelijk groeit het pak langzaam uit de ring over je heen: je arm op, naar de lantaarn op je borst, dan over je hele lichaam. Het masker komt pas als de rest helemaal aan is.
  - Tot slot sla je vanzelf met je ringvuist op de lantaarn en laadt de ring op (de recharge).
  - Tot dat klaar is doet de ring niets anders.
  - Het pak: groen met zwart, het lantaarnsymbool op de borst, witte handschoenen en een groen masker voor je ogen. Je eigen gezicht en haar blijven zichtbaar.
- **Terugveranderen:**
  - Andersom: eerst gaat het masker weg, dan trekt het pak terug over je benen, je lijf en je arm in de ring, en de ring vliegt omhoog weg.
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
  - **Actie:** Schiet een compacte bolt af: een rond kogeltje van hard licht met een punt, met een lichtstreep erachter.
  - **Schade:** 3 harten per schot.
  - **Kosten:** 0,16 power per schot.
  - **Cooldown:** 0,3 sec tussen losse schoten.
- **Hold na 2 sec (Continuous Laser / Beam):**
  - **Opladen:** Terwijl je inhoudt vult de energie je pak: vanuit de lantaarn op je borst loopt hij je rechterarm af naar de ring (die arm gloeit steeds feller en komt omhoog om te richten), daarna over de rest van het pak. Lichtspikkels stromen van alle kanten de ring in, een lichtbol zwelt in je vuist met twee draaiende lichtringen eromheen, aan het eind knetteren er vonken af en een fluittoon stijgt. Laat je eerder los, dan zakt het weer weg. Iedereen om je heen ziet het.
  - **Losbarsten:** De straal schiet in een oogwenk uit de ring met een flits en een ring van licht uit je vuist, en je beeld schokt even terug. Daarna brult hij door: een witheet hart in een dikke, ademende groene gloed, met lichtgolven die erlangs razen, drie draden die eromheen draaien, lichtringen die erlangs lopen en vonken die eraf knetteren. Waar hij inslaat spat hij uiteen: een hete flits, rimpels en vonken die terugspatten. Je beeld trilt licht zolang hij straalt.
  - **Actie:** Bundelt de energie in een continue, felle groene laserstraal recht vooruit. Elke treffer duwt wat hij raakt een stukje terug.
  - **Werking:** Blijft ononderbroken vuren zolang je linksklik inhoudt na de drempel van 2 seconden.
  - **Schade:** 2,5 harten per 0,25 sec (10 harten per seconde) aan alles in het pad van de laser: dwars door een hele rij wezens, tot de eerste muur, max 40 blokken.
  - **Kosten:** 0,8 power per seconde.
  - **Stopt:** Zodra je linksklik loslaat, de ring leeg is of je een Giant Fist begint.

### Right Click (Verdediging)
- **Click / Tap (Light Shield, aan/uit):**
  - **Actie:** Een tik zet een klein rond schild van hard licht voor je in je kijkrichting, vastgehouden door je linkerhand. Het blijft staan tot je nog een keer tikt. Anderen zien het solid; alleen in je eigen first-person-beeld kijk je erdoorheen (je ziet dan de omtrek en een zachte gloed).
  - **Uiterlijk:** licht bol, met een ronde verhoogde rand, een groef in de voorkant, klinknagels die langzaam ronddraaien, het lantaarn-embleem in het midden en een handgreep achterop. Een inslag stuurt een rimpel van licht over de voorkant, en af en toe glijdt er een glans overheen.
  - **Verdediging:** 70% minder schade van alles wat van voren komt. Schade die sowieso dwars door armor gaat (gif, vallen, verdrinken, de void) en doorborende pijlen gaan er ook dwars doorheen.
  - **Kosten:** 0,08 power per seconde zolang het staat.
  - **Stopt:** Nog een tik, recharge of een lege ring.
- **Hold na 2 sec (Forcefield Dome / Krachtveld-Koepel):**
  - **Actie:** Het schild vouwt open tot een koepel van hard licht rondom je hele lichaam (360°), zolang je rechtsklik inhoudt. Van buiten is hij solid; wie erin staat kijkt erdoorheen. De naden lopen als de voegen van een stenen muur, met een heldere band waar hij de grond raakt en een kroon van licht bovenop.
  - **Verdediging:** 40% minder schade uit alle richtingen (dekt meer dan het schild, houdt minder tegen). Dezelfde uitzonderingen als het schild.
  - **Kosten:** 0,24 power per seconde. Een schild dat al aan stond wacht onder de koepel en kost dan niets.
  - **Stopt:** Zodra je rechtsklik loslaat, recharge of een lege ring.

---

## 5. Giant Fist (toets R)

- **Actie:** Een grote vuist van groen licht verschijnt rechts naast je en laadt op zolang je R inhoudt.
- **Opladen:** Groeit langzaam zolang je R inhoudt. Wijkt zelf soepel uit naar boven of links als een muur of de grond in de weg zit.
- **Afvuren:** Laat R los om de vuist af te vuren. Gaat dwars door muren heen en raakt alle vijanden op zijn pad.
- **Richten (stuurbaar):** De vuist blijft zijn hele vlucht precies onder je crosshair. Bij het loslaten glijdt hij vanaf rechts naast je naar het midden van je beeld zonder te draaien, en daarna volgt hij je blik: kijk je opzij of omlaag, dan gaat hij mee.
- **Uiterlijk:** Je ring zit aan de middelvinger van de vuist (de ring met zijn edelsteen), en de duim heeft een nagel.
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
- **Inhouden:** Opent een groot wiel met 16 slots (midden is lege handen / alleen de ring). Het midden zegt wat je aanwijst; een balk onderaan zegt hoe de knoppen werken.
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
- **Vliegen:** Vooruit inhouden = vliegen in je kijkrichting. **Je begint langzaam en gaat sneller hoe langer je doorvliegt:** je start op ongeveer **12 blokken per sec** (een derde van de top) en bouwt **12 seconden** lang snelheid op, tot de top van **35 blokken per sec** (iets sneller dan een elytra met raketten, die ongeveer 33 haalt), en nooit meer. Vooruit loslaten = uitglijden tot zweven, en de opgebouwde snelheid zakt langzaam weer weg (helemaal in 3 sec); tegen een muur duwen houdt hem vast. Springen = stijgen, bukken = zakken, links/rechts = opzij, achteruit = terugdrijven. Je neemt je snelheid mee in bochten. Begin- en opbouwtijd zijn instellingen.
- **Animatie:** Zwevend sta je rechtop met armen iets uit. Hoe sneller, hoe meer je lichaam langs je vliegrichting ligt (armen langs je zij, benen bij elkaar); je helt over in bochten, duikt met je hoofd voorop en klimt met je hoofd omhoog. Een streep groen licht achter je bij hoge snelheid; laag vliegen blaast stof of water op.
- **Kosten:** Opstijgen minstens **0,8 power**. Vliegen kost **1,07 power per sec** (100 / 93,75): een volle ring houdt je **max 93,75 sec** (ruim anderhalve minuut) in de lucht. Alles wat je tijdens het vliegen doet kost daar bovenop.
- **Landen:** Rustig op de grond zakken = vanzelf landen, of C nog een keer (in de lucht val je dan vanaf daar). Daarna 1 sec cooldown.
- **Ring leeg in de lucht:** Je zakt langzaam omlaag met je armen omhoog, zonder sturen, en landt zonder schade. Opladen onderweg laat je weer vliegen.
- **Landen door een duik:** vlieg je de grond in terwijl je naar beneden kijkt, dan stop je automatisch met vliegen, ook als je niet volle snelheid gaat.
- **Landingsklap (automatisch; met X doe je hem zelf, zie 9):** duik je snel de grond in (minstens 21 blokken per sec, dus eerst een paar seconden doorvliegen), dan stopt de vlucht met een superhelden-landing. Vlak voor de grond draai je rechtop, voeten eerst, ringvuist hoog geheven; dan kom je laag op één knie en ram je die vuist de grond in, die rondom openscheurt en brokken opgooit, je andere arm naar achteren. In first person knikt je blik even omlaag naar je vuist in de grond, en kijkt dan omhoog naar het construct dat zich in de lucht voor je vormt en volgt het omlaag als het inslaat. De ring maakt een reuzenconstruct dat een schokgolf geeft (schade en terugslag rondom, standaard 6 harten in het midden, 5 blokken ver, kost 1,6 power; zonder genoeg power land je alleen hard). Elke keer willekeurig één van **32**:
  - *Uit de lucht:* vuist, strijdhamer, aambeeld, laars, gewicht van 1 ton, je eigen lantaarn, kluis, anker aan een ketting, stekelbal, halter, klok die luidt, slaande hand, zwaard dat blijft staan, piano, speelgoedsteen, stempel die het embleem in de grond drukt, blok TNT dat ontploft, brandende meteoor.
  - *Dichtklappend:* twee handen, twee vuisten, bekkens, berenklem, boek.
  - *Uit de grond:* uppercut (de grond scheurt en rommelt eerst, dan barsten vuist en arm eruit), twee ringen pieken, een pilaar die omhoog schiet en omvalt.
  - *Neergezwaaid:* vliegenmepper en pickaxe over je schouder, hamer van een rechter op zijn blok, drumstokken op een trommel.
  - Het embleem dat plat voorover valt, en een salvo van vijf raketten.
  - Elk construct is solid en groeit uit het licht van de ring; aan het eind breekt het in solid stukken (of zakt terug de grond in). De schokgolf is een lage ring van solid hard licht.
  - **Groot en goed te volgen:** standaard 35% groter dan eerst (ze slaan daarom iets verder voor je in) en anderhalf keer zo traag: het construct vormt zich in de lucht voor je, waar je het goed ziet, hangt even, haalt uit en slaat na ongeveer 1 sec in; het blijft na de klap ruim een seconde staan en breekt dan op; alles samen ruim 3 seconden. Allebei instelbaar (zie 9). Constructs die dichtklappen staan schuin naar je toe gedraaid, zodat je ze ziet dichtslaan in plaats van alleen hun randen.
  - **Elk construct beweegt:** de kluis ploft neer, het wiel draait, de deur vliegt open en munten, biljetten en goudstaven vallen eruit; de piano-klep springt open, de klep over de toetsen slaat omhoog, de toetsen springen en er zweven noten op; het aambeeld spat vonken en hupt nog een keer; het gewicht plet zwaar; de TNT landt, zwelt, knippert terwijl het lontje opbrandt en ontploft; de speelgoedsteen en de halter stuiteren; de stempel wiebelt en laat het embleem gloeiend achter; de laars landt hak eerst en de neus klapt erachteraan; het hengsel van de lantaarn zwaait; de ketting van het anker valt erachteraan en stapelt zich op; de stekelbal schommelt na; de klok schudt, de klepel zwaait en er lopen geluidsringen weg; de meteoor heeft gloeiende scheuren; het zwaard trilt na; de bekkens stuiteren uit elkaar en galmen; de berenklem rammelt; het boek bladert en klapt dicht; de hamer van de rechter klopt nog een keer; de trommelstokken spelen een roffel met rimpels over het vel; de kristalpieken schieten door en glinsteren; de zuil stuitert en zijn stukken schuiven uit elkaar; het embleem stuitert op de grond; de klets-hand trommelt met zijn vingers; de handen buigen hun vingers bij het klappen.
- **Vechten in de lucht (dezelfde knoppen):**
  - *Tik links:* bolt neemt jouw snelheid mee, dus je haalt je eigen schoten nooit in.
  - *Hold links:* de laser, voor strafing runs over vijanden en de grond.
  - *Tik rechts:* het schild wordt een puntige, gestroomlijnde **ramkegel** voor je, linkervuist vooruit, met ribbels die als de draad van een boor naar de punt lopen en ronddraaien. Houdt 70% van voren tegen; wat je raakt wordt weggeslingerd en krijgt **2 harten + 3 harten per blok per tick snelheid** (ongeveer 7 harten op topsnelheid). Zelfde wezen pas na 0,6 sec opnieuw.
  - *Schrapen:* vlieg je met de ramkegel laag over de grond (minder dan 1,5 blok erboven) of glijd je eroverheen, dan kost dat **2 power per sec extra**, vliegen er vonken af, schuurt hij hoorbaar en schudt je beeld (harder naarmate je sneller gaat). Alle drie instelbaar onder Light Shield › Ramkegel.
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
- **Instellingen:** schade, bereik, terugslag en kosten gelden ook voor de landingsklap bij het vliegen (één set, onder Schokgolf). Onder *De constructs* staan ook **Grootte van de constructs** (standaard 1,35) en **Slowmotion** (standaard 1,5: anderhalf keer zo traag; ook de schokgolf komt dan zoveel later).
- **Paneel:** op weg naar beneden staat er `duikt` achter de toets.

---

## 10. Constructs uitproberen (`/constructshockwave`)

- **Actie:** Typ `/constructshockwave` in de chat. Er opent een scherm met alle 32 constructs, in groepjes (uit de lucht, klappen dicht, uit de grond, neergezwaaid, en meer), met bij elk een korte uitleg als je hem aanwijst. Klik er één (of *Willekeurig*): het scherm sluit en een seconde later slaat dat construct voor je in, met schokgolf.
- **Voor wie:** iedereen, als personage maakt niet uit, maar alleen met cheats aan (of als operator). Zonder dat zegt de server het je.
- **Kosten:** geen power en geen cooldown. Loopt je vorige klap nog, dan wacht je even.

---

## 11. Ring-scan (toets N, ability 6)

- **Actie:** De ring scant alles om je heen, zoals in de strips ("Ring, scan het gebied"). Een golf van ringlicht rolt vanuit jou naar buiten, door muren heen, tot **32 blokken** ver. Iedereen om je heen ziet de golf.
- **Markeringen (alleen voor jou):** elk wezen dat de golf passeert krijgt **12 sec** een kader van licht met gemarkeerde hoeken, door muren heen zichtbaar, met zijn naam en levens erboven. Kleur: rood = vijandig, violet = wezen van het duister, blauw = andere speler, groen = de rest. Elk kader klikt vast met een tikje zodra de golf het bereikt.
- **Melding:** de ring zegt op je actiebalk hoeveel vijandige en andere levensvormen hij vond.
- **Kosten:** 2 power. **Cooldown:** 8 sec.

---

## 12. Constructstorm (toets Y, ability 7: de ultimate)

- **Y is altijd de ultimate** van een personage (bij Doctor Octopus de razernij).
- **Actie:** Je steekt je ringvuist naar de hemel. Een pilaar van licht schiet recht omhoog uit de ring en barst open in een **grote ring van licht** hoog boven je hoofd (ongeveer 15 blokken, lager onder een dak): twee ringen die tegen elkaar in draaien, spaken van licht ertussen en het lantaarn-embleem in het midden. Je actiebalk roept "CONSTRUCTSTORM!" en de titel van het paneel wordt rood en telt af.
- **Regen van constructs:** **8 sec** lang hangt de ring boven je, waar je ook gaat, met een draad van licht aan je ring vast. Elke 0,4 sec vormt zich eronder een van de constructs die uit de lucht vallen (vuist, strijdhamer, aambeeld, laars, gewicht van 1 ton, je lantaarn, kluis, anker, stekelbal, halter, klok, slaande hand, zwaard, piano, speelgoedsteen, stempel) en **valt op een wezen** binnen 20 blokken dat jou kwaad wil (of een speler, waar spelers mogen vechten). Hij blijft boven zijn wezen hangen terwijl hij vorm krijgt, een lichtring op de grond wijst aan waar hij inslaat, en hij slaat in met een eigen schokgolf: **5 harten** in het midden, de helft aan de rand, en alles vliegt weg.
- **Eerlijk verdeeld:** de constructs verdelen zich over de wezens; niets valt vlak naast jou; zonder wezens slaan ze om je heen op lege grond in, nooit op een huisdier of dorpeling.
- **Einde:** na de regen barst de ring in de lucht uiteen.
- **Kosten:** 20 power. **Cooldown:** 90 sec.
- **Instellingen:** duur, tijd tussen constructs, bereik, schade en kosten.
- Elk construct is solid en groeit uit het licht van de ring in de lucht (die zelf aan je ring hangt); aan het eind breekt het in solid stukken. De ring in de lucht en de pilaar zijn licht, geen constructs.

---

## 13. Lantaarnflits (toets Left Alt, ability 10)

- **Actie:** Je steekt je ringvuist hoog op en de ring vormt **je lantaarn** erboven: een solid construct dat uit het licht van de ring groeit. Licht stroomt er van alle kanten in en zijn hart brandt steeds feller, tot hij na bijna een seconde **uiteenbarst als een kleine zon**: stralen schieten weg, een schil van licht raast naar buiten en de lantaarn breekt in solid stukken.
- **Effect:** elk wezen binnen **12 blokken** dat de lantaarn kan zien is **4 sec verblind**, 3 sec vertraagd en verzwakt, en verliest wie het achterna zat. Wezens van het duister kunnen het licht niet verdragen: ze branden ook, krijgen **4 harten** schade en vluchten even.
- **Verblinding:** wie naar de flits kijkt krijgt even een wit-groen scherm, hoe rechter hij keek hoe erger. Je eigen flits houd je boven je hoofd, dus die verblindt jou maar een beetje.
- **Kosten:** 6 power. **Cooldown:** 15 sec.

---

## Lege toetsen

- Green Lantern gebruikt nu ability 1 t/m 10. Alleen **K (ability 11)** is nog vrij.
