package nl.tivek.multiversepowers.character.greenlantern.client.body.fist;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.greenlantern.client.body.fist.FistKeys.Key;
import nl.tivek.multiversepowers.character.greenlantern.client.body.fist.FistKeys.Spot;
import nl.tivek.multiversepowers.character.greenlantern.fist.FistMoves;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

// Where the fist blows put his body from moment to moment: a blow's keys eased from one to the next, each new blow
// flowing out of the one before for its first ticks, and his fists kept up in a guard between blows and for a while
// after the last one.
final class FistPoses {
    private static final float FLOW = 3.0F;
    private static final float GUARD_DOWN = 8.0F;

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
        // How much of the body the blows have: 1 in a blow or its guard, easing in and out of them.
        float weight;

        void guard() {
            for (int side = 0; side < 2; side++) {
                spot(Spot.GUARD, side, this);
            }
            this.twist = 0.0F;
            this.pitch = 0.04F;
            this.roll = 0.0F;
            this.drop = 0.6F;
        }

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
        }
    }

    private static final Pose NOW = new Pose();
    private static final Pose BEFORE = new Pose();

    private FistPoses() {
    }

    // The pose of the blows this frame, or null with the fists down. Shared scratch: render thread only.
    @Nullable
    static Pose of(ClientFists.View view, float partialTick) {
        float age = (float) view.age(partialTick);
        float after = age - FistMoves.length(view.move());
        if (after >= FistMoves.GUARD + GUARD_DOWN || age < 0.0F) {
            return null;
        }
        Pose pose = NOW;
        if (after >= 0.0F) {
            pose.guard();
            pose.weight = 1.0F - (float) Ease.smooth((after - FistMoves.GUARD) / GUARD_DOWN);
            return pose;
        }
        at(view.move(), age, pose);
        boolean flowing = view.last() >= 0 && age < FLOW;
        if (flowing) {
            float lastAge = (float) view.lastAge(partialTick);
            if (lastAge < FistMoves.length(view.last())) {
                at(view.last(), lastAge, BEFORE);
            } else {
                BEFORE.guard();
            }
            BEFORE.toward(pose, (float) Ease.smooth(age / FLOW));
            pose.guard();
            pose.toward(BEFORE, 1.0F);
        }
        pose.weight = view.last() >= 0 ? 1.0F : (float) Ease.smooth(age / 2.0F);
        return pose;
    }

    // How far round a spin has turned him, in radians.
    static float spin(ClientFists.View view, float partialTick) {
        if (view.move() != FistMoves.SPIN) {
            return 0.0F;
        }
        return (float) (Math.PI * 2.0 * Ease.smooth((view.age(partialTick) - 4.0) / 12.0));
    }

    private static void at(int move, float age, Pose out) {
        Key[] keys = FistKeys.of(move);
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
            spot(from, side, out);
            out.hand[side].lerp(mirrored(to.hand, side), u);
            out.pole[side].lerp(mirrored(to.pole, side), u);
            out.seen[side].lerp(mirrored(to.seen, side), u);
            out.from[side].lerp(mirrored(to.from, side), u);
        }
        out.twist = Mth.lerp(u, a.twist(), b.twist());
        out.pitch = Mth.lerp(u, a.pitch(), b.pitch());
        out.roll = Mth.lerp(u, a.roll(), b.roll());
        out.drop = Mth.lerp(u, a.drop(), b.drop());
    }

    private static void spot(Spot spot, int side, Pose out) {
        out.hand[side].set(mirrored(spot.hand, side));
        out.pole[side].set(mirrored(spot.pole, side));
        out.seen[side].set(mirrored(spot.seen, side));
        out.from[side].set(mirrored(spot.from, side));
    }

    // The spots are written for the right hand: the left's is the same across the middle.
    private static Vector3f mirrored(Vector3f right, int side) {
        return side == 0 ? new Vector3f(right) : new Vector3f(-right.x, right.y, right.z);
    }
}
