package nl.tivek.multiversepowers.character.greenlantern.client.body;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;

/**
 * How the ring arm moves with the bolts (a tap of the attack button), seen from outside ({@link FlightPose}) and in
 * your own first person ({@link LanternArms}) alike: it comes up and points straight where he aims, so every bolt
 * leaves the ring on his outstretched hand; it kicks back a little with every bolt, stays up while he keeps
 * shooting (or holds the button on for the beam), and goes down again a moment after the last bolt.
 */
final class BoltArm {
    // How long the arm stays up after the newest bolt, in ticks, before it goes down again.
    private static final float HOLD_TICKS = 14.0F;
    // How long the kick of one bolt lasts, in ticks.
    private static final float KICK_TICKS = 5.0F;

    private BoltArm() {
    }

    /**
     * Ticks since this player's newest bolt left the ring, or -1 when none did lately. For yourself it counts from
     * your click instead, as soon as that is nearer: your arm comes up before the server's bolt comes back, so the
     * bolt leaves a hand that is already up.
     */
    static float since(LivingEntity entity, float partialTick) {
        float seen = ClientConstructs.boltAge(entity.getId(), partialTick);
        if (entity == Minecraft.getInstance().player && ClientCharacter.active() == GameCharacter.GREEN_LANTERN) {
            float own = ClientCharacter.sinceTap(GameCharacter.GREEN_LANTERN.byName("light_bolt"), partialTick);
            if (own >= 0.0F && (seen < 0.0F || own < seen)) {
                seen = own;
            }
        }
        return seen;
    }

    /**
     * True while the ring arm points where he aims: a bolt left it a moment ago, or the ring gathers its light for
     * the beam (or pours it), which keeps the arm up in between.
     */
    static boolean pointing(LivingEntity entity, float partialTick) {
        float since = since(entity, partialTick);
        return (since >= 0.0F && since < HOLD_TICKS) || ClientRing.has(entity, RingPayload.BEAM)
                || BeamArm.gathering(entity, partialTick) >= 0.0F;
    }

    /** The kick of the newest bolt: 1 as it leaves the ring, dying down to 0. */
    static float kick(LivingEntity entity, float partialTick) {
        float since = since(entity, partialTick);
        if (since < 0.0F || since >= KICK_TICKS) {
            return 0.0F;
        }
        float left = 1.0F - since / KICK_TICKS;
        return left * left;
    }
}
