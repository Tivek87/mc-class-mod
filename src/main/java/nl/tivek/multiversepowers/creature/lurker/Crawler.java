package nl.tivek.multiversepowers.creature.lurker;

// A zombie that runs on all fours (the Lurker, the Screamer's ferals): drawn by LurkerModel, which needs to know
// whether it is creeping up unseen and how long ago it began to climb out of the ground.
public interface Crawler {
    // Creeping low and slow, unheard.
    boolean stalking();

    // How far it has climbed out of the ground, 0 (under it) to 1 (out).
    float risen(float partialTick);
}
