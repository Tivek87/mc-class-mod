package nl.tivek.multiversepowers.character.thor.client.pose;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorMotion;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorPull;
import nl.tivek.multiversepowers.character.thor.hammer.HammerRules;
import nl.tivek.multiversepowers.character.thor.hammer.ThrownHammer;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

// Thor's body with his hammer out of his hands, as everyone sees it: the hammer drawn back over his shoulder to throw
// and follow, his arm stretched to it while he calls it back or waits to be pulled to it, flat behind his reaching
// hand in the dash, the catch shoving his arm back, and arms out as he hangs in the air while a Storm Throw is out.
// Hands in the model's own frame (pixels: +x his left, y down, -z ahead), aimed at the hammer by his body's yaw.
final class ThorHammerPoses {
    // Where each shoulder is, and how far a stretched arm reaches from it.
    private static final Vector3f RIGHT_SHOULDER = new Vector3f(-5.0F, 2.0F, 0.0F);
    private static final Vector3f LEFT_SHOULDER = new Vector3f(5.0F, 2.0F, 0.0F);
    private static final float ARM = 10.5F;
    // Drawn back over his right shoulder, the left hand out ahead for balance.
    private static final Vector3f DRAWN = new Vector3f(-6.5F, -9.5F, 4.5F);
    private static final Vector3f BALANCE = new Vector3f(6.0F, 1.0F, -7.0F);
    private static final float CATCH_TICKS = 8.0F;

    private ThorHammerPoses() {
    }

    static void pose(ThorPoses.Mix mix, ClientThor.View view, ThorBody body, float age, LivingEntity entity,
            float partialTick) {
        draw(mix, body);
        hang(mix, body, entity);
        ThrownHammer hammer = ClientThor.hammer(entity);
        if (view.move() == ThorStatePayload.PULL && view.has(ThorStatePayload.PULLING)) {
            pull(mix, view, body, age, entity, hammer, partialTick);
        } else if (hammer != null && (view.has(ThorStatePayload.CALLING) || awaiting(entity))) {
            reach(mix, body, entity, hammer, partialTick, awaiting(entity) || view.has(ThorStatePayload.FLYING),
                    (float) body.reach.value);
        }
        caught(mix, view, age);
    }

    // Your own Thor waits, his left hand raised, for the hammer he called to fly with.
    private static boolean awaiting(LivingEntity entity) {
        return entity == Minecraft.getInstance().player && ThorMotion.awaiting();
    }

    // Throw and Follow's draw: the hammer drawn back high over his right shoulder, weight back, the left arm out.
    private static void draw(ThorPoses.Mix mix, ThorBody body) {
        float w = (float) body.cocked.value;
        if (w < 1.0E-3F) {
            return;
        }
        mix.weight = Math.max(mix.weight, w);
        mix.pitch -= 0.08F * w;
        mix.twist += 0.35F * w;
        mix.drop += 0.8F * w;
        mix.hand(0, DRAWN.x, DRAWN.y, DRAWN.z, w * 2.0F, false);
        mix.hand(1, BALANCE.x, BALANCE.y, BALANCE.z, w, false);
    }

    // Hanging in the air while a Storm Throw is out: arms spread, legs loose.
    private static void hang(ThorPoses.Mix mix, ThorBody body, LivingEntity entity) {
        float w = (float) body.hang.value;
        if (w < 1.0E-3F) {
            return;
        }
        float time = body.time;
        mix.weight = Math.max(mix.weight, w);
        mix.airborne = true;
        for (int s = 0; s < 2; s++) {
            float sign = s == 0 ? -1.0F : 1.0F;
            float sway = (float) Math.sin(time * 0.07F + s * 1.7F) * 0.8F;
            mix.hand(s, sign * (10.5F + sway), 3.5F + sway, 0.5F, w * 2.0F, false);
            mix.foot(s, sign * Stance.HIP_X, Stance.HIP_Y + mix.drop + 12.0F - (s == 0 ? 2.5F : 1.0F), 1.5F, w);
        }
    }

