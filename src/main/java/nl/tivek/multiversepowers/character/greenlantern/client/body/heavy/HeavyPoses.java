package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.body.heavy.HeavyKeys.Key;
import nl.tivek.multiversepowers.engine.client.pose.Gait;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
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
    // The launcher's new rocket grows into its mouth between these ticks of a shot.
    // The launcher's reload grows its new rocket into the mouth over these ticks.
    private static final double RELOAD_FROM = 7.0;
    private static final double RELOAD_TO = 17.0;
    // Carrying it while walking and running: the dip of each step and the side swing (pixels), the weapon's swing
    // and tilt and the trunk's turn, roll and lean (radians); sprinting it is drawn in, lowered and turned across.
    private static final float BOB = 0.9F;
    private static final float BOB_SWAY = 0.7F;
    private static final float SWING = 0.06F;
    private static final float TWIST = 0.07F;
    private static final float ROLL = 0.04F;
    private static final float DIP = 0.03F;
    private static final float RUN_LEAN = 0.28F;
    private static final float RUN_ACROSS = 1.5F;
    private static final float RUN_LOW = 1.5F;
    private static final float RUN_IN = 1.5F;
    private static final float RUN_TILT = 0.12F;
    // Breathing at rest: how far the weapon rises and tips (pixels, radians) and how fast (radians a tick).
    private static final float BREATH = 0.3F;
    private static final float BREATH_TIP = 0.012F;
    private static final float BREATH_RATE = 0.09F;
    // A shot's kick for the revolvers, the cannon and the minigun: back and up (pixels) and the muzzle thrown up.
    private static final float[] KICK_BACK = { 1.0F, 2.0F, 0.5F };
    private static final float[] KICK_UP = { 0.4F, 0.5F, 0.12F };
    private static final float[] KICK_FLIP = { 0.22F, 0.15F, 0.025F };
    private static final float BUZZ = 0.25F;

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

        void set(Pose other) {
            this.middle.set(other.middle);
            this.up.set(other.up);
            this.az = other.az;
            this.el = other.el;
            this.twist = other.twist;
            this.pitch = other.pitch;
            this.roll = other.roll;
            this.drop = other.drop;
            this.weight = other.weight;
            this.shake = other.shake;
        }

        // The same pose across his middle: what the right hand holds, held in the left.
        void mirror() {
            this.middle.x = -this.middle.x;
            this.up.x = -this.up.x;
            this.az = -this.az;
            this.twist = -this.twist;
            this.roll = -this.roll;
        }

        // The weapon turned about its own across line through the middle, muzzle up and back over for +.
        void twirl(float angle) {
            if (angle == 0.0F) {
                return;
            }
            Vector3f way = this.way(new Vector3f());
            Vector3f toward = new Vector3f(Mth.sin(this.az) * Mth.sin(this.el), -Mth.cos(this.el),
                    Mth.cos(this.az) * Mth.sin(this.el));
            Vector3f axis = way.cross(toward).normalize();
            new Quaternionf().rotationAxis(angle, axis).transform(this.up);
            this.el += angle;
        }

        // The way the weapon's length points, in the chest's frame.
        Vector3f way(Vector3f out) {
            float flat = Mth.cos(this.el);
            return out.set(-Mth.sin(this.az) * flat, -Mth.sin(this.el), -Mth.cos(this.az) * flat);
        }
    }

    private static final Pose NOW = new Pose();
    private static final Pose LEFT = new Pose();
    private static final Pose BEFORE = new Pose();
    private static final Pose NEXT = new Pose();

    private HeavyPoses() {
    }

    // The weapon's pose this frame. Shared scratch: render thread only.
    static Pose of(ClientHeavy.Held held, Gait gait, float partialTick) {
        Pose pose = NOW;
        at(held.weapon, held.move, held.age(partialTick), pose);
        float age = (float) held.age(partialTick);
        if (age < FLOW) {
            at(held.weapon, held.last, held.lastAge(partialTick), BEFORE);
            BEFORE.toward(pose, (float) Ease.smooth(age / FLOW));
            pose.set(HeavyKeys.rest(held.weapon));
            pose.toward(BEFORE, 1.0F);
        }
        carry(pose, gait, held.move < 0 || held.move >= MOVES ? 1.0F : 0.35F);
        float breath = Mth.sin((float) ClientHeavy.now(partialTick) * BREATH_RATE)
                * (held.move < 0 || held.move >= MOVES ? 1.0F : 0.3F);
        pose.middle.y += BREATH * breath;
        pose.el += BREATH_TIP * breath;
        if (dual(held.weapon)) {
            LEFT.set(pose);
            LEFT.mirror();
            if (held.move == KICK) {
                NEXT.set(HeavyKeys.aimed(held.weapon));
                NEXT.mirror();
                LEFT.toward(NEXT, (float) (Ease.smooth(age / 2.0) * (1.0 - Ease.smooth((age - 9.0) / 3.0))));
            }
            kick(held, 1, age, LEFT);
            LEFT.twirl(-twirl(held, 1, age));
        }
        if (GunFire.of(held)) {
            kick(held, 0, age, pose);
            pose.twirl(-twirl(held, 0, age));
        } else if (held.weapon == RPG || held.weapon == SHOTGUN) {
            float k = (float) shotKick(held, age);
            boolean shotgun = held.weapon == SHOTGUN;
            pose.middle.add(0.0F, -(shotgun ? 0.6F : 0.3F) * k, (shotgun ? 2.0F : 1.5F) * k);
            pose.el += (shotgun ? 0.3F : 0.12F) * k;
        }
        double formed = Ease.smooth((ClientHeavy.now(partialTick) - held.formedAt) / 3.0);
        double apart = held.apart(partialTick);
        pose.weight = (float) (formed * (apart < 0.0 ? 1.0 : 1.0 - Ease.smooth(apart * 1.4)));
        pose.shake = SHAKE * (float) revving(held, partialTick) * (held.weapon == SAW ? 1.0F : 0.0F);
        if (held.weapon == MINIGUN && GunFire.last(held, 0, age) < 1.5) {
            pose.shake = BUZZ;
        }
        LEFT.weight = pose.weight;
        LEFT.shake = pose.shake;
        return pose;
    }

    // The left revolver's pose as of the last `of`, in the chest's frame: the right one's across his middle, kept
    // aimed while the right one strikes, with its own shots' kick and twirl. Shared scratch: render thread only.
    static Pose left() {
        return LEFT;
    }

    // `side`'s gun thrown back and its muzzle up by its last shots; a charged blast throws the cannon hardest.
    private static void kick(ClientHeavy.Held held, int side, float age, Pose pose) {
        float k = (float) GunFire.kick(held, side, age);
        if (k <= 0.0F) {
            return;
        }
        int gun = held.weapon - REVOLVERS;
        float big = held.weapon == CANNON ? 1.0F + 2.0F * (float) GunPainter.charged(held) : 1.0F;
        pose.middle.add(0.0F, -KICK_UP[gun] * k * big, KICK_BACK[gun] * k * big);
        pose.el += KICK_FLIP[gun] * k * big;
    }

    // The launcher's and the shotgun's kick as a shot leaves, both barrels at once kicking hardest.
    private static double shotKick(ClientHeavy.Held held, double age) {
        if (held.brokeAt >= 0.0) {
            return 0.0;
        }
        boolean shot = held.move == SHOOT || held.move == LOOSE || held.weapon == SHOTGUN && held.move == UNBRACE
                || held.weapon == RPG && held.move == BRACE;
        int hit = hit(held.weapon, held.move);
        if (!shot || hit < 0) {
            return 0.0;
        }
        double both = held.weapon == SHOTGUN && held.move == LOOSE ? 1.6 : 1.0;
        return both * Ease.jolt((age - hit) / 7.0);
    }

    // How far round a revolver is spun on its trigger finger: once as it forms, after a fan and after a reload, the
    // left one a moment behind the right.
    private static float twirl(ClientHeavy.Held held, int side, float age) {
        if (held.weapon != REVOLVERS || held.brokeAt >= 0.0) {
            return 0.0F;
        }
        float t = age - 1.5F * side;
        double turn = switch (held.move) {
            case FORM -> spun(t, 9.0F, 7.0F);
            case LOOSE -> held.last == AIM ? spun(t, 1.0F, 6.0F) : 0.0;
            case RELOAD -> -spun(t, 28.0F, 7.0F);
            default -> 0.0;
        };
        return (float) (turn % (Math.PI * 2.0));
    }

    private static double spun(float t, float from, float ticks) {
        return Math.PI * 2.0 * Ease.smooth((t - from) / ticks);
    }

    // Walking, the weapon rides his steps: it dips with each footfall, swings a little from side to side against his
    // hips and the trunk rolls with it; sprinting, he leans into the run with the weapon drawn in and lowered across
    // his body. In a move (`free` small) only a little of it shows.
    private static void carry(Pose pose, Gait gait, float free) {
        if (gait.amount() < 1.0E-3F) {
            return;
        }
        float walk = free * (1.0F - 0.5F * gait.sprint());
        float run = free * gait.sprint();
        pose.middle.add(BOB_SWAY * gait.sway() * walk, BOB * gait.bounce() * (walk + 1.6F * run), 0.0F);
        pose.middle.add(-RUN_ACROSS * run, RUN_LOW * run, RUN_IN * run);
        pose.az += SWING * gait.sway() * walk;
        pose.el += RUN_TILT * run;
        pose.twist -= TWIST * gait.sway() * (walk + run);
        pose.roll += ROLL * gait.sway() * (walk + run);
        pose.pitch += RUN_LEAN * run + DIP * gait.bounce() * walk;
        pose.drop += BOB * 0.5F * gait.bounce() * (walk + run);
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
            for (int side = 0; side < (dual(weapon) ? 1 : 2); side++) {
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
            case FORM -> window(age, 13.5, 18.0);
            case REV, REV_BACK -> window(age, 2.0, 8.0);
            case REND, GUARD -> Ease.smooth((age - 1.0) / 3.0);
            case IMPALE -> window(age, 4.0, 13.0);
            case REND_OUT, GUARD_DOWN -> 1.0 - Ease.smooth(age / 5.0);
            default -> 0.0;
        };
    }

    // How bright a gun's shot flashes at its muzzle now: a moment as it fires.
    static double flash(ClientHeavy.Held held, float partialTick) {
        if (!gun(held.weapon) || held.brokeAt >= 0.0) {
            return 0.0;
        }
        int hit = hit(held.weapon, held.move);
        boolean shot = held.move == SHOOT || held.move == LOOSE || held.weapon == SHOTGUN && held.move == UNBRACE
                || held.weapon == RPG && held.move == BRACE;
        if (!shot || hit < 0) {
            return 0.0;
        }
        double since = held.age(partialTick) - hit;
        return since < -0.5 || since > 2.5 ? 0.0 : Ease.smooth((since + 0.5) / 0.5) * (1.0 - Ease.smooth(since / 2.5));
    }

    // How much of the launcher's rocket sits in its mouth: there while it is loaded, gone once fired, grown back as
    // the reload pushes the next one in.
    static double loaded(ClientHeavy.Held held, float partialTick) {
        if (held.weapon != RPG) {
            return 1.0;
        }
        if (held.move == RELOAD) {
            return Ease.smooth((held.age(partialTick) - RELOAD_FROM) / (RELOAD_TO - RELOAD_FROM));
        }
        return held.ammo > 0 ? 1.0 : 0.0;
    }

    // How many ticks into its reload a gun is, or below 0.
    static double reload(ClientHeavy.Held held, float partialTick) {
        return gun(held.weapon) && held.move == RELOAD && held.brokeAt < 0.0 ? held.age(partialTick) : -1.0;
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
