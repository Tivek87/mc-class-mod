package nl.tivek.multiversepowers.character.greenlantern.client.body;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipLash;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The lash of a player's whip in the world: flung by its move, hanging and lying on the ground when slack, and
// wound round a creature by the lasso.
final class WhipLine {
    // Enough pieces for the loops of a curled lash to read round.
    static final int SEGMENTS = 48;
    private static final int LINE = 20;
    private static final double LIFT = 0.03;
    private static final double TURNS = 3.0;
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);

    // What a lash is drawn from: the end of the handle and its way, where its owner looks and how it lies.
    record Hold(Vec3 root, Vec3 handle, WhipLash.Look look, double length, WhipLash.Lie lie) {
    }

    private WhipLine() {
    }

    static Hold hold(Entity player, Vec3 root, Vec3 handle, float partialTick, float time) {
        WhipLash.Look look = WhipLash.Look.of(player.getViewYRot(partialTick), player.getViewXRot(partialTick));
        Vec3 feet = player.getPosition(partialTick);
        Vec3 moving = player.getDeltaMovement();
        Vec3 drag = new Vec3(moving.x, 0.0, moving.z);
        Vec3 sway = look.right().scale(0.06 * Mth.sin(time * 0.071F)).add(look.flat().scale(0.05 * Mth.sin(time
                * 0.053F + 1.0F)));
        Vec3 hang = DOWN.add(drag.scale(-2.5)).add(sway).normalize();
        Vec3 lie = look.flat().scale(0.55).add(look.right().scale(0.7)).subtract(drag.scale(8.0));
        lie = lie.lengthSqr() < 1.0E-6 ? look.right() : new Vec3(lie.x, 0.0, lie.z).normalize();
        double length = WhipStates.wheel().value("whipLength");
        return new Hold(root, handle, look, length, new WhipLash.Lie(ground(player, feet) + LIFT, hang, lie, time));
    }

    private static double ground(Entity player, Vec3 feet) {
        if (player.onGround()) {
            return feet.y;
        }
        BlockHitResult below = player.level().clip(new ClipContext(feet.add(0.0, 0.1, 0.0), feet.subtract(0.0, 4.0,
                0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return below.getType() == HitResult.Type.MISS ? feet.y - 16.0 : below.getLocation().y;
    }

    // The lash at a moment t of the current move (the time of the world passes with it for ripples).
    static Vec3[] at(Entity player, WhipStates.Blend blend, WhipStates.State state, Hold hold, float t, float time) {
        WhipLash.Aims aims = moment -> WhipStates.lashAt(blend, (float) moment, time, state.before());
        if (state.move() != WhipMove.LASSO) {
            return WhipLash.shape(hold.root(), hold.handle(), hold.look(), hold.length(), aims, t, SEGMENTS,
                    hold.lie());
        }
        Entity target = caught(player);
        if (target == null) {
            return WhipLash.shape(hold.root(), hold.handle(), hold.look(), hold.length(), aims, t, SEGMENTS,
                    hold.lie());
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 middle = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.55, 0.0);
        WhipLash.Aims thrown = moment -> toward(aims.at(moment), hold, middle, (float) moment);
        Vec3[] loose = WhipLash.shape(hold.root(), hold.handle(), hold.look(), hold.length(), thrown, t, SEGMENTS,
                hold.lie());
        if (t < WhipMove.LASSO_REACH) {
            return loose;
        }
        Vec3[] coil = coiled(hold.root(), target, middle, t);
        double unwound = Ease.smooth((t - WhipMove.LASSO_LAND) / 5.0);
        if (unwound <= 0.0) {
            return coil;
        }
        Vec3[] out = new Vec3[coil.length];
        for (int i = 0; i < coil.length; i++) {
            out[i] = coil[i].lerp(loose[i], unwound);
        }
        return out;
    }

    // While it is thrown, the lash turns from its own path to the creature, and grows as long as it needs.
    private static float[] toward(float[] aim, Hold hold, Vec3 middle, float t) {
        double w = Ease.smooth((t - 4.5) / 3.0);
        if (w <= 0.0) {
            return aim;
        }
        Vec3 to = middle.subtract(hold.root());
        double far = to.length();
        if (far < 1.0E-3) {
            return aim;
        }
        Vec3 way = to.scale(1.0 / far);
        Vec3 up = hold.look().right().cross(hold.look().ahead());
        float[] target = aim.clone();
        target[WhipLash.YAW] = (float) Math.atan2(way.dot(hold.look().right()), way.dot(hold.look().ahead()));
        target[WhipLash.PITCH] = (float) Math.asin(Mth.clamp(way.dot(up), -1.0, 1.0));
        target[WhipLash.LEVEL] = 0.0F;
        target[WhipLash.REACH] = (float) ((far + 0.3) / Math.max(0.5, hold.length()));
        target[WhipLash.TAUT] = 1.0F;
        float[] out = aim.clone();
        WhipLash.nearest(target, aim);
        for (int c = 0; c < out.length; c++) {
            out[c] = (float) Mth.lerp(w, aim[c], target[c]);
        }
        return out;
    }

    // From the hand straight to the creature, then round it three times from the top down, tightening as it winds.
    private static Vec3[] coiled(Vec3 root, Entity target, Vec3 middle, float t) {
        double wrap = Ease.smooth((t - WhipMove.LASSO_REACH) / (double) (WhipMove.LASSO_WRAPPED
                - WhipMove.LASSO_REACH));
        double radius = target.getBbWidth() * 0.5 + 0.1;
        double tight = Mth.lerp(wrap, 1.4, 1.0);
        double high = target.getBbHeight();
        Vec3 in = new Vec3(root.x - middle.x, 0.0, root.z - middle.z);
        double start = in.lengthSqr() < 1.0E-6 ? 0.0 : Math.atan2(in.z, in.x);
        double turns = Math.max(0.05, TURNS * wrap);
        Vec3[] points = new Vec3[SEGMENTS + 1];
        Vec3 entry = round(middle, start, 0.0, radius * tight, high);
        double sag = t < WhipMove.LASSO_HAUL ? 0.08 : 0.015;
        double far = root.distanceTo(entry);
        for (int i = 0; i <= LINE; i++) {
            double u = (double) i / LINE;
            points[i] = root.lerp(entry, u).subtract(0.0, sag * far * 4.0 * u * (1.0 - u), 0.0);
        }
        for (int i = LINE + 1; i <= SEGMENTS; i++) {
            double u = (double) (i - LINE) / (SEGMENTS - LINE) * turns;
            points[i] = round(middle, start + Math.PI * 2.0 * u, u, radius * tight, high);
        }
        return points;
    }

    private static Vec3 round(Vec3 middle, double angle, double turned, double radius, double high) {
        double y = high * (0.2 - 0.35 * turned / TURNS);
        return middle.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    @Nullable
    static Entity caught(Entity player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        int snared = ClientConstructs.snared(player.getId());
        if (snared >= 0) {
            return minecraft.level.getEntity(snared);
        }
        WhipStates.Own mine = WhipStates.own;
        if (player == minecraft.player && mine != null && mine.aimed >= 0 && mine.move == WhipMove.LASSO) {
            return minecraft.level.getEntity(mine.aimed);
        }
        return null;
    }

    // The way the lash leaves the handle for a third-person body: the pose's way, bent up or down with the look.
    static Vec3 tipped(Vec3 view, float pitch, double share) {
        double angle = -Math.toRadians(pitch) * share;
        return Vectors.spin(view, new Vec3(1.0, 0.0, 0.0), angle);
    }
}
