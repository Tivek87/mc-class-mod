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

// The lash of a player's whip in the world as it is meant to go: flung by its move, drooping when slack and wound
// round a creature by the lasso. The rope that draws it (see WhipRope) keeps to it as firmly as the lash is taut.
final class WhipLine {
    // Enough pieces for the loops of a curled lash to read round.
    static final int SEGMENTS = 72;
    private static final int LINE = 30;
    private static final double WIND_IN = 2.0;
    private static final double LIFT = 0.03;
    private static final double TURNS = 3.0;
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);

    // What a lash is drawn from: the end of the handle and its way, where its owner looks, how it hangs and the ground
    // under it.
    record Hold(Vec3 root, Vec3 handle, WhipLash.Look look, double length, WhipLash.Hang hang, double ground) {
    }

    // The lash's points, and how firmly each keeps its place: 1 taut or wound up, 0 slack.
    record Shape(Vec3[] points, @Nullable float[] firm) {
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
        double length = WhipStates.wheel().value("whipLength");
        return new Hold(root, handle, look, length, new WhipLash.Hang(hang, time), ground(player, feet) + LIFT);
    }

    private static double ground(Entity player, Vec3 feet) {
        if (player.onGround()) {
            return feet.y;
        }
        BlockHitResult below = player.level().clip(new ClipContext(feet.add(0.0, 0.1, 0.0), feet.subtract(0.0, 4.0,
                0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return below.getType() == HitResult.Type.MISS ? feet.y - 16.0 : below.getLocation().y;
    }

    // The lash at a moment t of the current move (the time of the world passes with it for ripples), lying on the
    // ground where it runs into it, as the rope does: its streak and cracks never show under the ground.
    static Vec3[] at(Entity player, WhipStates.Blend blend, WhipStates.State state, Hold hold, float t, float time) {
        Vec3[] points = build(player, blend, state, hold, t, time, false).points();
        return WhipRope.laid(points, points, hold.ground());
    }

    // The same with how firmly each point keeps its place, for the rope that draws it.
    static Shape shape(Entity player, WhipStates.Blend blend, WhipStates.State state, Hold hold, float t,
            float time) {
        return build(player, blend, state, hold, t, time, true);
    }

    private static Shape build(Entity player, WhipStates.Blend blend, WhipStates.State state, Hold hold, float t,
            float time, boolean firmed) {
        WhipLash.Aims aims = moment -> WhipStates.lashAt(blend, (float) moment, time, state.before());
        Entity target = state.move() == WhipMove.LASSO ? caught(player) : null;
        if (target == null) {
            return new Shape(lash(hold, aims, t), firmed ? firm(aims, t, 0.0) : null);
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 middle = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.55, 0.0);
        WhipLash.Aims thrown = moment -> toward(aims.at(moment), hold, middle, (float) moment);
        Vec3[] loose = lash(hold, thrown, t);
        // The lash flows into its coils as it reaches the creature and out of them once it is let go, never jumps.
        double wound = Ease.smooth((t - (WhipMove.LASSO_REACH - WIND_IN)) / (2.0 * WIND_IN))
                * (1.0 - Ease.smooth((t - WhipMove.LASSO_LAND) / 5.0));
        float[] firm = firmed ? firm(thrown, t, wound) : null;
        if (wound <= 0.0) {
            return new Shape(loose, firm);
        }
        Vec3[] coil = coiled(hold.root(), target, middle, t, hold.length());
        Vec3[] out = new Vec3[coil.length];
        for (int i = 0; i < coil.length; i++) {
            out[i] = loose[i].lerp(coil[i], wound);
        }
        return new Shape(out, firm);
    }

    private static Vec3[] lash(Hold hold, WhipLash.Aims aims, float t) {
        return WhipLash.shape(hold.root(), hold.handle(), hold.look(), hold.length(), aims, t, SEGMENTS, hold.hang());
    }

    // Each point is as firm as the lash is taut at the moment that point shows, and fully firm where it is wound in
    // the coil (see WhipLash.shape) or round a creature.
    private static float[] firm(WhipLash.Aims aims, float t, double wound) {
        float[] firm = new float[SEGMENTS + 1];
        firm[0] = 1.0F;
        float[] now = aims.at(t);
        double reach = Math.max(0.0, now[WhipLash.REACH]);
        for (int i = 0; i < SEGMENTS; i++) {
            float[] aim = aims.at(t + WhipLash.lag(reach * (i + 0.5) / SEGMENTS, now[WhipLash.STEADY], t));
            float coiled = Mth.clamp(Mth.clamp(aim[WhipLash.CURL], 0.0F, 1.0F) * SEGMENTS - i, 0.0F, 1.0F);
            float held = Math.max(Mth.clamp(aim[WhipLash.TAUT], 0.0F, 1.0F), coiled);
            firm[i + 1] = (float) Math.max(held, wound);
        }
        return firm;
    }

    // While it is thrown, the lash turns from its own path to the creature and takes the length it needs; once the
    // creature lies at the owner's feet it goes back to its own path.
    private static float[] toward(float[] aim, Hold hold, Vec3 middle, float t) {
        double w = Ease.smooth((t - 4.5) / 3.0) * (1.0 - Ease.smooth((t - WhipMove.LASSO_LAND - 3.0) / 8.0));
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

    // From the hand straight to the creature, then round it up to three times from the top down, tightening as it
    // winds: only as often as the lash has length left, so it never stretches.
    private static Vec3[] coiled(Vec3 root, Entity target, Vec3 middle, float t, double length) {
        double wrap = Ease.smooth((t - WhipMove.LASSO_REACH) / (double) (WhipMove.LASSO_WRAPPED
                - WhipMove.LASSO_REACH));
        double radius = target.getBbWidth() * 0.5 + 0.1;
        double tight = Mth.lerp(wrap, 1.4, 1.0);
        double high = target.getBbHeight();
        Vec3 in = new Vec3(root.x - middle.x, 0.0, root.z - middle.z);
        double start = in.lengthSqr() < 1.0E-6 ? 0.0 : Math.atan2(in.z, in.x);
        Vec3[] points = new Vec3[SEGMENTS + 1];
        Vec3 entry = round(middle, start, 0.0, radius * tight, high);
        double left = length - root.distanceTo(entry);
        double most = Mth.clamp(left / (Math.PI * 2.0 * radius), 1.0, TURNS);
        double turns = Math.max(0.05, most * wrap);
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
