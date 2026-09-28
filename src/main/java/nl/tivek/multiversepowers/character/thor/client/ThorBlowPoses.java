package nl.tivek.multiversepowers.character.thor.client;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.client.ThorBlowKeys.Foot;
import nl.tivek.multiversepowers.character.thor.client.ThorBlowKeys.Key;
import nl.tivek.multiversepowers.character.thor.client.ThorBlowKeys.Script;
import nl.tivek.multiversepowers.character.thor.client.ThorBlowKeys.Spot;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

// Where Thor's blows put his body from moment to moment: a blow's keys eased from one to the next, each new blow
// flowing out of the one before for its first ticks, and his fists kept up in a guard between blows and for a while
// after the last one.
final class ThorBlowPoses {
    // Ticks a new blow takes to flow out of the one before it.
    private static final float FLOW = 2.5F;
    // After the last blow ends the guard stays up this long, then comes down over GUARD_DOWN ticks.
    private static final float GUARD_UP = 40.0F;
    private static final float GUARD_DOWN = 10.0F;
    private static final float TURN = (float) (Math.PI * 2.0);

    // One moment of a blow. Sides: 0 his right, 1 his left.
    static final class Pose {
        final Vector3f[] hand = { new Vector3f(), new Vector3f() };
        final Vector3f[] pole = { new Vector3f(), new Vector3f() };
        final Vector3f[] seen = { new Vector3f(), new Vector3f() };
        final Vector3f[] from = { new Vector3f(), new Vector3f() };
        float twist;
        float pitch;
        float roll;
        float drop;
        float spin;
        // The kicking foot (0 right, 1 left, -1 none), how far into the kick, and where it is.
        int footSide = -1;
        float kick;
        final Vector3f foot = new Vector3f();
        final Vector3f footSeen = new Vector3f();
        // How much of the body the blows have: 1 in a blow or its guard, easing in and out of them.
        float weight;

        void guard() {
            for (int side = 0; side < 2; side++) {
                spot(Spot.GUARD, side, this.hand[side], this.pole[side], this.seen[side], this.from[side]);
            }
            this.twist = 0.0F;
            this.pitch = 0.04F;
            this.roll = 0.0F;
            this.drop = 0.6F;
            this.spin = 0.0F;
            this.footSide = -1;
            this.kick = 0.0F;
        }

        // Towards another pose by u; the spin goes the short way round.
        void toward(Pose other, float u) {
            for (int side = 0; side < 2; side++) {
                this.hand[side].lerp(other.hand[side], u);
                this.pole[side].lerp(other.pole[side], u);
                this.seen[side].lerp(other.seen[side], u);
                this.from[side].lerp(other.from[side], u);
            }
            this.twist = Mth.lerp(u, this.twist, other.twist);
            this.pitch = Mth.lerp(u, this.pitch, other.pitch);
            this.roll = Mth.lerp(u, this.roll, other.roll);
            this.drop = Mth.lerp(u, this.drop, other.drop);
            this.spin += (float) Math.IEEEremainder(other.spin - this.spin, TURN) * u;
            if (other.footSide >= 0 && (this.footSide < 0 || this.footSide == other.footSide)) {
                if (this.footSide < 0) {
                    this.foot.set(other.foot);
                    this.footSeen.set(other.footSeen);
                } else {
                    this.foot.lerp(other.foot, u);
                    this.footSeen.lerp(other.footSeen, u);
                }
                this.footSide = other.footSide;
                this.kick = Mth.lerp(u, this.kick, other.kick);
            } else {
                this.kick *= 1.0F - u;
            }
        }
    }

    private static final Pose NOW = new Pose();
    private static final Pose BEFORE = new Pose();

    private ThorBlowPoses() {
    }

