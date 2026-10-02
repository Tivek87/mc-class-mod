package nl.tivek.multiversepowers.engine.client.stagger;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.entity.impact.ImpactPayload;
import org.joml.Vector3f;

// One creature reeling from a blow it lived through, as this player's game shows it. Where it was struck and the way
// the blow went are kept in its body's own frame (model pixels: +x its left, +y down, -z ahead). Its trunk is thrown
// from the blow and springs back upright, turning about where it was struck; its hips sink as its knees take the blow;
// its feet stay planted where they stood while the blow carries it, and step one at a time to stay under it; a hand
// goes to the wound and the other arm out for balance; its head snaps from the blow and then looks down at the wound.
final class Reaction {
    // The lean's spring (a share of the lean taken back each tick, squared) and its damping, and the head's; the
    // furthest it leans (radians).
    private static final float STIFF = 0.16F;
    private static final float DAMP = 0.38F;
    private static final float MOST_LEAN = 0.6F;
    private static final float HEAD_STIFF = 0.4F;
    private static final float HEAD_DAMP = 0.6F;
    // How fast (radians a tick) a blow of strength 1 throws the trunk: back (or to a side) for one that lands on it,
    // and more per pixel it lands above the hips; twists it per pixel off its middle, and snaps the head back.
    private static final float TRUNK_KICK = 0.3F;
    private static final float KICK = 0.015F;
    private static final float TWIST = 0.02F;
    private static final float SNAP = 0.35F;
    private static final float HEAD_SNAP = 0.5F;
    // Stronger blows than this are shown as this strong.
    private static final float STRONGEST = 1.6F;
    private static final float HIP_Y = 12.0F;
    private static final float HIP_X = 1.9F;
    // Steps (blocks, ticks): the body this far from over its feet (ahead by AHEAD ticks of its motion) takes one, each
    // step this long at most, overshooting by this share, lifting its foot this high and lasting this long.
    private static final double REACH = 0.08;
    private static final double AHEAD = 3.0;
    private static final double MOST_STEP = 0.5;
    private static final double OVERSHOOT = 0.25;
    static final double LIFT = 0.1;
    static final int STEP_TICKS = 4;
    // A flinch never steps; how deep (pixels) the hips sink at most.
    private static final float SINK = 2.2F;

    final LivingEntity entity;
    final ImpactPayload.Reaction kind;
    final int ticks;
    int age;
    // Where it was struck and the way the blow went, in its body's frame as it stood.
    final Vector3f wound = new Vector3f();
    final Vector3f way = new Vector3f();
    final float strength;
    // Which hand holds the wound (the right one when true), and whether both do; whether the wound is on the trunk,
    // the head, an arm or a leg.
    final boolean rightHand;
    final boolean bothHands;
    final Where where;
    // Pitch (forward +), roll (its left +) and twist of the trunk, and their speeds, now and the tick before.
    final float[] lean = new float[3];
    final float[] spin = new float[3];
    final float[] leanWas = new float[3];
    // The head's own nod from the blow (forward +), its speed, the tick before.
    float head;
    float headSpin;
    float headWas;
    // The feet: right, then left.
    final Foot[] feet = { new Foot(), new Foot() };
    // Where the creature stood last tick, for how fast the blow carries it.
    private double lastX;
    private double lastZ;

    enum Where {
        TRUNK,
        HEAD,
        ARM,
        LEG
    }

    // A foot planted at (x, y, z) in the world, or stepping from there to (toX, toZ) `step` ticks in (-1 planted).
    static final class Foot {
        double x;
        double y;
        double z;
        double fromX;
        double fromZ;
        double toX;
        double toZ;
        int step = -1;
        // Where it was the tick before, for drawing between ticks.
        double wasX;
        double wasY;
        double wasZ;
        int stepWas = -1;
    }

