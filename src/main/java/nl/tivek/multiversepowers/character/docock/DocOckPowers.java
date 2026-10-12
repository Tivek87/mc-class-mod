package nl.tivek.multiversepowers.character.docock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterPowers;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.Captives;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.entity.PlayerKnockdowns;

// Doctor Octopus: the iron harness on his back and round his waist and the four tentacles out of it (OctoRig). His
// stance key takes the tentacles down to the ground and up again; the rest comes from his own game (OctoInputPayload):
// grabbing, squeezing, slamming, hurling, letting go and striking. Two tentacles on the ground or more carry him over a
// block's step and keep him from falling hurt. Everything held is let go of when he stops being him, dies, logs out or
// goes to another dimension.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class DocOckPowers implements CharacterPowers {
    private static final ResourceLocation STEP_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "tentacle_step");
    private static final Map<UUID, OctoRig> RIGS = new HashMap<>();

    static {
        HeldMobs.addHolder(entity -> {
            for (OctoRig rig : RIGS.values()) {
                if (rig.holds(entity)) {
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public void enter(ServerPlayer player) {
        Characters.transformFlash(player, 0.8F);
        OctoRig old = RIGS.remove(player.getUUID());
        if (old != null) {
            old.stop();
        }
        OctoRig rig = new OctoRig(player);
        RIGS.put(player.getUUID(), rig);
        Effects.start(player.serverLevel(), rig);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_EXTEND,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        rig.send();
    }

    @Override
    public void leave(ServerPlayer player) {
        OctoRig rig = RIGS.remove(player.getUUID());
        if (rig != null) {
            rig.stop();
        }
        step(player, 0);
    }

    @Override
    public boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig == null || !on || !ability.id().equals("stance")) {
            return false;
        }
        rig.cycleStance();
        step(player, rig.grounded());
        return true;
    }

    @Override
    public void showTo(ServerPlayer viewer, ServerPlayer target) {
        OctoRig rig = RIGS.get(target.getUUID());
        if (rig != null) {
            rig.showTo(viewer);
        }
    }

    @Override
    public void knockedDown(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null) {
            rig.releaseAll();
        }
    }

    @Override
    public void clear() {
        for (OctoRig rig : RIGS.values()) {
            rig.stop();
        }
        RIGS.clear();
    }

    public static void input(ServerPlayer player, OctoInputPayload input) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig == null || Characters.of(player) != GameCharacter.DOC_OCK || !player.isAlive()) {
            return;
        }
        int mask = input.mask() & 0xF;
        boolean busy = PlayerKnockdowns.isDown(player) || Captives.held(player);
        switch (input.action()) {
            case OctoInputPayload.GRAB -> {
                if (!busy) {
                    rig.grab(input.target(), mask);
                }
            }
            case OctoInputPayload.SQUEEZE -> rig.squeeze(mask, !busy);
            case OctoInputPayload.SQUEEZE_END -> rig.squeeze(0, false);
            case OctoInputPayload.SLAM -> {
                Vec3 way = new Vec3(input.x(), input.y(), input.z());
                if (!busy && way.lengthSqr() < 4.0) {
                    rig.slam(mask, way);
                }
            }
            case OctoInputPayload.THROW -> {
                CharacterAbility hurl = OctoRig.ability("throw");
                if (!busy && Characters.cooldownLeft(player, hurl) <= 0 && rig.hurl(mask, input.target())) {
                    Characters.startCooldown(player, hurl);
                }
            }
            case OctoInputPayload.RELEASE -> rig.release(mask);
            case OctoInputPayload.STRIKE -> {
                if (!busy) {
                    rig.strikeAt(input.target(), mask);
                }
            }
            default -> {
            }
        }
        step(player, rig.grounded());
    }

    private static void step(ServerPlayer player, int grounded) {
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step == null) {
            return;
        }
        if (grounded >= 2) {
            step.addOrUpdateTransientModifier(new AttributeModifier(STEP_ID, 0.5,
                    AttributeModifier.Operation.ADD_VALUE));
        } else {
            step.removeModifier(STEP_ID);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OctoRig rig = RIGS.get(player.getUUID());
            if (rig != null) {
                rig.releaseAll();
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OctoRig rig = RIGS.get(player.getUUID());
            if (rig != null && rig.grounded() >= 2) {
                event.setDamageMultiplier(0.0F);
            }
        }
    }
}
