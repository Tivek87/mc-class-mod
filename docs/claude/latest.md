# Laatste sessie

- Datum: 2026-10-02. Verzoek: dubbelcheck, dan alles committen, pushen en releasen (geen Thor/Mjolnir-werk).
- Commit `7fb8a78` "Revert hit reactions, ledge grabs and faster ragdolls": alles van 0.5.9-alpha eruit, ook ledge grab.
- Commit "Fix creature glitches in poses and ragdolls": enderman-kaak blijft onder het opgetilde hoofd; vermoeide pose
  alleen bij mens-bouw en zonder diepe hurk (husk); mobs met gewaad vallen meteen; gedragen blok, item in gevouwen
  armen en mooshroom-paddenstoelen volgen het slappe lijf.
- Bestanden: `Poses`, `Stance`, `Tired`, `ModelBends`, `RagdollBuild`, `RagdollBody`, `Ragdoll`, `Restore`, `Ragdolls`,
  `LivingEntityRendererMixin`, nieuw `MushroomCowMushroomLayerMixin`, `welcomescreen.mixins.json`, `CHANGELOG.md`.
- Getest: in-game voor/na-shots (enderman, husk, gewaad-mobs, mooshroom, iron golem); `gradlew build` + 147 tests
  groen op precies de commit-inhoud.
- Gepusht naar origin/master; release v0.6.1-alpha (Latest, jar erbij).
- Andere sessie: Thor/Mjolnir al in `9b3b29d` + release v0.6.0-alpha; daarom heet deze release v0.6.1-alpha.
- Open: `Ragdolls.java` > 600 regels (al van vóór) -> splitsen voorstellen; panda/dolfijn-items niet gedaan.
  Bugs/ideeën bewust niet aangeraakt.