    // `had`: the reaction it was still in, if any: its body goes on from there.
    Reaction(LivingEntity entity, ImpactPayload payload, @Nullable Reaction had) {
        this.entity = entity;
        this.kind = payload.reaction();
        this.ticks = Math.max(1, payload.ticks());
        this.strength = Math.min(STRONGEST, payload.strength());
        toModel(entity, payload.at().subtract(entity.position()), true, this.wound);
        toModel(entity, payload.way(), false, this.way);
        if (this.way.lengthSquared() > 1.0E-6F) {
            this.way.normalize();
        }
        float x = this.wound.x;
        float y = this.wound.y;
        this.where = y < 0.0F ? Where.HEAD : y > HIP_Y ? Where.LEG : Math.abs(x) > 4.0F ? Where.ARM : Where.TRUNK;
        // A wound on its right side (its own -x) is held with its right hand; one on an arm with the other hand.
        boolean right = x < 0.0F;
        this.rightHand = this.where == Where.ARM ? !right : Math.abs(x) < 1.0F || right;
        this.bothHands = this.where == Where.TRUNK && Math.abs(x) < 2.0F && this.strength > 0.9F;
        float s = this.strength;
        float above = y - HIP_Y;
        // The blow turns the trunk about the hips as it pushes the point it landed on: back for a blow from ahead
        // above the hips (forward for one at the legs), to the side, and round about its middle.
        float thrown = this.where == Where.LEG ? 0.0F : TRUNK_KICK;
        this.spin[0] = s * this.way.z * (KICK * above - thrown);
        this.spin[1] = -s * this.way.x * (KICK * above - thrown);
        this.spin[2] = TWIST * s * (this.way.x * this.wound.z - this.way.z * x);
        this.headSpin = -(this.where == Where.HEAD ? HEAD_SNAP : SNAP) * s * this.way.z;
        double side = HIP_X / 16.0 * entity.getScale();
        float yaw = entity.yBodyRot * Mth.DEG_TO_RAD;
        for (int k = 0; k < 2; k++) {
            // The right foot to its right: facing yaw, its right is (-cos, sin)... as the game turns a body.
            double sign = k == 0 ? -1.0 : 1.0;
            Foot foot = this.feet[k];
            foot.x = entity.getX() + Math.cos(yaw) * side * sign;
            foot.y = entity.getY();
            foot.z = entity.getZ() + Math.sin(yaw) * side * sign;
            foot.wasX = foot.x;
            foot.wasY = foot.y;
            foot.wasZ = foot.z;
        }
        this.lastX = entity.getX();
        this.lastZ = entity.getZ();
        if (had != null) {
            for (int k = 0; k < 3; k++) {
                this.lean[k] = had.lean[k];
                this.spin[k] += had.spin[k];
            }
            this.head = had.head;
            this.headSpin += had.headSpin;
            for (int k = 0; k < 2; k++) {
                Foot foot = this.feet[k];
                Foot old = had.feet[k];
                foot.x = old.x;
                foot.y = old.y;
                foot.z = old.z;
                foot.wasX = old.x;
                foot.wasY = old.y;
                foot.wasZ = old.z;
            }
            System.arraycopy(this.lean, 0, this.leanWas, 0, 3);
            this.headWas = this.head;
        }
        if (this.where == Where.LEG && this.kind == ImpactPayload.Reaction.STAGGER) {
            // The struck leg is knocked from under it: it steps the way the blow went at once.
            Foot struck = this.feet[x < 0.0F ? 0 : 1];
            Vec3 push = payload.way();
            double flat = Math.sqrt(push.x * push.x + push.z * push.z);
            if (flat > 1.0E-4) {
                this.step(struck, struck.x + push.x / flat * 0.3 * s, struck.z + push.z / flat * 0.3 * s);
            }
        }
    }

    // A world offset into the model's frame as the creature stands now (pixels for a place; a unit way when not).
    static Vector3f toModel(LivingEntity entity, Vec3 offset, boolean place, Vector3f out) {
        double angle = entity.yBodyRot * Mth.DEG_TO_RAD - Math.PI;
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double x = offset.x * c + offset.z * s;
        double z = -offset.x * s + offset.z * c;
        double scale = place ? 16.0 / Math.max(0.05F, entity.getScale()) : 1.0;
        return out.set((float) (-x * scale), (float) ((place ? 1.501 - offset.y : -offset.y) * scale),
                (float) (z * scale));
    }

