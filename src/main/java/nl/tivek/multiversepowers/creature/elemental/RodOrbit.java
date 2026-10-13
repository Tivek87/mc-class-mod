package nl.tivek.multiversepowers.creature.elemental;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;

// Where each of the Elemental Blaze's fifteen rods is at a moment, in its model's pixels (y down, its face towards -z):
// three rings of five as a blaze has, every ring holding one rod of each element and turning its own way. Its moves
// bend the rings (gathered tight for a fireball, sunk low as it heaves a stone, flung wide as it awakens, whirled for a
// gust, crackling for lightning), and the upper rod of the element it casts swings round to the front, pointing at its
// foe, until the move lets it go. Dying, the rods fly apart and fall. The same in the model and in the particles its
// rods shed.
public final class RodOrbit {
    public static final int RINGS = 3;
    public static final int RODS = RINGS * Element.ALL.length;
    private static final float[] RADIUS = { 9.0F, 7.0F, 5.0F };
    private static final float[] HEIGHT = { 2.0F, 6.0F, 14.0F };
    private static final float[] SPEED = { -0.1F * Mth.PI, 0.03F * Mth.PI, -0.05F * Mth.PI };
    private static final float[] TILT = { 0.22F, 0.16F, 0.1F };
    private static final float[] SWAY = { 0.021F, -0.033F, 0.027F };
    private static final float FRONT = 11.0F;
    // Tipped so the rod's lower end points ahead.
    private static final float POINT = -Mth.HALF_PI * 0.9F;

    // How it is at a moment: its age, its move and how far in, how long since it awoke (below 0 not yet), how far it
    // is into dying and how hard it was just hit (0 to 1).
    public record State(float age, int move, float moveAge, float awake, float dying, float hurt) {
    }

    private RodOrbit() {
    }

    public static int ring(int rod) {
        return rod / Element.ALL.length;
    }

    public static Element element(int rod) {
        return Element.of(rod % Element.ALL.length + ring(rod) * 2);
    }

