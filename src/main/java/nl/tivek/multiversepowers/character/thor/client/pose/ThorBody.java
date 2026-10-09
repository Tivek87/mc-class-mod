package nl.tivek.multiversepowers.character.thor.client.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorMotion;
import nl.tivek.multiversepowers.character.thor.hammer.HammerRules;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Spring;
import org.joml.Quaternionf;

// What Thor's body carries from frame to frame, so it moves with momentum instead of snapping between poses: how
// fast he goes and how that changes, how far into flight or a float he is, a landing still being soaked up by his
// knees, and in flight the lean and bank of his whole body. Brought up to date once a frame, as he is turned.
final class ThorBody {
    // In flight he lies flat by this speed (blocks a tick) and stands up below the first.
    private static final double UPRIGHT = 0.12;
    private static final double FLAT = 0.75;
    private static final float MOST_TILT = 2.6F;
    private static final float HOVER_LEAN = -0.12F;
    private static final double PIVOT = 0.9;
    // A take-off draws the hammer from the belt at this tick; a touch-down puts it back at that one.
    static final float DRAW = 4.0F;
    static final float SHEATHE = 6.0F;

    private static final Int2ObjectOpenHashMap<ThorBody> BODIES = new Int2ObjectOpenHashMap<>();

    float time = Float.NaN;
    float dt;
    Vec3 velocity = Vec3.ZERO;
    Vec3 acceleration = Vec3.ZERO;
    final Spring fly = new Spring();
    final Spring floating = new Spring();
    final Spring tilt = new Spring();
    final Spring bank = new Spring();
    final Spring absorb = new Spring();
    final Spring hammer = new Spring();
    // Drawing the hammer back, reaching for it as it flies back, hanging in the air while it is out.
    final Spring cocked = new Spring();
    final Spring reach = new Spring();
    final Spring hang = new Spring();
    boolean inHand;
    boolean grounded = true;
    double falling;
    float yaw;
    final Quaternionf turn = new Quaternionf();
    boolean turned;

    private ThorBody() {
    }

    @Nullable
    static ThorBody of(Entity entity) {
        return BODIES.get(entity.getId());
    }

    static void forget() {
        BODIES.clear();
    }

