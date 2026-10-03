package nl.tivek.multiversepowers.character.greenlantern.client.body.pose;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.body.arm.BeamArm;
import nl.tivek.multiversepowers.character.greenlantern.client.body.arm.BoltArm;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Green Lantern's whole body in his big moments, under the arms his own poses give (LanternPose): his arrival (wary
// under the scan, stretching up for the lantern, the catch soaked up in his knees, the oath in a wide proud stance,
// jolted as the ring comes on), the recharge (braced and leaning back to the lantern, twisting into the punch of
// the ring, rocked by the burst) and the beam and bolt (a staggered, braced stance turned into the shot). The arms
// keep aiming where their poses point; the hips, knees, trunk and waist carry the rest (Stance).
public final class LanternBody {
    private static final Vector3f KNEE = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf WAIST = new Quaternionf();
    private static final Vector3f HIPS = new Vector3f();

    private LanternBody() {
    }

    // What this frame's body is made of: hips sunk, lean, waist, twist and each foot's shift from where it stands.
    private static final class Mix {
        float drop;
        float pitch;
        float waist;
        float twist;
        float wide;
        float stagger;
        float weight;

        void reset() {
            this.drop = 0.0F;
            this.pitch = 0.0F;
            this.waist = 0.0F;
            this.twist = 0.0F;
            this.wide = 0.0F;
            this.stagger = 0.0F;
            this.weight = 0.0F;
        }

        void add(float w, float drop, float pitch, float waist, float twist, float wide, float stagger) {
            if (w <= 0.0F) {
                return;
            }
            this.weight = Math.max(this.weight, w);
            this.drop += drop * w;
            this.pitch += pitch * w;
            this.waist += waist * w;
            this.twist += twist * w;
            this.wide = Math.max(this.wide, wide * w);
            this.stagger += stagger * w;
        }
    }

    private static final Mix MIX = new Mix();

    public static boolean pose(PlayerModel<?> model, LivingEntity entity) {
        if (!EntityPass.inWorld() || ClientRing.flight(entity, 0.0F) >= 0.0F) {
            return false;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        Mix mix = MIX;
        mix.reset();
        arrival(mix, ClientRing.arrival(entity, partialTick));
        recharge(mix, ClientRing.recharge(entity, partialTick));
        shot(mix, entity, partialTick);
        if (mix.weight < 1.0E-3F) {
            return false;
        }
        Vector3f right = Stance.foot(model, true, new Vector3f());
        Vector3f left = Stance.foot(model, false, new Vector3f());
        HIPS.set(0.0F, Stance.HIP_Y + mix.drop, 0.0F);
        LEAN.rotationZYX(0.0F, mix.twist * 0.4F, mix.pitch);
        WAIST.rotationZYX(0.0F, mix.twist * 0.6F, mix.waist);
        Stance.trunk(model, HIPS, LEAN, WAIST, false);
        float w = mix.weight;
        right.lerp(new Vector3f(-Stance.HIP_X - mix.wide, Stance.GROUND, mix.stagger), w);
        left.lerp(new Vector3f(Stance.HIP_X + mix.wide, Stance.GROUND, -mix.stagger), w);
        Stance.leg(model, true, right, KNEE);
        Stance.leg(model, false, left, KNEE);
        return true;
    }

    private static float window(float t, float from, float to, float in, float out) {
        return (float) (Ease.smooth((t - from) / in) * (1.0 - Ease.smooth((t - to) / out)));
    }

    private static float jolt(float t, float at, float length) {
        return (float) Ease.jolt((t - at) / length);
    }

    private static void arrival(Mix mix, float a) {
        if (a < 0.0F) {
            return;
        }
        // Wary under the scan.
        mix.add(window(a, Arrival.SCAN, Arrival.SCANNED + 2.0F, 4.0F, 6.0F), 1.5F, 0.08F, 0.0F, 0.0F, 0.7F,
                0.8F);
        // Stretching up for the lantern as it forms above him.
        mix.add(window(a, Arrival.LANTERN_FORMED - 4.0F, Arrival.LANTERN_CAUGHT, 5.0F, 2.0F), -0.3F, -0.1F,
                -0.12F, 0.0F, 0.4F, 0.0F);
        // The catch, soaked up in the knees.
        mix.add(jolt(a, Arrival.LANTERN_CAUGHT - 1.0F, 8.0F), 2.6F, 0.14F, 0.05F, 0.0F, 0.6F, 0.0F);
        // The oath, fist up: a wide, proud stance, the chest out; jolted as the ring comes on.
        float oath = window(a, Arrival.RING_FLY - 2.0F, Arrival.DRESSED, 5.0F, 8.0F);
        mix.add(oath, 1.1F, -0.04F, -0.1F, 0.06F, 1.4F, 1.3F);
        mix.add(jolt(a, Arrival.RING_ON, 7.0F), 1.6F, -0.05F, -0.16F, 0.0F, 1.2F, 0.0F);
    }

    private static void recharge(Mix mix, float t) {
        if (t < 0.0F) {
            return;
        }
        float on = window(t, 0.0F, PowerRing.RECHARGE_TICKS - 6.0F, 5.0F, 6.0F);
        // Braced wide, leaning back to the lantern held up.
        mix.add(on, 1.2F, -0.06F, -0.08F, 0.0F, 1.1F, 0.9F);
        // The right shoulder drawn back for the punch of the ring into the lantern, then driven in.
        float wind = RechargeAnimation.wind(t);
        mix.add(on * wind, 0.8F, 0.0F, 0.0F, 0.32F, 0.0F, 0.0F);
        float kick = RechargeAnimation.kick(t);
        mix.add(kick, 2.2F, 0.16F, 0.04F, -0.14F, 0.0F, 0.0F);
        // The burst of light rocks him back.
        mix.add(RechargeAnimation.burst(t) * on, 0.4F, -0.08F, -0.16F, 0.0F, 0.0F, 0.0F);
    }

    // The beam (charging and firing) and the bolt: braced, the right shoulder turned into the shot.
    private static void shot(Mix mix, LivingEntity entity, float partialTick) {
        float gather = Math.max(0.0F, BeamArm.gathering(entity, partialTick));
        float firing = ClientRing.has(entity, RingPayload.BEAM) ? 1.0F : 0.0F;
        float beam = Math.max(gather * 0.8F, firing);
        float time = entity.tickCount + partialTick;
        float tremble = firing * 0.02F * Mth.sin(time * 2.7F);
        mix.add(beam, 2.0F, 0.08F + tremble, 0.04F, -0.3F, 0.9F, 2.2F);
        float kick = BoltArm.kick(entity, partialTick);
        mix.add(kick, 0.9F, 0.06F, 0.0F, -0.22F, 0.3F, 1.0F);
    }
}