    // The pose of Thor's blows this frame, or null when his fists are down. Shared scratch: render thread only.
    @Nullable
    static Pose of(ClientThor.View view, float partialTick) {
        ThorBlow blow = ThorBlow.byIndex(view.blow);
        if (blow == null) {
            return null;
        }
        float age = view.blowAge(partialTick);
        float after = age - blow.ticks();
        if (after >= GUARD_UP + GUARD_DOWN) {
            return null;
        }
        Pose pose = NOW;
        if (after >= 0.0F) {
            pose.guard();
            pose.weight = 1.0F - (float) Ease.smooth((after - GUARD_UP) / GUARD_DOWN);
            return pose;
        }
        at(blow, age, pose);
        ThorBlow last = ThorBlow.byIndex(view.lastBlow);
        float lastAge = view.lastBlowAge(partialTick);
        boolean flowing = last != null && age < FLOW && lastAge < last.ticks() + GUARD_UP;
        if (flowing) {
            if (lastAge < last.ticks()) {
                at(last, lastAge, BEFORE);
            } else {
                BEFORE.guard();
            }
            BEFORE.toward(pose, (float) Ease.smooth(age / FLOW));
            copy(BEFORE, pose);
        }
        // Out of a guard already up it is all his at once; from rest it comes up over its first tick and a half.
        pose.weight = flowing ? 1.0F : (float) Ease.smooth(age / 1.5F);
        return pose;
    }

    // His whole body's turn round for a spinning blow (+ to his right), 0 when none.
    static float spin(ClientThor.View view, float partialTick) {
        Pose pose = of(view, partialTick);
        return pose == null ? 0.0F : (float) Math.IEEEremainder(pose.spin, TURN) * pose.weight;
    }

    private static void copy(Pose from, Pose to) {
        to.guard();
        to.toward(from, 1.0F);
        to.spin = from.spin;
    }

    // A blow's keys at `age`: the two keys round it eased between.
    private static void at(ThorBlow blow, float age, Pose out) {
        Script script = ThorBlowKeys.of(blow);
        Key[] keys = script.keys();
        int i = 0;
        while (i < keys.length - 2 && age >= keys[i + 1].t()) {
            i++;
        }
        Key a = keys[i];
        Key b = keys[i + 1];
        float u = (float) Ease.smooth(Mth.clamp((age - a.t()) / (b.t() - a.t()), 0.0F, 1.0F));
        for (int side = 0; side < 2; side++) {
            Spot from = side == 0 ? a.right() : a.left();
            Spot to = side == 0 ? b.right() : b.left();
            spot(from, side, out.hand[side], out.pole[side], out.seen[side], out.from[side]);
            out.hand[side].lerp(mirrored(to.hand, side), u);
            out.pole[side].lerp(mirrored(to.pole, side), u);
            out.seen[side].lerp(seenSide(to.seen, side), u);
            out.from[side].lerp(seenSide(to.from, side), u);
        }
        out.twist = Mth.lerp(u, a.twist(), b.twist());
        out.pitch = Mth.lerp(u, a.pitch(), b.pitch());
        out.roll = Mth.lerp(u, a.roll(), b.roll());
        out.drop = Mth.lerp(u, a.drop(), b.drop());
        out.spin = Mth.lerp(u, a.spin(), b.spin());
        feet(script, age, out);
    }

    // The kicking foot between its keys; the kick fades in over its first stretch and out over its last.
    private static void feet(Script script, float age, Pose out) {
        Foot[] feet = script.feet();
        out.footSide = -1;
        out.kick = 0.0F;
        if (feet.length < 3 || age <= feet[0].t() || age >= feet[feet.length - 1].t()) {
            return;
        }
        int i = 0;
        while (i < feet.length - 2 && age >= feet[i + 1].t()) {
            i++;
        }
        Foot a = feet[i];
        Foot b = feet[i + 1];
        float u = (float) Ease.smooth((age - a.t()) / (b.t() - a.t()));
        out.foot.set(a.at()).lerp(b.at(), u);
        out.footSeen.set(a.seen()).lerp(b.seen(), u);
        out.footSide = script.rightFoot() ? 0 : 1;
        if (i == 0) {
            out.kick = u;
        } else if (i == feet.length - 2) {
            out.kick = 1.0F - u;
        } else {
            out.kick = 1.0F;
        }
    }

    private static void spot(Spot spot, int side, Vector3f hand, Vector3f pole, Vector3f seen, Vector3f from) {
        hand.set(mirrored(spot.hand, side));
        pole.set(mirrored(spot.pole, side));
        seen.set(seenSide(spot.seen, side));
        from.set(seenSide(spot.from, side));
    }

    // The spots are written for the right hand: the left's is the same across the middle.
    private static Vector3f mirrored(Vector3f right, int side) {
        return side == 0 ? new Vector3f(right) : new Vector3f(-right.x, right.y, right.z);
    }

    private static Vector3f seenSide(Vector3f right, int side) {
        return side == 0 ? new Vector3f(right) : new Vector3f(-right.x, right.y, right.z);
    }
}
