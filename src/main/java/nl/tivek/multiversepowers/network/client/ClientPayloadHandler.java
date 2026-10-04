package nl.tivek.multiversepowers.network.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.RandomSource;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.tivek.multiversepowers.character.CharacterLookPayload;
import nl.tivek.multiversepowers.character.CharacterStatePayload;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.docock.ArmPayload;
import nl.tivek.multiversepowers.character.docock.GrabStatePayload;
import nl.tivek.multiversepowers.character.docock.client.ClientArms;
import nl.tivek.multiversepowers.character.docock.client.ClientGrabState;
import nl.tivek.multiversepowers.character.docock.portal.PortalPayload;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientLooks;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.Flattened;
import nl.tivek.multiversepowers.character.greenlantern.client.victim.HandVictims;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.construct.FlattenPayload;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandVictimPayload;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.storm.StormFxPayload;
import nl.tivek.multiversepowers.character.thor.storm.client.StormBolts;
import nl.tivek.multiversepowers.classes.ClassSyncPayload;
import nl.tivek.multiversepowers.classes.PlayerClass;
import nl.tivek.multiversepowers.classes.client.ClientClassData;
import nl.tivek.multiversepowers.classes.client.ClientWelcome;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.WorldSettingsPayload;
import nl.tivek.multiversepowers.engine.client.fx.ParticleAmount;
import nl.tivek.multiversepowers.engine.client.fx.VoiceLine;
import nl.tivek.multiversepowers.engine.client.pose.Tired;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ashes;
import nl.tivek.multiversepowers.engine.client.ragdoll.Downed;
import nl.tivek.multiversepowers.engine.client.ragdoll.Knocked;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import nl.tivek.multiversepowers.engine.entity.DeathBlowPayload;
import nl.tivek.multiversepowers.killconfirm.KillConfirmPayload;
import nl.tivek.multiversepowers.killconfirm.client.KillMarker;
import nl.tivek.multiversepowers.engine.entity.DeathStylePayload;
import nl.tivek.multiversepowers.engine.entity.DeathStyles;
import nl.tivek.multiversepowers.engine.entity.FatiguePayload;
import nl.tivek.multiversepowers.engine.entity.HeldPayload;
import nl.tivek.multiversepowers.engine.entity.KnockdownPayload;
import nl.tivek.multiversepowers.engine.fx.ParticlesPayload;
import nl.tivek.multiversepowers.engine.fx.VoicePayload;
import nl.tivek.multiversepowers.faction.StandingsPayload;
import nl.tivek.multiversepowers.faction.client.ClientStandings;
import nl.tivek.multiversepowers.spell.ClapPayload;
import nl.tivek.multiversepowers.spell.Spell;
import nl.tivek.multiversepowers.spell.SpellCooldownPayload;
import nl.tivek.multiversepowers.spell.SpellFxPayload;
import nl.tivek.multiversepowers.spell.client.ClientClaps;
import nl.tivek.multiversepowers.spell.client.ClientSpellCooldowns;
import nl.tivek.multiversepowers.spell.client.SpellFx;
import nl.tivek.multiversepowers.spell.dark.VoidStatePayload;
import nl.tivek.multiversepowers.spell.dark.client.ClientVoidState;
import nl.tivek.multiversepowers.stamina.StaminaCostPayload;
import nl.tivek.multiversepowers.stamina.client.StaminaClient;
import nl.tivek.multiversepowers.testfight.TestFightPayload;
import nl.tivek.multiversepowers.testfight.client.FightClient;

