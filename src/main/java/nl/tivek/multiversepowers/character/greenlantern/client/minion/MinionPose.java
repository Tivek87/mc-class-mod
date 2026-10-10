package nl.tivek.multiversepowers.character.greenlantern.client.minion;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.minion.MechMinion;
import nl.tivek.multiversepowers.character.greenlantern.minion.MinionMoves;
import nl.tivek.multiversepowers.engine.math.Ease;

// How a helper stands this frame, from its walk and the move it makes (MinionMoves); each joint's turn in radians,
// index 0 its right: legs swung ahead (-) with knees bent back (+), arms swung ahead (-) and spread out (+) with elbows
// bent ahead (-); its hips sunk, its trunk twisted over the waist (its right shoulder back, +) and leant ahead, its head
// turned to where it looks.
final class MinionPose {
    private static final double WALK = 0.6662;
    private static final double LEG = 1.42;
    private static final double SHOULDER_Y = 2.78;
    // Coming up out of the ground takes RISE ticks; a landing gives LAND, the first out of the hatch (within
    // HERO_WITHIN ticks of coming) HERO.
    static final double RISE = 36.0;
    private static final double LAND = 9.0;
    private static final double HERO = 20.0;
    private static final double HERO_WITHIN = 60.0;

    double sink;
    double twist;
    double lean;
    double look;
    // Its head bowed (+) or tipped back.
    double nod;
    // How hard its feet burn, 0 to 1.
    double thrust;
    final double[] hip = new double[2];
    final double[] knee = new double[2];
    final double[] arm = new double[2];
    final double[] spread = new double[2];
    final double[] elbow = new double[2];
    // Where its cannon's mouth is drawn, for its light.
    @Nullable
    Vec3 muzzle;

    private MinionPose() {
    }

    static MinionPose of(ClientLevel level, MechMinion minion, float partialTick) {
        MinionPose pose = new MinionPose();
        double time = minion.tickCount + partialTick;
        pose.walk(minion, partialTick);
        pose.idle(minion, time);
        float body = Mth.rotLerp(partialTick, minion.yBodyRotO, minion.yBodyRot);
        float head = Mth.rotLerp(partialTick, minion.yHeadRotO, minion.yHeadRot);
        pose.look = Math.toRadians(Mth.clamp(Mth.wrapDegrees(head - body), -60.0F, 60.0F));
        int move = minion.move();
        double since = minion.since(partialTick);
        boolean own = move == MinionMoves.LEAP || move == MinionMoves.SLAM;
        if (!own) {
            pose.air(minion.air(partialTick), since, time);
            pose.land(minion.landed(partialTick), minion.came() == MechMinion.HATCH
                    && since - minion.landed(partialTick) < HERO_WITHIN);
        }
        if (minion.came() == MechMinion.GROUND && since < RISE) {
            pose.rise(since / RISE);
        }
        double age = minion.moveAge(partialTick);
        switch (move) {
            case MinionMoves.PUNCH -> pose.punch(age);
            case MinionMoves.CANNON -> pose.cannon(level, minion, age, partialTick);
            case MinionMoves.LEAP -> pose.leap(age);
            case MinionMoves.SLAM -> pose.slam(age);
            default -> {
            }
        }
        if (own) {
            pose.thrust = Math.max(pose.thrust, minion.air(partialTick));
        }
        pose.flinch(minion, partialTick);
        pose.worn(minion.cracks(), time);
        return pose;
    }

    // Standing still it breathes: its chest rises and falls, its arms sway a little, out of step with any other.
    private void idle(MechMinion minion, double time) {
        double still = 1.0 - Math.min(1.0, minion.walkAnimation.speed());
        double breath = Math.sin(time * 0.09 + minion.getId());
        this.lean += 0.03 * still * breath;
        this.sink += 0.025 * still * (1.0 + breath);
        this.nod -= 0.04 * still * breath;
        for (int s = 0; s < 2; s++) {
            double sway = Math.sin(time * 0.07 + minion.getId() * 1.7 + s * 2.1);
            this.arm[s] += 0.05 * still * sway;
            this.spread[s] += 0.03 * still * (1.0 + sway);
        }
    }

