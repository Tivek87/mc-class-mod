package nl.tivek.multiversepowers.character.greenlantern.client.body;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;

final class BoltArm {
    private static final float HOLD_TICKS = 14.0F;
    private static final float KICK_TICKS = 5.0F;

    private BoltArm() {
    }

    static float since(LivingEntity entity, float partialTick) {
        float seen = ClientConstructs.boltAge(entity.getId(), partialTick);
        if (entity == Minecraft.getInstance().player && ClientCharacter.active() == GameCharacter.GREEN_LANTERN) {
            // Your own click, so the arm comes up before the server's bolt confirms it.
            float own = ClientCharacter.sinceTap(GameCharacter.GREEN_LANTERN.byName("light_bolt"), partialTick);
            if (own >= 0.0F && (seen < 0.0F || own < seen)) {
                seen = own;
            }
        }
        return seen;
    }

    static boolean pointing(LivingEntity entity, float partialTick) {
        float since = since(entity, partialTick);
        return (since >= 0.0F && since < HOLD_TICKS) || ClientRing.has(entity, RingPayload.BEAM)
                || BeamArm.gathering(entity, partialTick) >= 0.0F;
    }

    static float kick(LivingEntity entity, float partialTick) {
        float since = since(entity, partialTick);
        if (since < 0.0F || since >= KICK_TICKS) {
            return 0.0F;
        }
        float left = 1.0F - since / KICK_TICKS;
        return left * left;
    }
}