    // One tick on. False once it is over.
    boolean tick() {
        System.arraycopy(this.lean, 0, this.leanWas, 0, 3);
        this.headWas = this.head;
        for (Foot foot : this.feet) {
            foot.wasX = foot.x;
            foot.wasY = foot.y;
            foot.wasZ = foot.z;
            foot.stepWas = foot.step;
        }
        this.age++;
        for (int k = 0; k < 3; k++) {
            this.spin[k] += -STIFF * this.lean[k] - DAMP * this.spin[k];
            this.lean[k] = Mth.clamp(this.lean[k] + this.spin[k], -MOST_LEAN, MOST_LEAN);
        }
        this.headSpin += -HEAD_STIFF * this.head - HEAD_DAMP * this.headSpin;
        this.head += this.headSpin;
        this.feet();
        return this.age < this.ticks + StaggerPose.FADE_OUT && this.entity.isAlive() && !this.entity.isRemoved();
    }

    // The feet: a stepping foot goes on; else, should the body be carried from over its feet, the foot further from
    // where it should stand steps there.
    private void feet() {
        double vx = this.entity.getX() - this.lastX;
        double vz = this.entity.getZ() - this.lastZ;
        this.lastX = this.entity.getX();
        this.lastZ = this.entity.getZ();
        boolean stepping = false;
        for (Foot foot : this.feet) {
            if (foot.step < 0) {
                continue;
            }
            stepping = true;
            if (++foot.step >= STEP_TICKS) {
                foot.step = -1;
                foot.x = foot.toX;
                foot.z = foot.toZ;
            } else {
                double u = (double) foot.step / STEP_TICKS;
                foot.x = Mth.lerp(u, foot.fromX, foot.toX);
                foot.z = Mth.lerp(u, foot.fromZ, foot.toZ);
            }
            foot.y = this.entity.getY();
        }
        if (stepping || this.kind == ImpactPayload.Reaction.FLINCH || this.age >= this.ticks) {
            return;
        }
        double gx = this.entity.getX() + vx * AHEAD;
        double gz = this.entity.getZ() + vz * AHEAD;
        double mx = (this.feet[0].x + this.feet[1].x) * 0.5;
        double mz = (this.feet[0].z + this.feet[1].z) * 0.5;
        if ((gx - mx) * (gx - mx) + (gz - mz) * (gz - mz) < REACH * REACH) {
            return;
        }
        double side = HIP_X / 16.0 * this.entity.getScale();
        float yaw = this.entity.yBodyRot * Mth.DEG_TO_RAD;
        Foot pick = null;
        double far = -1.0;
        double px = 0.0;
        double pz = 0.0;
        for (int k = 0; k < 2; k++) {
            double sign = k == 0 ? -1.0 : 1.0;
            double ox = gx + Math.cos(yaw) * side * sign;
            double oz = gz + Math.sin(yaw) * side * sign;
            Foot foot = this.feet[k];
            double d = (ox - foot.x) * (ox - foot.x) + (oz - foot.z) * (oz - foot.z);
            if (d > far) {
                far = d;
                pick = foot;
                px = ox;
                pz = oz;
            }
        }
        if (pick != null) {
            this.step(pick, px + (px - pick.x) * OVERSHOOT, pz + (pz - pick.z) * OVERSHOOT);
        }
    }

    private void step(Foot foot, double x, double z) {
        double dx = x - foot.x;
        double dz = z - foot.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > MOST_STEP) {
            dx *= MOST_STEP / length;
            dz *= MOST_STEP / length;
        }
        foot.fromX = foot.x;
        foot.fromZ = foot.z;
        foot.toX = foot.x + dx;
        foot.toZ = foot.z + dz;
        foot.step = 0;
    }

    // How deep the hips sink (pixels) at `t` ticks: as the knees take the blow, then back.
    float sink(float t) {
        if (this.kind == ImpactPayload.Reaction.FLINCH) {
            return 0.0F;
        }
        float u = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
        return SINK * this.strength * Mth.sin(u * Mth.PI) * (this.where == Where.LEG ? 1.5F : 1.0F);
    }
}