    // Off the ground: dropped out of the hatch it falls curled up and unfolds; in the air its arms spread for balance,
    // one knee drawn up and the other leg trailing, its feet burning to hold it steady.
    private void air(double air, double since, double time) {
        if (air <= 0.0) {
            return;
        }
        double open = Ease.smooth((since - 2.0) / 7.0);
        double wobble = Math.sin(time * 0.5);
        double[] arm = { Mth.lerp(open, -1.1, -0.35 + 0.08 * wobble), Mth.lerp(open, -1.1, -0.45 - 0.08 * wobble) };
        double[] spread = { Mth.lerp(open, -0.35, 1.05), Mth.lerp(open, -0.35, 1.15) };
        double[] elbow = { Mth.lerp(open, -2.0, -0.55), Mth.lerp(open, -2.0, -0.7) };
        double[] hip = { Mth.lerp(open, -1.7, -0.95), Mth.lerp(open, -1.7, 0.3) };
        double[] knee = { Mth.lerp(open, 2.3, 1.45), Mth.lerp(open, 2.3, 0.55) };
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(air, this.arm[s], arm[s]);
            this.spread[s] = Mth.lerp(air, this.spread[s], spread[s]);
            this.elbow[s] = Mth.lerp(air, this.elbow[s], elbow[s]);
            this.hip[s] = Mth.lerp(air, this.hip[s], hip[s]);
            this.knee[s] = Mth.lerp(air, this.knee[s], knee[s]);
        }
        this.lean += air * Mth.lerp(open, 0.55, -0.12 + 0.04 * wobble);
        this.nod += air * Mth.lerp(open, 0.45, -0.1);
        this.sink -= air * 0.1;
        this.thrust = air * open;
    }

    // Come down: it gives at the knees and comes up again. Its first landing out of the hatch is a deep three-point
    // landing, the right fist on the ground and the left arm thrown back, its head down a moment before it looks up.
    private void land(double ago, boolean hero) {
        double length = hero ? HERO : LAND;
        if (ago < 0.0 || ago >= length) {
            return;
        }
        double w = Ease.smooth(ago / 2.0) * (1.0 - Ease.smooth((ago - length * 0.45) / (length * 0.55)));
        if (!hero) {
            this.crouch(0.35 * w);
            this.lean += 0.15 * w;
            for (int s = 0; s < 2; s++) {
                this.spread[s] += 0.35 * w;
            }
            return;
        }
        this.crouch(0.95 * w);
        this.lean += 0.55 * w;
        this.nod += 0.35 * w * (1.0 - Ease.smooth((ago - length * 0.3) / 4.0)) - 0.15 * w;
        this.hip[0] -= 0.25 * w;
        this.knee[1] += 0.35 * w;
        this.arm[0] = Mth.lerp(w, this.arm[0], -0.55);
        this.elbow[0] = Mth.lerp(w, this.elbow[0], -0.05);
        this.spread[0] = Mth.lerp(w, this.spread[0], 0.12);
        this.arm[1] = Mth.lerp(w, this.arm[1], 0.75);
        this.spread[1] = Mth.lerp(w, this.spread[1], 0.55);
        this.elbow[1] = Mth.lerp(w, this.elbow[1], -0.25);
    }

    // Up out of the ground: it rises from a kneel, its head bowed and its right fist on the ground, stands, straightens
    // and looks up.
    private void rise(double at) {
        double kneel = 1.0 - Ease.smooth((at - 0.35) / 0.5);
        this.crouch(0.95 * kneel);
        this.lean += 0.5 * kneel;
        this.nod += 0.45 * (1.0 - Ease.smooth((at - 0.55) / 0.35));
        this.hip[0] -= 0.3 * kneel;
        this.arm[0] = Mth.lerp(kneel, this.arm[0], -0.6);
        this.elbow[0] = Mth.lerp(kneel, this.elbow[0], -0.1);
        this.arm[1] = Mth.lerp(kneel, this.arm[1], -0.4);
        this.elbow[1] = Mth.lerp(kneel, this.elbow[1], -1.5);
        double flex = Ease.bump((at - 0.82) / 0.18);
        for (int s = 0; s < 2; s++) {
            this.spread[s] += 0.35 * flex;
            this.elbow[s] -= 0.6 * flex;
        }
        this.lean -= 0.1 * flex;
    }

    // Hit: it rocks back from the blow, its head snapping back and its arms thrown out.
    private void flinch(MechMinion minion, float partialTick) {
        if (minion.hurtTime <= 0 || minion.deathTime > 0) {
            return;
        }
        double h = Ease.smooth((minion.hurtTime - partialTick) / 10.0);
        double side = (minion.getId() & 1) == 0 ? 1.0 : -1.0;
        this.lean -= 0.3 * h;
        this.twist += 0.25 * side * h;
        this.nod -= 0.3 * h;
        this.sink += 0.08 * h;
        for (int s = 0; s < 2; s++) {
            this.arm[s] += 0.35 * h;
            this.spread[s] += 0.3 * h;
        }
    }

    // Cracked it hunches, and badly cracked it twitches now and then.
    private void worn(int cracks, double time) {
        if (cracks == 0) {
            return;
        }
        this.lean += 0.04 * cracks;
        this.sink += 0.03 * cracks;
        if (cracks >= 3) {
            double twitch = Math.max(0.0, Math.sin(time * 0.9) * Math.sin(time * 0.23)) - 0.6;
            this.nod += Math.max(0.0, twitch) * 0.5;
        }
    }

    // A heavy stride: the legs swing in turn, the knee of the one coming forward bent, the arms swing against them and
    // the body dips as the legs spread.
    private void walk(MechMinion minion, float partialTick) {
        double at = minion.walkAnimation.position(partialTick) * WALK;
        double speed = Math.min(1.0, minion.walkAnimation.speed(partialTick));
        double swing = Math.sin(at);
        this.hip[0] = -0.7 * speed * swing;
        this.hip[1] = 0.7 * speed * swing;
        this.knee[0] = 0.8 * speed * Math.max(0.0, Math.cos(at));
        this.knee[1] = 0.8 * speed * Math.max(0.0, -Math.cos(at));
        this.arm[0] = 0.45 * speed * swing;
        this.arm[1] = -0.45 * speed * swing;
        this.spread[0] = 0.08;
        this.spread[1] = 0.08;
        this.elbow[0] = -0.3 - 0.2 * speed;
        this.elbow[1] = -0.3 - 0.2 * speed;
        this.sink = 0.2 * speed * Math.abs(swing);
    }

    // Sinks its hips by `by`, bending both legs so its feet stay where they are.
    private void crouch(double by) {
        if (by <= 0.0) {
            return;
        }
        double bend = Math.acos(Mth.clamp((LEG - by) / LEG, -1.0, 1.0));
        for (int s = 0; s < 2; s++) {
            this.hip[s] -= bend;
            this.knee[s] += 2.0 * bend;
        }
        this.sink += by;
    }

    // Its fists up on guard, the left jabbing straight out at JAB, the right hooking round from the side at HOOK.
    private void punch(double age) {
        double guard = Ease.smooth(age / 2.0) * (1.0 - Ease.smooth((age - MinionMoves.PUNCH_END + 3.0) / 3.0));
        double jab = bump(age, 1.0, MinionMoves.JAB, MinionMoves.JAB + 4.0);
        double hook = bump(age, MinionMoves.JAB + 1.0, MinionMoves.HOOK, MinionMoves.PUNCH_END);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(guard, this.arm[s], -0.5);
            this.elbow[s] = Mth.lerp(guard, this.elbow[s], -1.7);
            this.spread[s] = Mth.lerp(guard, this.spread[s], 0.15);
        }
        this.arm[1] = Mth.lerp(jab, this.arm[1], -1.5);
        this.elbow[1] = Mth.lerp(jab, this.elbow[1], -0.05);
        double across = 0.6 - 0.9 * Ease.smooth((age - MinionMoves.HOOK + 3.0) / 3.0);
        this.arm[0] = Mth.lerp(hook, this.arm[0], -1.3);
        this.spread[0] = Mth.lerp(hook, this.spread[0], across);
        this.elbow[0] = Mth.lerp(hook, this.elbow[0], -0.9);
        this.twist += 0.35 * jab - 0.45 * hook;
        this.lean += 0.12 * Math.max(jab, hook);
        this.crouch(0.12 * Math.max(jab, hook));
    }

    // The right arm held straight out at its target, kicked up as the bolt leaves; the left braced at its chest.
    private void cannon(ClientLevel level, MechMinion minion, double age, float partialTick) {
        double held = Ease.smooth(age / 4.0) * (1.0 - Ease.smooth((age - MinionMoves.CANNON_END + 5.0) / 5.0));
        double pitch = 0.0;
        Entity target = level.getEntity(minion.aimed());
        if (target != null) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(minion.getPosition(partialTick)
                    .add(0.0, SHOULDER_Y, 0.0));
            pitch = Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z));
        }
        double kick = Ease.jolt((age - MinionMoves.FIRE) / 5.0);
        this.arm[0] = Mth.lerp(held, this.arm[0], -Math.PI / 2.0 - pitch - 0.35 * kick);
        this.elbow[0] = Mth.lerp(held, this.elbow[0], 0.0);
        this.spread[0] = Mth.lerp(held, this.spread[0], 0.0);
        this.arm[1] = Mth.lerp(held, this.arm[1], -0.5);
        this.elbow[1] = Mth.lerp(held, this.elbow[1], -1.4);
        this.lean -= 0.08 * kick;
        this.crouch(0.15 * held);
    }

    // Crouched with its arms swung back, it springs with them thrown up over its head and its legs tucked.
    private void leap(double age) {
        double down = Ease.smooth(age / MinionMoves.SPRING) * (1.0 - Ease.smooth((age - MinionMoves.SPRING) / 2.0));
        double up = Ease.smooth((age - MinionMoves.SPRING) / 3.0);
        this.lean += 0.3 * down - 0.1 * up;
        this.crouch(0.5 * down);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(up, Mth.lerp(down, this.arm[s], 0.9), -2.7);
            this.elbow[s] = Mth.lerp(up, this.elbow[s], -0.4);
            this.hip[s] = Mth.lerp(up, this.hip[s], -0.6);
            this.knee[s] = Mth.lerp(up, this.knee[s], 1.0);
        }
    }

    // Landed: both fists hammered down from over its head into the ground before it, bent deep over them.
    private void slam(double age) {
        double hit = 1.0 - Ease.smooth(age / MinionMoves.SLAM_END);
        double down = Ease.smooth(age / 2.0);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(hit, this.arm[s], Mth.lerp(down, -2.7, -0.7));
            this.elbow[s] = Mth.lerp(hit, this.elbow[s], -0.2);
            this.spread[s] = Mth.lerp(hit, this.spread[s], 0.05);
        }
        this.lean += 0.5 * hit;
        this.crouch(0.6 * hit);
    }

    // Up from `from` to whole at `peak`, down again by `to`.
    private static double bump(double t, double from, double peak, double to) {
        if (t <= from || t >= to) {
            return 0.0;
        }
        return t < peak ? Ease.smooth((t - from) / (peak - from)) : 1.0 - Ease.smooth((t - peak) / (to - peak));
    }
}
