package nl.tivek.welcomescreen.client.character.lantern;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.client.character.ClientCharacter;
import nl.tivek.welcomescreen.client.character.MouseHold;
import nl.tivek.welcomescreen.network.RingPayload;

/**
 * How the ring arm moves with the beam, seen from outside ({@link FlightPose}) and in your own first person
 * ({@link LanternArms}) alike. While the ring gathers its light the arm trembles, harder the fuller it gets; as the
 * beam breaks loose it kicks up and back, overshoots a little and settles; while it pours the arm holds steady against
 * it with a fine tremble. The other hand, when it is free, comes over to grip the ring arm's wrist and brace it.
 */
final class BeamArm {
    // How long the kick of the beam breaking loose lasts, in ticks, how fast it dies down and how fast it swings.
    private static final float KICK_TICKS = 12.0F;
    private static final float KICK_DAMP = 0.42F;
    private static final float KICK_SWING = 0.72F;
    // How hard the arm trembles: while the ring gathers its light (at the full charge) and while the beam pours, as
    // a part of the tremble at its hardest.
    private static final float CHARGE_TREMBLE = 1.0F;
    private static final float POUR_TREMBLE = 0.45F;
    // How full the ring is when the other hand starts to come over to brace the wrist, and when it is there.
    private static final float BRACE_FROM = 0.2F;
    private static final float BRACE_AT = 0.65F;

    private BeamArm() {
    }

    /**
     * How full this player's ring is for the beam, 0 to 1, or -1 while it gathers no light: {@link BeamCharge#charge},
     * but for yourself kept full from the moment you have held the button long enough until the server's word comes
     * in that the beam pours, so the arm does not drop in between.
     */
    static float gathering(LivingEntity entity, float partialTick) {
        float charge = BeamCharge.charge(entity, partialTick);
        if (charge < 0.0F && entity == Minecraft.getInstance().player && !ClientRing.has(entity, RingPayload.BEAM)
                && ClientRing.power(entity) > 0.0F && ClientCharacter.active() == GameCharacter.GREEN_LANTERN
                && !SwordArms.holding()
                && MouseHold.progress(GameCharacter.GREEN_LANTERN.byName("light_bolt"), partialTick) >= 1.0F) {
            return 1.0F;
        }
        return charge;
    }

    /**
     * The kick of the beam breaking loose: 1 as it does, dying down and swinging a little past rest (below 0) before
     * it settles at 0.
     */
    static float kick(LivingEntity entity, float partialTick) {
        float age = ClientConstructs.beamAge(entity.getId(), partialTick);
        if (age < 0.0F || age >= KICK_TICKS) {
            return 0.0F;
        }
        return (float) Math.exp(-KICK_DAMP * age) * Mth.cos(KICK_SWING * age);
    }

    /** How hard the arm trembles right now, 0 to 1: rising with the charge, then a fine tremble while it pours. */
    static float tremble(LivingEntity entity, float partialTick) {
        if (ClientRing.has(entity, RingPayload.BEAM)) {
            return POUR_TREMBLE;
        }
        float charge = gathering(entity, partialTick);
        return charge <= 0.0F ? 0.0F : CHARGE_TREMBLE * charge * charge;
    }

    /**
     * The tremble itself, two ways across the arm ({@code which} 0 or 1), -1 to 1 at its hardest: two uneven beats
     * each, so it never looks like a plain swing.
     */
    static float shake(float time, int which) {
        return which == 0 ? 0.7F * Mth.sin(time * 3.1F) + 0.3F * Mth.sin(time * 7.3F + 0.4F)
                : 0.7F * Mth.sin(time * 2.3F + 1.0F) + 0.3F * Mth.sin(time * 6.1F + 2.2F);
    }

    /**
     * How far the other hand should have come over to brace the ring arm's wrist, 0 to 1: once the ring is well on
     * its way to full, and all the time the beam pours; never while that hand holds something, a shield or the dome
     * open, or punches into the ram cone.
     */
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
        return charge <= 0.0F ? 0.0F : (float) ClientFlight.smooth((charge - BRACE_FROM) / (BRACE_AT - BRACE_FROM));
    }
}
