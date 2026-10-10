package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.body.heavy.HeavyKeys.Key;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// Where a heavy weapon and the body holding it are from moment to moment: a move's keys eased from one to the next,
// each move flowing out of the one before for its first ticks, held moves going round their loop, and the weapon at
// rest between moves.
final class HeavyPoses {
    private static final float FLOW = 3.0F;
    // Ticks a whirlwind takes for one turn, and to come to a stop once let go.
    static final double TURN = 10.0;
    private static final double STOP = 6.0;
    // How far a revving chainsaw shakes his hands, in pixels.
    private static final float SHAKE = 0.35F;
    // How far from its shoulder a hand holds the weapon at most, in pixels: the arm all but straight.
    private static final float REACH = 11.0F;

    static final class Pose {
        final Vector3f middle = new Vector3f();
        final Vector3f up = new Vector3f();
        float az;
        float el;
        float twist;
        float pitch;
        float roll;
        float drop;
        // How much of the body the weapon has: 1 while it is whole, easing in as it forms and out as it breaks.
        float weight;
        float shake;

        void set(Key key) {
            this.middle.set(key.gx(), key.gy(), key.gz());
            this.up.set(key.ux(), key.uy(), key.uz());
            this.az = key.az();
            this.el = key.el();
            this.twist = key.twist();
            this.pitch = key.pitch();
            this.roll = key.roll();
            this.drop = key.drop();
        }

        void toward(Pose other, float u) {
            this.middle.lerp(other.middle, u);
            this.up.lerp(other.up, u);
            this.az = Mth.lerp(u, this.az, other.az);
            this.el = Mth.lerp(u, this.el, other.el);
            this.twist = Mth.lerp(u, this.twist, other.twist);
            this.pitch = Mth.lerp(u, this.pitch, other.pitch);
            this.roll = Mth.lerp(u, this.roll, other.roll);
            this.drop = Mth.lerp(u, this.drop, other.drop);
        }

        // The way the weapon's length points, in the chest's frame.
        Vector3f way(Vector3f out) {
            float flat = Mth.cos(this.el);
            return out.set(-Mth.sin(this.az) * flat, -Mth.sin(this.el), -Mth.cos(this.az) * flat);
        }
    }

    private static final Pose NOW = new Pose();
    private static final Pose BEFORE = new Pose();
    private static final Pose NEXT = new Pose();

    private HeavyPoses() {
    }

    // The weapon's pose this frame. Shared scratch: render thread only.
    static Pose of(ClientHeavy.Held held, float partialTick) {
        Pose pose = NOW;
        at(held.weapon, held.move, held.age(partialTick), pose);
        float age = (float) held.age(partialTick);
        if (age < FLOW) {
            at(held.weapon, held.last, held.lastAge(partialTick), BEFORE);
            BEFORE.toward(pose, (float) Ease.smooth(age / FLOW));
            pose.set(HeavyKeys.rest(held.weapon));
            pose.toward(BEFORE, 1.0F);
        }
        double formed = Ease.smooth((ClientHeavy.now(partialTick) - held.formedAt) / 3.0);
        double apart = held.apart(partialTick);
        pose.weight = (float) (formed * (apart < 0.0 ? 1.0 : 1.0 - Ease.smooth(apart * 1.4)));
        pose.shake = SHAKE * (float) revving(held, partialTick) * (held.weapon == SAW ? 1.0F : 0.0F);
        return pose;
    }

    // `move` of `weapon`, `age` ticks in; the weapon at rest for none, or past a move's end.
    static void at(int weapon, int move, double age, Pose out) {
        if (move < 0 || move >= MOVES) {
            out.set(HeavyKeys.rest(weapon));
            return;
        }
        float t = (float) looped(weapon, move, age);
        Key[] keys = HeavyKeys.of(weapon, move);
        int i = 0;
        while (i < keys.length - 2 && t >= keys[i + 1].t()) {
            i++;
        }
        Key a = keys[i];
        Key b = keys[i + 1];
        float u = (float) Ease.smooth(Mth.clamp((t - a.t()) / Math.max(1.0E-3F, b.t() - a.t()), 0.0F, 1.0F));
        out.set(a);
        NEXT.set(b);
        out.toward(NEXT, u);
        reach(weapon, out);
    }

    // The weapon drawn in toward his shoulders until both hands reach it, its way kept.
    private static void reach(int weapon, Pose pose) {
        Vector3f way = pose.way(new Vector3f());
        Vector3f up = new Vector3f(pose.up).sub(new Vector3f(way).mul(pose.up.dot(way)));
        if (up.lengthSquared() < 1.0E-6F) {
            return;
        }
        up.normalize();
        Vector3f across = new Vector3f(up).cross(way);
        Vec3 middle = HeavyPainter.middle(weapon);
        for (int pass = 0; pass < 3; pass++) {
            Vector3f pull = new Vector3f();
            float most = 0.0F;
            for (int side = 0; side < 2; side++) {
                Vec3 grip = HeavyPainter.grip(weapon, side == 0).subtract(middle)
                        .scale(16.0 * HeavyPainter.size(weapon));
                Vector3f hand = new Vector3f(pose.middle).add(new Vector3f(across).mul((float) grip.x))
                        .add(new Vector3f(up).mul((float) grip.y)).add(new Vector3f(way).mul((float) grip.z));
                Vector3f shoulder = new Vector3f(side == 0 ? -Stance.SHOULDER_X : Stance.SHOULDER_X,
                        Stance.SHOULDER_Y, 0.0F);
                float far = hand.distance(shoulder) - REACH;
                if (far > most) {
                    most = far;
                    pull.set(shoulder).sub(hand).normalize().mul(far);
                }
            }
            if (most <= 0.0F) {
                return;
            }
            pose.middle.add(pull);
        }
    }

    // How hard the chainsaw revs, 0 idling to 1 cutting.
    static double revving(ClientHeavy.Held held, float partialTick) {
        if (held.weapon != SAW || held.brokeAt >= 0.0) {
            return 0.0;
        }
        double age = held.age(partialTick);
        return switch (held.move) {
            case FORM -> window(age, 5.0, 10.0);
            case REV, REV_BACK -> window(age, 2.0, 8.0);
            case REND, GUARD -> Ease.smooth((age - 1.0) / 3.0);
            case IMPALE -> window(age, 4.0, 13.0);
            case REND_OUT, GUARD_DOWN -> 1.0 - Ease.smooth(age / 5.0);
            default -> 0.0;
        };
    }

    private static double window(double age, double from, double to) {
        return Ease.smooth((age - from) / 1.5) * (1.0 - Ease.smooth((age - to) / 2.0));
    }

    // How far round a whirlwind has turned him, in radians: on and on while held, then on to a whole turn.
    static float spin(ClientHeavy.Held held, float partialTick) {
        if (held.weapon != AXE) {
            return 0.0F;
        }
        if (held.move == WHIRL) {
            return (float) turned(held.age(partialTick));
        }
        if (held.move == WHIRL_OUT && held.last == WHIRL) {
            double from = turned(held.start - held.lastStart) % (Math.PI * 2.0);
            return (float) Mth.lerp(Ease.smooth(held.age(partialTick) / STOP), from, Math.PI * 2.0);
        }
        return 0.0F;
    }

    private static double turned(double age) {
        double lead = Math.max(0.0, age - LOOP_FROM + 2.0);
        return Math.PI * 2.0 * lead / TURN;
    }
}
