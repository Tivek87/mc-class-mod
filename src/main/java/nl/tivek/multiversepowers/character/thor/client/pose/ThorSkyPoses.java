package nl.tivek.multiversepowers.character.thor.client.pose;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorRise;
import nl.tivek.multiversepowers.character.thor.storm.LightningBomb;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// Thor's storm and lightning bomb on his body: a hand raised to the sky to call the storm up (the hammer's, while he
// holds it), thrust out ahead to call a bolt down, and the bomb: rising with his arms open, curled up tight while it
// charges, flung wide as it bursts, then loose again as he sinks back down. Model space as in ThorPoses.
final class ThorSkyPoses {
    private static final float RAISE = 36.0F;
    private static final float THRUST = 12.0F;
    // After the burst he stays flung open this long, then eases out of it over `LOOSEN`.
    private static final float OPEN = 14.0F;
    private static final float LOOSEN = 26.0F;

    private ThorSkyPoses() {
    }

    static void storm(ThorPoses.Mix mix, ClientThor.View view, ThorBody body, float age) {
        int side = body.hammer.value > 0.5 ? 1 : 0;
        float sign = side == 0 ? -1.0F : 1.0F;
        if (view.move() == ThorStatePayload.STORM && age < RAISE) {
            float w = (float) (Ease.smooth(age / 5.0) * (1.0 - Ease.smooth((age - (RAISE - 9.0F)) / 9.0)));
            mix.weight = Math.max(mix.weight, w);
            mix.hand(side, sign * 3.5F, -12.5F, -1.0F, w, false);
            mix.pitch -= 0.1F * w;
        } else if (view.move() == ThorStatePayload.CALL && age < THRUST) {
            float w = (float) (Ease.smooth(age / 2.0) * (1.0 - Ease.smooth((age - (THRUST - 5.0F)) / 5.0)));
            mix.weight = Math.max(mix.weight, w);
            mix.aimedHands[side] = true;
            mix.hand(side, sign * 4.0F, -1.5F, -10.5F, w, false);
        }
    }

    static void bomb(ThorPoses.Mix mix, ClientThor.View view, ThorBody body, float age, LivingEntity entity) {
        if (view.move() != ThorStatePayload.BOMB || view.arg() == ThorStatePayload.PUT_OUT
                || age >= LightningBomb.BURST + OPEN + LOOSEN
                || age < LightningBomb.BURST && entity == Minecraft.getInstance().player && !ThorRise.active()) {
            return;
        }
        float w = (float) (Ease.smooth(age / 6.0)
                * (1.0 - Ease.smooth((age - (LightningBomb.BURST + OPEN)) / LOOSEN)));
        if (w < 1.0E-3F) {
            return;
        }
        float charge = (float) Ease.smooth((age - LightningBomb.RISE) / 6.0);
        float wide = (float) Ease.smooth((age - LightningBomb.BURST) / 2.0);
        float open = 1.0F - charge;
        float curl = charge * (1.0F - wide);
        mix.weight = Math.max(mix.weight, w);
        mix.airborne = true;
        mix.pitch += w * (-0.06F * open + 0.3F * curl - 0.18F * wide);
        mix.waist += w * 0.25F * curl;
        for (int s = 0; s < 2; s++) {
            float sign = s == 0 ? -1.0F : 1.0F;
            float shake = (float) (Noise.smooth(entity.getId() * 3 + s, body.time * 0.6) - 0.5) * 0.8F * curl;
            mix.hand(s, sign * (9.0F * open + 2.5F * curl + 10.5F * wide) + shake,
                    7.0F * open + 5.0F * curl - 7.5F * wide + shake, 0.5F * open - 4.0F * curl + 1.0F * wide, w,
                    false);
            float hang = Stance.HIP_Y + mix.drop + 12.0F;
            mix.foot(s, sign * (Stance.HIP_X * (open + 0.8F * curl) + (Stance.HIP_X + 3.0F) * wide),
                    hang - 1.5F * open - 6.5F * curl - 0.5F * wide, 1.0F * open - 3.0F * curl + 1.0F * wide, w);
        }
    }
}
