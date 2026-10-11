import java.nio.file.Path;

// Paints the Bleeding effect's 18 pixel icon (textures/mob_effect/bleeding.png): a dark red drop with a light on its
// side, falling from a claw's three cuts. From the repository root, with Java 22 or newer:
// java scripts/models/zombies/BleedingIcon.java
public final class BleedingIcon {
    private static final int[] EDGE = { 52, 6, 8 };
    private static final int[] BLOOD = { 150, 16, 20 };
    private static final int[] DEEP = { 104, 10, 14 };
    private static final int[] SHINE = { 236, 120, 116 };
    private static final int[] CUT = { 196, 40, 40 };

    private BleedingIcon() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Paint.textures(args, "mob_effect");
        Paint p = new Paint(18, 18, 18);
        int[] all = { 0, 0, 18, 18 };
        // The drop: a point at the top widening to a round body.
        for (int y = 4; y < 17; y++) {
            double half = y < 10 ? (y - 3) * 0.6 : Math.sqrt(Math.max(0.0, 25.0 - (y - 11.5) * (y - 11.5)));
            for (int x = 0; x < 18; x++) {
                double dx = x + 0.5 - 9.0;
                if (Math.abs(dx) <= half + 0.6) {
                    boolean edge = Math.abs(dx) > half - 0.6 || y == 16;
                    p.dot(all, x, y, edge ? EDGE : dx > 1.0 || y > 13 ? DEEP : BLOOD);
                }
            }
        }
        p.dot(all, 7, 10, SHINE);
        p.dot(all, 7, 11, SHINE);
        p.dot(all, 6, 12, SHINE);
        // Three slanted cuts above it.
        for (int k = 0; k < 3; k++) {
            for (int s = 0; s < 3; s++) {
                p.dot(all, 3 + k * 5 + s, 3 - s, CUT);
            }
        }
        p.save(folder, "bleeding");
        System.out.println("painted");
    }
}