    // The pull: the wait, planted, the catching hand stretched to the hammer; then the dash, flat behind it.
    private static void pull(ThorPoses.Mix mix, ClientThor.View view, ThorBody body, float age, LivingEntity entity,
            ThrownHammer hammer, float partialTick) {
        boolean flyOn = hammer != null && !ThorPull.landsBy(hammer);
        int side = flyOn ? 1 : 0;
        if (age < HammerRules.WAIT || hammer == null) {
            float w = (float) Ease.smooth(age / 3.0);
            mix.weight = Math.max(mix.weight, w);
            mix.drop += 1.2F * w;
            mix.pitch += 0.08F * w;
            if (hammer != null) {
                reach(mix, body, entity, hammer, partialTick, flyOn, w);
            }
            return;
        }
        // Flat out behind his reaching hand: the body lies along the way he goes (ThorBody), the hand straight on.
        float w = (float) Ease.smooth((age - HammerRules.WAIT) / 2.0);
        float sign = side == 0 ? -1.0F : 1.0F;
        mix.weight = Math.max(mix.weight, w);
        mix.airborne = true;
        mix.hand(side, sign * 2.5F, -12.5F, -1.5F, w * 3.0F, true);
        mix.hand(1 - side, -sign * 6.0F, 10.0F, 2.5F, w, false);
        for (int s = 0; s < 2; s++) {
            mix.foot(s, (s == 0 ? -1.0F : 1.0F) * 1.2F, Stance.HIP_Y + 12.0F - (s == side ? 0.5F : 2.0F), 2.0F, w);
        }
    }

    // An arm stretched out at the hammer, palm open: the right, or the left when it will come into that hand.
    private static void reach(ThorPoses.Mix mix, ThorBody body, LivingEntity entity, ThrownHammer hammer,
            float partialTick, boolean left, float w) {
        if (w < 1.0E-3F) {
            return;
        }
        Vector3f shoulder = left ? LEFT_SHOULDER : RIGHT_SHOULDER;
        double scale = entity.getScale();
        Vec3 from = entity.getPosition(partialTick).add(0.0, (24.0 - shoulder.y) / 16.0 * scale * 0.9375, 0.0);
        Vec3 to = hammer.getPosition(partialTick).subtract(from);
        if (to.lengthSqr() < 1.0E-4) {
            return;
        }
        Vector3f way = toModel(to.normalize(), body.yaw);
        mix.weight = Math.max(mix.weight, w);
        mix.hand(left ? 1 : 0, shoulder.x + way.x * ARM, shoulder.y + way.y * ARM, shoulder.z + way.z * ARM,
                w * 3.0F, true);
    }

    // Caught: the hammer slams into his palm and shoves his arm back, his body rocking with it.
    private static void caught(ThorPoses.Mix mix, ClientThor.View view, float age) {
        if (view.move() != ThorStatePayload.CATCH || age >= CATCH_TICKS
                || view.arg() == ThorStatePayload.BELT) {
            return;
        }
        float shove = (float) (Ease.smooth(age / 1.5) * (1.0 - Ease.smooth((age - 1.5) / (CATCH_TICKS - 1.5))));
        if (shove < 1.0E-3F) {
            return;
        }
        boolean left = view.arg() == ThorStatePayload.LEFT_HAND;
        float sign = left ? 1.0F : -1.0F;
        mix.weight = Math.max(mix.weight, shove);
        mix.pitch -= 0.06F * shove;
        mix.hand(left ? 1 : 0, sign * 5.0F, -3.0F, -4.0F, shove * 1.5F, false);
    }

    // A world direction in the model's frame, turned by the body's yaw: +x his left, y down, -z ahead.
    private static Vector3f toModel(Vec3 way, float yaw) {
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        double lx = Math.cos(rad);
        double lz = Math.sin(rad);
        return new Vector3f((float) (way.x * lx + way.z * lz), (float) -way.y, (float) -(way.x * fx + way.z * fz));
    }
}
