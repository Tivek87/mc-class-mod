package nl.tivek.multiversepowers.network.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.tivek.multiversepowers.character.CharacterLookPayload;
import nl.tivek.multiversepowers.character.CharacterStatePayload;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.docock.ArmPayload;
import nl.tivek.multiversepowers.character.docock.GrabStatePayload;
import nl.tivek.multiversepowers.character.docock.PortalPayload;
import nl.tivek.multiversepowers.character.docock.client.ClientArms;
import nl.tivek.multiversepowers.character.docock.client.ClientGrabState;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.FlattenPayload;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.Flattened;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientLooks;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.classes.ClassSyncPayload;
import nl.tivek.multiversepowers.classes.PlayerClass;
import nl.tivek.multiversepowers.classes.client.ClientClassData;
import nl.tivek.multiversepowers.classes.client.ClientWelcome;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.WorldSettingsPayload;
import nl.tivek.multiversepowers.engine.fx.ParticlesPayload;
import nl.tivek.multiversepowers.faction.StandingsPayload;
import nl.tivek.multiversepowers.faction.client.ClientStandings;
import nl.tivek.multiversepowers.spell.Spell;
import nl.tivek.multiversepowers.spell.SpellCooldownPayload;
import nl.tivek.multiversepowers.spell.SpellFxPayload;
import nl.tivek.multiversepowers.spell.VoidStatePayload;
import nl.tivek.multiversepowers.spell.client.ClientSpellCooldowns;
import nl.tivek.multiversepowers.spell.client.ClientVoidState;
import nl.tivek.multiversepowers.spell.client.SpellFx;
import nl.tivek.multiversepowers.stamina.StaminaCostPayload;
import nl.tivek.multiversepowers.stamina.client.StaminaClient;

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
        context.enqueueWork(() -> ClientLooks.update(payload));
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
            for (ParticlesPayload.Entry entry : payload.entries()) {
                connection.handleParticleEvent(new ClientboundLevelParticlesPacket(entry.options(), entry.force(),
                        entry.x(), entry.y(), entry.z(), entry.dx(), entry.dy(), entry.dz(), entry.speed(),
                        entry.count()));
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
