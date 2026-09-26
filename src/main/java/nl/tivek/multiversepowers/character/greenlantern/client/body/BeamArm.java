package nl.tivek.multiversepowers.character.greenlantern.client.body;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BeamCharge;
import nl.tivek.multiversepowers.engine.math.Ease;

final class BeamArm {
    private static final float KICK_TICKS = 12.0F;
    private static final float KICK_DAMP = 0.42F;
    private static final float KICK_SWING = 0.72F;
    private static final float CHARGE_TREMBLE = 1.0F;
    private static final float POUR_TREMBLE = 0.45F;
    private static final float BRACE_FROM = 0.2F;
    private static final float BRACE_AT = 0.65F;

    private BeamArm() {
    }

    static float gathering(LivingEntity entity, float partialTick) {
        float charge = BeamCharge.charge(entity, partialTick);
        if (charge < 0.0F && entity == Minecraft.getInstance().player && !ClientRing.has(entity, RingPayload.BEAM)
                && ClientRing.power(entity) > 0.0F && ClientCharacter.active() == GameCharacter.GREEN_LANTERN
                && !SwordArms.holding() && !FlameArms.holding() && !WhipArms.holding()
                && MouseHold.progress(GameCharacter.GREEN_LANTERN.byName("light_bolt"), partialTick) >= 1.0F) {
            return 1.0F;
        }
        return charge;
    }

    static float kick(LivingEntity entity, float partialTick) {
        float age = ClientConstructs.beamAge(entity.getId(), partialTick);
        if (age < 0.0F || age >= KICK_TICKS) {
            return 0.0F;
        }
        return (float) Math.exp(-KICK_DAMP * age) * Mth.cos(KICK_SWING * age);
    }

    static float tremble(LivingEntity entity, float partialTick) {
        if (ClientRing.has(entity, RingPayload.BEAM)) {
            return POUR_TREMBLE;
        }
        float charge = gathering(entity, partialTick);
        return charge <= 0.0F ? 0.0F : CHARGE_TREMBLE * charge * charge;
    }

    static float shake(float time, int which) {
        return which == 0 ? 0.7F * Mth.sin(time * 3.1F) + 0.3F * Mth.sin(time * 7.3F + 0.4F)
                : 0.7F * Mth.sin(time * 2.3F + 1.0F) + 0.3F * Mth.sin(time * 6.1F + 2.2F);
    }

    static float brace(LivingEntity entity, float partialTick) {
        if (!entity.getOffhandItem().isEmpty() || ClientRing.has(entity, RingPayload.DOME)
                || ClientRing.has(entity, RingPayload.SHIELD)) {
            return 0.0F;
        }
        if (ClientConstructs.heldBy(entity.getId(), true) != null) {
            return 0.0F;
        }
        if (ClientRing.has(entity, RingPayload.BEAM)) {
            return 1.0F;
        }
        float charge = gathering(entity, partialTick);
        return charge <= 0.0F ? 0.0F : (float) Ease.smooth((charge - BRACE_FROM) / (BRACE_AT - BRACE_FROM));
    }
}
