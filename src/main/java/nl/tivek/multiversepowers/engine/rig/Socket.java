package nl.tivek.multiversepowers.engine.rig;

// A fixed place on a bone (in the bone's own frame, model units): where a weapon, a caught creature or a seat hangs.
public record Socket(int bone, double x, double y, double z) {
    public static Socket on(Rig rig, String bone, double x, double y, double z) {
        return new Socket(rig.index(bone), x, y, z);
    }
}
