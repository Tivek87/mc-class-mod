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
// index 0 its right: legs swung ahead (-) with knees bent back (+) and feet tipped toes down (+), arms swung ahead (-)
// and spread out (+) with elbows bent ahead (-), hands bent at the wrist (ahead -) and fingers curled from 0 (open) to
// 1 (a fist); its hips sunk and tipped down at its left (+), its trunk twisted over the waist (its right shoulder back,
// +) and leant ahead, its head turned to where it looks. Every move winds up before it strikes and follows through.
final class MinionPose {
    private static final double WALK = 0.6662;
    private static final double LEG = MinionShapes.THIGH + MinionShapes.SHIN;
    private static final double SHOULDER_Y = MinionShapes.SHOULDER_Y;
    // Coming up out of the ground takes RISE ticks; a landing gives LAND, the first out of the hatch (within
    // HERO_WITHIN ticks of coming) HERO.
    static final double RISE = 36.0;
    private static final double LAND = 9.0;
    private static final double HERO = 20.0;
    private static final double HERO_WITHIN = 60.0;

    double sink;
    double roll;
    double twist;
    double lean;
    double look;
    // Its head bowed (+) or tipped back.
    double nod;
    // How hard its feet and back pack burn, 0 to 1.
    double thrust;
    final double[] hip = new double[2];
    final double[] knee = new double[2];
    final double[] ankle = new double[2];
    final double[] arm = new double[2];
    final double[] spread = new double[2];
    final double[] elbow = new double[2];
    final double[] wrist = new double[2];
    final double[] curl = { 0.4, 0.4 };
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
        pose.worn(lost(minion), time, minion.getId());
        for (int s = 0; s < 2; s++) {
            pose.curl[s] = Mth.clamp(pose.curl[s], 0.0, 1.0);
        }
        return pose;
    }

    // Standing still it breathes, its chest rising and falling; it shifts its weight from foot to foot, its hips
    // tipping over the leg it stands on, looks about now and then, and its fingers flex. No two are in step.
    private void idle(MechMinion minion, double time) {
        double still = 1.0 - Math.min(1.0, minion.walkAnimation.speed());
        if (still <= 0.0) {
            return;
        }
        int id = minion.getId();
        double breath = Math.sin(time * 0.09 + id);
        this.lean += 0.03 * still * breath;
        this.sink += 0.025 * still * (1.0 + breath);
        this.nod -= 0.04 * still * breath;
        double weight = Math.sin(time * 0.031 + id * 2.3);
        this.roll += 0.06 * still * weight;
        this.twist += 0.04 * still * Math.sin(time * 0.023 + id);
        double onto = 0.08 * still * Math.max(0.0, weight);
        double off = 0.08 * still * Math.max(0.0, -weight);
        this.knee[0] += 0.14 * off;
        this.hip[0] -= 0.07 * off;
        this.knee[1] += 0.14 * onto;
        this.hip[1] -= 0.07 * onto;
        double scan = Math.sin(time * 0.017 + id * 1.3) * Math.sin(time * 0.011 + id);
        this.look += 0.5 * still * scan * Math.abs(scan);
        for (int s = 0; s < 2; s++) {
            double sway = Math.sin(time * 0.07 + id * 1.7 + s * 2.1);
            this.arm[s] += 0.05 * still * sway;
            this.spread[s] += 0.03 * still * (1.0 + sway);
            this.wrist[s] += 0.08 * still * sway;
            this.curl[s] += still * (0.12 * Math.sin(time * 0.05 + id + s * 1.9) - 0.1);
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
            this.ankle[s] = Mth.lerp(air, this.ankle[s], Mth.lerp(open, 0.2, 0.5));
            this.wrist[s] = Mth.lerp(air, this.wrist[s], 0.15 * wobble);
            this.curl[s] = Mth.lerp(air, this.curl[s], Mth.lerp(open, 0.9, 0.15));
        }
        this.roll += air * 0.05 * wobble;
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
                this.wrist[s] -= 0.3 * w;
            }
            return;
        }
        this.curl[0] = Mth.lerp(w, this.curl[0], 1.0);
        this.wrist[0] = Mth.lerp(w, this.wrist[0], 0.35);
        this.curl[1] = Mth.lerp(w, this.curl[1], 0.1);
        this.roll += 0.08 * w;
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
        this.curl[0] = Mth.lerp(kneel, this.curl[0], 1.0);
        this.wrist[0] = Mth.lerp(kneel, this.wrist[0], 0.35);
        this.ankle[1] = Mth.lerp(kneel, this.ankle[1], 0.6);
        double flex = Ease.bump((at - 0.82) / 0.18);
        for (int s = 0; s < 2; s++) {
            this.spread[s] += 0.35 * flex;
            this.elbow[s] -= 0.6 * flex;
            this.curl[s] = Mth.lerp(flex, this.curl[s], 1.0);
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
        // A sharp rock back at once, then a slower sway home.
        double snap = h * h * (3.0 - 2.0 * h);
        this.lean -= 0.32 * snap;
        this.twist += 0.25 * side * snap;
        this.roll += 0.1 * side * snap;
        this.nod -= 0.34 * snap;
        this.sink += 0.08 * snap;
        for (int s = 0; s < 2; s++) {
            this.arm[s] += 0.35 * snap;
            this.spread[s] += 0.3 * snap;
            this.wrist[s] += 0.4 * snap;
            this.curl[s] -= 0.3 * snap;
        }
    }

    // Worn down it hunches more and more and its hands hang; badly worn it twitches now and then, a shudder running
    // through it.
    private void worn(double lost, double time, int id) {
        if (lost <= 0.0) {
            return;
        }
        this.lean += 0.14 * lost;
        this.sink += 0.1 * lost;
        this.nod += 0.08 * lost;
        for (int s = 0; s < 2; s++) {
            this.spread[s] -= 0.05 * lost;
            this.wrist[s] += 0.15 * lost;
        }
        if (lost > 0.6) {
            double twitch = Math.max(0.0, Math.sin(time * 0.9 + id) * Math.sin(time * 0.23 + id * 0.7) - 0.55);
            this.nod += twitch * 0.9;
            this.twist += twitch * 0.4 * Math.sin(time * 3.1);
            this.roll += twitch * 0.25;
        }
    }

    // A heavy stride: each leg swings ahead with its knee lifted and lands heel first, toes up, then rolls over the foot
    // and pushes off its toes behind. The hips dip as the legs spread and rise over the leg it stands on, tipping
    // towards it; the shoulders turn against the hips and the arms swing against the legs, the elbow bending as the
    // arm comes forward and the hand trailing at the wrist.
    private void walk(MechMinion minion, float partialTick) {
        double at = minion.walkAnimation.position(partialTick) * WALK;
        double speed = Math.min(1.0, minion.walkAnimation.speed(partialTick));
        for (int s = 0; s < 2; s++) {
            double phase = s == 0 ? at : at + Math.PI;
            double reach = Math.sin(phase);
            double coming = Math.cos(phase);
            this.hip[s] = -0.62 * speed * reach - 0.12 * speed * Math.max(0.0, coming);
            this.knee[s] = speed * (0.1 + 0.95 * Math.max(0.0, coming) * (0.4 + 0.6 * Math.max(0.0, -reach + 0.4)));
            this.ankle[s] = speed * (-0.32 * Math.max(0.0, reach) * Math.max(0.0, -coming + 0.3)
                    + 0.5 * Math.max(0.0, -reach) * Math.max(0.0, coming + 0.2));
            this.arm[s] = 0.5 * speed * reach;
            this.elbow[s] = -0.3 - 0.5 * speed * Math.max(0.0, -reach);
            this.wrist[s] = 0.12 * speed * coming;
            this.spread[s] = 0.1;
            this.curl[s] = 0.55;
        }
        double swing = Math.sin(at);
        this.sink = speed * (0.14 * Math.abs(swing) - 0.03);
        this.roll = -0.06 * speed * Math.cos(at);
        this.twist = 0.16 * speed * swing;
        this.lean = 0.08 * speed;
        this.nod = -0.03 * speed;
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

    // Fists up on guard. The left draws back, snaps out straight at JAB as the left foot steps in, holds a moment and
    // comes back; the right swings out level at the side, elbow bent, cocks back as the hips wind and whips round
    // across in a hook at HOOK, the right heel lifting as the hips turn through. (Swung out to the side, its shoulder's
    // forward swing and its elbow both turn level, so the hook stays level.)
    private void punch(double age) {
        double guard = Ease.smooth(age / 2.0) * (1.0 - Ease.smooth((age - MinionMoves.PUNCH_END + 3.0) / 3.0));
        double windJab = bump(age, 0.5, MinionMoves.JAB - 1.5, MinionMoves.JAB - 0.3);
        double jab = strike(age, MinionMoves.JAB - 1.3, MinionMoves.JAB, MinionMoves.JAB + 4.5);
        double windHook = bump(age, MinionMoves.JAB + 2.0, MinionMoves.HOOK - 1.6, MinionMoves.HOOK - 0.3);
        double hook = strike(age, MinionMoves.HOOK - 1.4, MinionMoves.HOOK, MinionMoves.PUNCH_END);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(guard, this.arm[s], -0.55);
            this.elbow[s] = Mth.lerp(guard, this.elbow[s], -1.75);
            this.spread[s] = Mth.lerp(guard, this.spread[s], 0.18);
            this.wrist[s] = Mth.lerp(guard, this.wrist[s], 0.1);
            this.curl[s] = Mth.lerp(guard, this.curl[s], 1.0);
        }
        this.arm[1] = Mth.lerp(jab, this.arm[1] + 0.35 * windJab, -1.55);
        this.elbow[1] = Mth.lerp(jab, this.elbow[1] - 0.3 * windJab, -0.05);
        this.spread[1] = Mth.lerp(jab, this.spread[1], 0.04);
        this.wrist[1] = Mth.lerp(jab, this.wrist[1], -0.1);
        double hooking = Math.max(windHook, hook);
        double sweep = Ease.smooth((age - MinionMoves.HOOK + 1.4) / 2.4);
        this.spread[0] = Mth.lerp(hooking, this.spread[0], 1.35);
        this.arm[0] = Mth.lerp(hooking, this.arm[0], Mth.lerp(sweep, 0.35, -0.95));
        this.elbow[0] = Mth.lerp(hooking, this.elbow[0], -1.5);
        this.twist += -0.18 * windJab + 0.38 * jab + 0.3 * windHook - 0.5 * hook;
        this.roll += 0.05 * jab - 0.06 * hook;
        this.lean += 0.14 * Math.max(jab, hook) - 0.05 * Math.max(windJab, windHook);
        this.crouch(0.12 * Math.max(jab, hook) + 0.05 * Math.max(windJab, windHook));
        this.hip[1] -= 0.28 * jab;
        this.knee[1] += 0.12 * jab;
        this.hip[0] += 0.12 * jab;
        this.ankle[0] += 0.4 * hook;
    }

    // The right arm comes up straight at its target, the hand bent down out of the barrel's way, the left hand
    // bracing the right forearm and the body turned side on, feet apart. It shudders as the cannon charges, and the
    // shot kicks the arm up and rocks it back a step.
    private void cannon(ClientLevel level, MechMinion minion, double age, float partialTick) {
        double held = Ease.smooth(age / 4.0) * (1.0 - Ease.smooth((age - MinionMoves.CANNON_END + 5.0) / 5.0));
        double pitch = 0.0;
        Entity target = level.getEntity(minion.aimed());
        if (target != null) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(minion.getPosition(partialTick)
                    .add(0.0, SHOULDER_Y, 0.0));
            pitch = Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z));
        }
        double charge = Ease.smooth((age - 3.0) / (MinionMoves.FIRE - 3.0));
        double kick = Ease.jolt((age - MinionMoves.FIRE) / 5.0);
        double shake = age < MinionMoves.FIRE ? 0.025 * charge * Math.sin(age * 5.3) : 0.0;
        this.arm[0] = Mth.lerp(held, this.arm[0], -Math.PI / 2.0 - pitch - 0.45 * kick + shake);
        this.elbow[0] = Mth.lerp(held, this.elbow[0], -0.25 * kick);
        this.spread[0] = Mth.lerp(held, this.spread[0], -0.05);
        this.wrist[0] = Mth.lerp(held, this.wrist[0], 0.3);
        this.curl[0] = Mth.lerp(held, this.curl[0], 1.0);
        this.arm[1] = Mth.lerp(held, this.arm[1], -1.25 - 0.8 * pitch);
        this.spread[1] = Mth.lerp(held, this.spread[1], -0.5);
        this.elbow[1] = Mth.lerp(held, this.elbow[1], -1.2);
        this.curl[1] = Mth.lerp(held, this.curl[1], 0.7);
        this.twist -= 0.3 * held;
        this.lean -= 0.1 * kick;
        this.nod += shake;
        this.sink += 0.05 * kick;
        this.crouch(0.18 * held);
        this.hip[1] -= 0.18 * held;
        this.hip[0] += 0.1 * held + 0.12 * kick;
    }

    // It gathers itself: sinks deep with its arms swung back behind it and its heels up, then springs at SPRING, arms
    // thrown up over its head, and tucks its legs under it in the air.
    private void leap(double age) {
        double down = Ease.smooth(age / MinionMoves.SPRING) * (1.0 - Ease.smooth((age - MinionMoves.SPRING) / 1.5));
        double up = Ease.smooth((age - MinionMoves.SPRING) / 3.0);
        double tuck = bump(age, MinionMoves.SPRING, MinionMoves.SPRING + 4.0, MinionMoves.SPRING + 12.0);
        this.lean += 0.4 * down - 0.15 * up;
        this.nod += 0.2 * down - 0.15 * up;
        this.crouch(0.65 * down);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(up, Mth.lerp(down, this.arm[s], 1.0), -2.75);
            this.elbow[s] = Mth.lerp(up, Mth.lerp(down, this.elbow[s], -0.2), -0.35);
            this.spread[s] = Mth.lerp(up, this.spread[s], 0.25);
            this.hip[s] = Mth.lerp(up, this.hip[s], -0.9 - 0.5 * tuck);
            this.knee[s] = Mth.lerp(up, this.knee[s], 1.3 + 0.6 * tuck);
            this.ankle[s] = Mth.lerp(up, Mth.lerp(down, this.ankle[s], 0.35), 0.5);
            this.wrist[s] = Mth.lerp(down, this.wrist[s], -0.4);
            this.curl[s] = Mth.lerp(up, Mth.lerp(down, this.curl[s], 0.8), 1.0);
        }
    }

    // Landed: both fists hammer down from over its head into the ground before it, it bends deep over them and holds
    // there a moment, squashed by the blow, before it rises.
    private void slam(double age) {
        double hit = 1.0 - Ease.smooth((age - 2.0) / (MinionMoves.SLAM_END - 2.0));
        double down = Ease.smooth(age / 1.5);
        double squash = Ease.jolt(age / 4.0);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(hit, this.arm[s], Mth.lerp(down, -2.75, -0.55));
            this.elbow[s] = Mth.lerp(hit, this.elbow[s], -0.15);
            this.spread[s] = Mth.lerp(hit, this.spread[s], 0.08);
            this.wrist[s] = Mth.lerp(hit, this.wrist[s], 0.3);
            this.curl[s] = Mth.lerp(hit, this.curl[s], 1.0);
        }
        this.lean += 0.55 * hit;
        this.nod += 0.3 * hit;
        this.crouch(0.7 * hit + 0.1 * squash);
        this.hip[1] -= 0.25 * hit;
        this.ankle[0] += 0.3 * hit;
    }

    // Up from `from` to whole at `peak`, down again by `to`.
    private static double bump(double t, double from, double peak, double to) {
        if (t <= from || t >= to) {
            return 0.0;
        }
        return t < peak ? Ease.smooth((t - from) / (peak - from)) : 1.0 - Ease.smooth((t - peak) / (to - peak));
    }

    // A blow: snapped out to past whole by `peak`, held a tick, drawn back by `to`.
    private static double strike(double t, double from, double peak, double to) {
        if (t <= from || t >= to) {
            return 0.0;
        }
        if (t < peak) {
            return Ease.backOut((t - from) / (peak - from));
        }
        return t < peak + 1.0 ? 1.0 : 1.0 - Ease.smooth((t - peak - 1.0) / (to - peak - 1.0));
    }

    // How much of its health it has lost, 0 to 1.
    static double lost(MechMinion minion) {
        return Mth.clamp(1.0 - minion.getHealth() / Math.max(1.0F, minion.getMaxHealth()), 0.0, 1.0);
    }
}