    // Turns the whole body in flight, about its middle: called once a frame per Thor, before his model is posed.
    static void turn(AbstractClientPlayer player, PoseStack pose, float scale) {
        ClientThor.View view = ClientThor.view(player);
        ThorBody body = BODIES.get(player.getId());
        if (view == null && (body == null || body.idle())) {
            BODIES.remove(player.getId());
            return;
        }
        if (body == null) {
            body = new ThorBody();
            BODIES.put(player.getId(), body);
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        body.update(player, view, partialTick);
        if (body.turned) {
            double pivot = PIVOT * scale;
            pose.translate(0.0, pivot, 0.0);
            pose.mulPose(body.turn);
            pose.translate(0.0, -pivot, 0.0);
        }
    }

    private boolean idle() {
        return this.fly.value < 1.0E-3 && Math.abs(this.absorb.value) < 1.0E-2 && this.floating.value < 1.0E-3
                && Math.abs(this.absorb.speed) < 1.0E-2 && this.cocked.value < 1.0E-3 && this.reach.value < 1.0E-3
                && this.hang.value < 1.0E-3;
    }

    private void update(AbstractClientPlayer player, @Nullable ClientThor.View view, float partialTick) {
        float now = player.tickCount + partialTick;
        this.dt = Float.isNaN(this.time) ? 0.0F : Mth.clamp(now - this.time, 0.0F, 3.0F);
        this.time = now;
        Vec3 raw = new Vec3(player.getX() - player.xo, player.getY() - player.yo, player.getZ() - player.zo);
        Vec3 before = this.velocity;
        double follow = 1.0 - Math.exp(-0.6 * this.dt);
        this.velocity = this.velocity.lerp(raw, follow);
        if (this.dt > 1.0E-4F) {
            this.acceleration = this.acceleration.lerp(this.velocity.subtract(before).scale(1.0 / this.dt),
                    1.0 - Math.exp(-0.4 * this.dt));
        }
        boolean flying = view != null && view.has(ThorStatePayload.FLYING);
        boolean thrown = view != null && view.has(ThorStatePayload.THROWN);
        // The dash to his hammer lays him out as fast flight does.
        boolean dashing = view != null && view.has(ThorStatePayload.PULLING) && view.move() == ThorStatePayload.PULL
                && view.age(partialTick) >= HammerRules.WAIT;
        boolean ground = player.onGround();
        if (ground && !this.grounded && this.falling > 0.15 && !flying) {
            this.absorb.kick(Math.min(10.0, this.falling * 7.0));
        }
        if (!ground) {
            this.falling = Math.max(0.0, -raw.y);
        }
        this.grounded = ground;
        this.fly.step(flying || dashing ? 1.0 : 0.0, this.dt, 0.05, 1.0);
        this.floating.step(view != null && view.has(ThorStatePayload.FLOATING) ? 1.0 : 0.0, this.dt, 0.05, 1.0);
        this.absorb.step(0.0, this.dt, 0.085, 0.5);
        this.inHand = this.holding(view);
        this.hammer.step(this.inHand ? 1.0 : 0.0, this.dt, 0.12, 1.0);
        boolean drawing = view != null && view.has(ThorStatePayload.COCKED)
                || player == Minecraft.getInstance().player && ThorMotion.drawn(partialTick) >= 0.0F;
        this.cocked.step(drawing ? 1.0 : 0.0, this.dt, 0.1, 1.0);
        boolean calling = view != null && view.has(ThorStatePayload.CALLING)
                || player == Minecraft.getInstance().player && ThorMotion.awaiting();
        this.reach.step(calling ? 1.0 : 0.0, this.dt, 0.15, 1.0);
        this.hang.step(flying && thrown && !ThorHammerLayer.windingUp(view, partialTick) ? 1.0 : 0.0, this.dt, 0.08,
                1.0);
        this.yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        this.lean(view, flying || dashing);
    }

    // Holds the hammer from the moment a take-off draws it until a touch-down sheathes it or the flight ends otherwise
    // (knocked out of the sky), but not while it is out of his hands (past a Storm Throw's letting go).
    private boolean holding(@Nullable ClientThor.View view) {
        if (view == null || view.has(ThorStatePayload.THROWN) && !ThorHammerLayer.windingUp(view, 0.0F)) {
            return false;
        }
        float age = view.age(0.0F);
        return switch (view.move()) {
            case ThorStatePayload.TAKE_OFF -> age >= DRAW && view.has(ThorStatePayload.FLYING);
            case ThorStatePayload.TOUCH_DOWN, ThorStatePayload.SLAM -> age < SHEATHE;
            default -> view.has(ThorStatePayload.FLYING);
        };
    }

    // The whole body leans the way he flies: flat out at speed, head first into a dive, feet first carrying someone
    // down, and banks into a turn by how hard he swings aside.
    private void lean(@Nullable ClientThor.View view, boolean flying) {
        double yaw = Math.toRadians(this.yaw);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 left = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        double speed = this.velocity.length();
        double fast = Ease.smooth((speed - UPRIGHT) / (FLAT - UPRIGHT));
        float target = HOVER_LEAN;
        if (speed > 1.0E-3) {
            Vec3 way = this.velocity.scale(1.0 / speed);
            double ahead = way.dot(forward);
            double along = Math.acos(Mth.clamp(way.y, -1.0, 1.0));
            // Moving backwards he only leans back a little; forwards the body follows the way he goes.
            double wanted = ahead >= -0.2 ? along : -0.35 * Math.sin(along);
            target = (float) Mth.lerp(fast, HOVER_LEAN, Math.min(MOST_TILT, wanted));
        }
        if (view != null && view.has(ThorStatePayload.CARRYING)) {
            target = 0.25F;
        }
        float bankTarget = (float) Mth.clamp(-this.acceleration.dot(left) * 9.0, -0.75, 0.75);
        float weight = (float) this.fly.value;
        this.tilt.step(flying ? target : 0.0, this.dt, 0.06, 0.85);
        this.bank.step(flying ? bankTarget * (0.4 + 0.6 * fast) : 0.0, this.dt, 0.05, 0.8);
        float tiltNow = (float) this.tilt.value * weight;
        float bankNow = (float) this.bank.value * weight;
        this.turned = Math.abs(tiltNow) > 1.0E-3F || Math.abs(bankNow) > 1.0E-3F;
        this.turn.identity().rotateAxis(bankNow, (float) forward.x, 0.0F, (float) forward.z)
                .rotateAxis(tiltNow, (float) left.x, 0.0F, (float) left.z);
    }

    // How flat he lies in flight now: 0 upright, 1 flat out.
    float flat() {
        return (float) Mth.clamp(this.tilt.value / Mth.HALF_PI, 0.0, 1.0) * (float) this.fly.value;
    }
}