public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleOpenWelcome(IPayloadContext context) {
        context.enqueueWork(ClientWelcome::requestWelcomeScreen);
    }

    public static void handleSpellCooldown(SpellCooldownPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Spell spell = Spell.byId(payload.spellId());
            if (spell != null) {
                ClientSpellCooldowns.set(spell, payload.ticks());
            }
        });
    }

    public static void handleSpellFx(SpellFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SpellFx.add(payload));
    }

    public static void handleVoidState(VoidStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientVoidState.set(payload.ticks()));
    }

    public static void handleThorState(ThorStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientThor.update(payload));
    }

    public static void handleStormFx(StormFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> StormBolts.add(payload));
    }

    public static void handleClap(ClapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientClaps.clap(payload.entity()));
    }

    public static void handleGrabState(GrabStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientGrabState.set(payload.holding(), payload.blocks()));
    }

    public static void handleArm(ArmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientArms.update(payload));
    }

    public static void handleCharacterState(CharacterStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCharacter.set(payload));
    }

    public static void handleCharacterLook(CharacterLookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLooks.update(payload);
            ClientCharacter.seen(payload);
        });
    }

    public static void handleStaminaCost(StaminaCostPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> StaminaClient.use(payload.amount()));
    }

    public static void handlePortal(PortalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientArms.updatePortal(payload));
    }

    public static void handleConstruct(ConstructPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientConstructs.update(payload));
    }

    public static void handleFlatten(FlattenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Flattened.flatten(payload.entity()));
    }

    public static void handleHeld(HeldPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Ragdolls.held(payload.entity(), payload.held()));
    }

    public static void handleKnockdown(KnockdownPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Knocked.told(payload.entity(), payload.ticks());
            Downed.told(payload.entity(), payload.ticks());
        });
    }

    public static void handleFatigue(FatiguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Tired.told(payload.entity(), payload.hits()));
    }

    public static void handleKillConfirm(KillConfirmPayload payload, IPayloadContext context) {
        context.enqueueWork(KillMarker::confirm);
    }

    public static void handleTestFight(TestFightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> FightClient.told(payload.fighter(), payload.target(), payload.phase()));
    }

    public static void handleDeathBlow(DeathBlowPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Ragdolls.struck(payload.entity(), payload.from(), payload.push()));
    }

    public static void handleDeathStyle(DeathStylePayload payload, IPayloadContext context) {
        if (payload.style() == DeathStyles.Style.ASH.ordinal()) {
            context.enqueueWork(() -> Ashes.burn(payload.entity()));
        }
    }

    public static void handleHandVictim(HandVictimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> HandVictims.mark(payload.entity(), payload.hand(), payload.kind()));
    }

    public static void handleVoice(VoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> VoiceLine.say(payload.speaker(), payload.sound()));
    }

    public static void handleRing(RingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientRing.update(payload));
    }

    public static void handleStandings(StandingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientStandings.update(payload));
    }

    public static void handleClassSync(ClassSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientClassData.set(PlayerClass.byId(payload.classId())));
    }

    public static void handleParticles(ParticlesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection == null) {
                return;
            }
            RandomSource random = Minecraft.getInstance().level == null ? RandomSource.create()
                    : Minecraft.getInstance().level.random;
            for (ParticlesPayload.Entry entry : payload.entries()) {
                // A count of 0 is one particle sent off exactly as given: the player's share repeats it instead.
                int count = entry.count() == 0 ? 0 : ParticleAmount.count(entry.count(), random);
                int times = entry.count() == 0 ? ParticleAmount.count(1, random) : count > 0 ? 1 : 0;
                for (int k = 0; k < times; k++) {
                    connection.handleParticleEvent(new ClientboundLevelParticlesPacket(entry.options(), entry.force(),
                            entry.x(), entry.y(), entry.z(), entry.dx(), entry.dy(), entry.dz(), entry.speed(), count));
                }
            }
        });
    }

    public static void handleWorldSettings(WorldSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().isLocalServer() || !ModConfigs.worldFiles().containsKey(payload.file())) {
                return;
            }
            ModConfig config = net.neoforged.fml.config.ModConfigs.getFileMap().get(payload.file());
            if (config != null && config.getType() == ModConfig.Type.SERVER) {
                ConfigTracker.acceptSyncedConfig(config, payload.contents());
                StaminaClient.onConfigUpdated();
            }
        });
    }
}