    // The rod's place and turn: x, y, z, then its turns about x, y and z.
    public static void place(State s, int rod, float[] out) {
        int ring = ring(rod);
        int i = rod % Element.ALL.length;
        Element own = element(rod);
        float spacing = Mth.TWO_PI / Element.ALL.length;
        float angle = SPEED[ring] * s.age() + i * spacing + ring * 0.6F;
        float radius = RADIUS[ring];
        float y = HEIGHT[ring] + Mth.cos((i * 2 + s.age()) * 0.25F) * (ring == 2 ? 0.6F : 1.0F);
        float xRot = 0.0F;
        float zRot = 0.0F;
        // Each ring breathes and lies tilted, its tilt slowly turning round, so no two rods ever move alike.
        radius *= 1.0F + 0.05F * Mth.sin(s.age() * 0.11F + ring * 2.1F);
        float tilt = TILT[ring] * Mth.sin(angle - s.age() * SWAY[ring] + ring * 1.9F);
        y += tilt * radius;
        zRot += tilt * 0.7F;
        if (s.awake() >= 0.0F) {
            angle += Math.signum(SPEED[ring]) * s.awake() * 0.06F;
            float fierce = (float) Ease.smooth(s.awake() / BlazeTimeline.AWAKEN_END);
            radius *= 1.0F - 0.08F * fierce;
            zRot += 0.12F * fierce;
            y += Mth.sin(s.age() * 0.31F + i) * 0.6F * fierce;
        }
        float t = s.moveAge();
        Element casting = BlazeTimeline.element(s.move());
        if (s.move() == BlazeTimeline.AWAKEN) {
            float spread = t < BlazeTimeline.BURST ? (float) Ease.smooth(t / BlazeTimeline.BURST)
                    : 1.0F - (float) Ease.smooth((t - BlazeTimeline.BURST) / 10.0);
            radius *= 1.0F + 0.9F * spread;
            y -= 5.0F * spread * (ring == 0 ? 1.0F : 0.6F);
            angle += Mth.TWO_PI * 2.0F * (float) Ease.smoother(t / BlazeTimeline.AWAKEN_END) * Math.signum(SPEED[ring]);
            zRot += 0.5F * spread;
            radius *= 1.0F - 0.25F * (float) Ease.jolt((t - BlazeTimeline.BURST) / 8.0);
        } else if (casting != null) {
            float c = t - BlazeTimeline.shift(s.move());
            float into = c / BlazeTimeline.cast(casting);
            if (own != casting) {
                // The other elements draw in and bow away, making room for the one that casts.
                float brace = brace(s);
                radius *= 1.0F - 0.12F * brace;
                zRot -= 0.22F * brace;
                y += 1.2F * brace * (ring == 2 ? -1.0F : 1.0F);
            }
            switch (casting) {
                case FIRE -> {
                    float gather = c < 0.0F ? 0.0F : (float) Ease.smooth(into);
                    if (c >= BlazeTimeline.cast(casting)) {
                        gather = 0.0F;
                    }
                    float burst = (float) Ease.jolt((c - BlazeTimeline.cast(casting)) / 10.0);
                    radius *= 1.0F - 0.4F * gather + 0.5F * burst;
                }
                case ICE -> {
                    float held = c < 0.0F ? 0.0F : c < BlazeTimeline.BEAM ? (float) Ease.smooth(c / 4.0)
                            : 1.0F - (float) Ease.smooth((c - BlazeTimeline.BEAM) / 4.0);
                    radius *= 1.0F - 0.15F * held;
                    zRot += 0.25F * held * Mth.sin(i * 1.7F);
                    xRot += 0.04F * held * Mth.sin(s.age() * 2.3F + rod);
                }
                case EARTH -> {
                    float lift = c < 0.0F ? 0.0F : c < BlazeTimeline.LIFT ? (float) Ease.smooth(c / BlazeTimeline.LIFT)
                            : 1.0F - (float) Ease.smooth((c - BlazeTimeline.LIFT) / 4.0);
                    y += 4.0F * lift;
                    radius *= 1.0F + 0.15F * lift;
                    y -= 3.0F * (float) Ease.jolt((c - BlazeTimeline.LIFT) / 6.0);
                }
                case STORM -> {
                    float crackle = c < -4.0F || c > BlazeTimeline.cast(casting) + 2 ? 0.0F : 0.7F;
                    radius += crackle * Mth.sin(s.age() * 5.3F + rod * 2.1F);
                    y += crackle * Mth.cos(s.age() * 4.7F + rod * 1.3F);
                    radius *= 1.0F + 0.3F * (float) Ease.jolt((c - BlazeTimeline.cast(casting)) / 7.0);
                }
                case WIND -> {
                    angle += Mth.TWO_PI * (float) Ease.smoother(c / BlazeTimeline.end(casting))
                            * Math.signum(SPEED[ring]);
                    radius *= 1.0F + 0.35F * (float) Ease.bump((c - BlazeTimeline.cast(casting)) / 8.0);
                }
            }
        }
        radius *= 1.0F + 0.18F * s.hurt();
        float front = ring == 0 && own == casting ? front(s) : 0.0F;
        if (front > 0.0F) {
            // Swung to the front the shorter way round from where it was as the move began, so it never jumps.
            float started = angle - SPEED[ring] * t;
            float toward = started + Mth.wrapDegrees((-Mth.HALF_PI - started) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            angle = Mth.lerp(front, angle, toward);
            radius = Mth.lerp(front, radius, FRONT);
            y = Mth.lerp(front, y, -1.0F);
            xRot = Mth.lerp(front, xRot, POINT);
        }
        float x = Mth.cos(angle) * radius;
        float z = Mth.sin(angle) * radius;
        if (front > 0.0F && casting != null) {
            float c = t - BlazeTimeline.shift(s.move());
            z -= 3.0F * front * (float) Ease.jolt((c - BlazeTimeline.cast(casting)) / 6.0);
            if (casting == Element.ICE && c >= 0.0F && c < BlazeTimeline.BEAM) {
                x += Mth.sin(s.age() * 2.7F) * 0.25F;
                y += Mth.cos(s.age() * 3.1F) * 0.2F;
            }
        }
        if (s.dying() > 0.0F) {
            float d = Mth.clamp(s.dying() / 20.0F, 0.0F, 1.0F);
            x *= 1.0F + 1.6F * d;
            z *= 1.0F + 1.6F * d;
            y += 18.0F * d * d;
            xRot += d * 3.0F * (i + 1);
            zRot += d * 2.0F * (ring + 1);
        }
        out[0] = x;
        out[1] = y;
        out[2] = z;
        out[3] = xRot;
        // Turning about its own length too, faster as it wakes; the rod that points at the foe holds still.
        float spin = s.age() * (ring == 1 ? -0.07F : 0.05F) * (s.awake() >= 0.0F ? 1.8F : 1.0F);
        out[4] = -angle - Mth.HALF_PI + spin * (1.0F - front);
        out[5] = zRot;
    }

    // How far it is braced for the cast to come (0 to 1): drawn in while the rod shifts, let go as it casts.
    public static float brace(State s) {
        if (BlazeTimeline.element(s.move()) == null) {
            return 0.0F;
        }
        float shift = BlazeTimeline.shift(s.move());
        float c = s.moveAge() - shift;
        return c < 0.0F ? (float) Ease.smooth(s.moveAge() / shift) : 1.0F - (float) Ease.smooth(c / 5.0);
    }

    // The jolt of the cast itself (0 to 1): whole as the spell leaves, gone a few ticks on.
    public static float release(State s) {
        Element casting = BlazeTimeline.element(s.move());
        if (s.move() == BlazeTimeline.AWAKEN) {
            return (float) Ease.jolt((s.moveAge() - BlazeTimeline.BURST) / 10.0);
        }
        return casting == null ? 0.0F
                : (float) Ease.jolt((s.moveAge() - BlazeTimeline.shift(s.move()) - BlazeTimeline.cast(casting)) / 8.0);
    }

    // How far the move's rod is swung to the front (0 to 1): round while it shifts, held while it casts, back in its
    // ring over the move's last ticks.
    public static float front(State s) {
        Element casting = BlazeTimeline.element(s.move());
        if (casting == null) {
            return 0.0F;
        }
        float t = s.moveAge();
        float shift = BlazeTimeline.shift(s.move());
        float back = shift + BlazeTimeline.end(casting) - BlazeTimeline.RETURN;
        if (t < shift) {
            return (float) Ease.smooth(t / shift);
        }
        return 1.0F - (float) Ease.smooth((t - back) / BlazeTimeline.RETURN);
    }

    // How brightly the rods of `element` glow now: the upper one as it swings to the front, the others half as much,
    // all of them flaring as it casts.
    public static float glow(State s, Element element, int ring) {
        Element casting = BlazeTimeline.element(s.move());
        if (s.move() == BlazeTimeline.AWAKEN) {
            return 0.6F * (float) Ease.bump((s.moveAge() - BlazeTimeline.BURST) / 14.0);
        }
        if (casting != element) {
            return 0.0F;
        }
        float c = s.moveAge() - BlazeTimeline.shift(s.move());
        float flare = (float) Ease.jolt((c - BlazeTimeline.cast(casting)) / 8.0);
        return Mth.clamp(front(s) * (ring == 0 ? 1.0F : 0.5F) + 0.6F * flare, 0.0F, 1.0F);
    }

    // How far its head is thrown back now (radians): a kick as it casts, back as it awakens.
    public static float headKick(State s) {
        Element casting = BlazeTimeline.element(s.move());
        if (s.move() == BlazeTimeline.AWAKEN) {
            float t = s.moveAge();
            return 0.6F * (t < BlazeTimeline.BURST ? (float) Ease.smooth(t / BlazeTimeline.BURST)
                    : 1.0F - (float) Ease.smooth((t - BlazeTimeline.BURST) / 10.0));
        }
        if (casting == null) {
            return 0.0F;
        }
        float c = s.moveAge() - BlazeTimeline.shift(s.move());
        return 0.35F * (float) Ease.jolt((c - BlazeTimeline.cast(casting)) / 7.0);
    }

    // Where the rod is in the world, for its blaze standing at `at` with its body turned `yaw` degrees.
    public static Vec3 world(float[] rod, Vec3 at, float yaw) {
        double x = -rod[0] / 16.0;
        double y = 1.501 - rod[1] / 16.0;
        double z = rod[2] / 16.0;
        double turn = Math.toRadians(180.0 - yaw);
        double cos = Math.cos(turn);
        double sin = Math.sin(turn);
        return at.add(x * cos + z * sin, y, -x * sin + z * cos);
    }

    // The rod that leads its move's cast, or -1.
    public static int leader(@Nullable Element casting) {
        return casting == null ? -1 : casting.ordinal();
    }
}
